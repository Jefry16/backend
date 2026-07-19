package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.LoginUserInput;
import com.vointika.identity.application.dto.output.LoginUserOutput;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.service.IdGenerator;

import java.time.Duration;

public class LoginUserUseCase {

    // Real BCrypt hash at cost 10 — used to keep login timing constant when no
    // user is found so attackers can't enumerate registered emails by response
    // time. The value must be a syntactically valid BCrypt hash, otherwise the
    // encoder short-circuits and we lose the timing protection.
    private static final String SENTINEL_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    // Per-account throttle (§7.9): all attempts for one email, successes
    // included — 20/15min is unreachable for a human, fatal for a script
    // doing distributed-IP credential guessing against one account.
    private static final int MAX_ATTEMPTS_PER_EMAIL = 20;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final IdGenerator idGenerator;
    private final RateLimiterPort rateLimiter;

    public LoginUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasher,
            TokenGeneratorPort tokenGenerator,
            TokenHasherPort tokenHasher,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
    }

    public LoginUserOutput execute(LoginUserInput input) {
        // 1. Construct email value object
        Email email = new Email(input.email());

        // 2. Find user (Optional — do NOT throw yet, to keep timing constant)
        var maybeUser = userRepository.findByEmail(email);

        // 3. Always run BCrypt, either against the real hash or a sentinel.
        //    This normalizes login latency so attackers can't distinguish "no
        //    such user" from "wrong password" by response time.
        String hash = maybeUser.map(User::getHashedPassword).orElse(SENTINEL_HASH);
        boolean passwordMatches = passwordHasher.matches(input.password(), hash);

        // Counted AFTER the BCrypt work so a throttled attempt costs the same
        // wall-clock as a wrong password, and rejected with the SAME message —
        // the throttle is invisible (no lockout oracle, no existence leak).
        boolean withinLimit = rateLimiter.tryAcquire(
                "rl:login:email:" + email.value(), MAX_ATTEMPTS_PER_EMAIL, ATTEMPT_WINDOW);

        if (maybeUser.isEmpty() || !passwordMatches || !withinLimit) {
            throw new UnauthorizedException("Invalid credentials");
        }
        User user = maybeUser.get();

        // 4. Guard — only verified users can log in. Distinct from the 401 above:
        //    reaching here means the credentials were correct, so this is an
        //    authenticated-but-forbidden state (403), not a credentials failure.
        //    Only someone holding the right password can trigger it, so it adds
        //    no enumeration oracle beyond the message itself.
        if (!user.isVerified()) {
            throw new ForbiddenException("Account is not verified");
        }

        // 5. Generate tokens. Raw refresh token never persisted — only its hash.
        String rawRefreshToken = tokenGenerator.generateRefreshToken();
        String accessToken = tokenGenerator.generateAccessToken(user.getId().toString());

        // 6. Persist root refresh token (familyId = id)
        RefreshToken refreshToken = RefreshToken.newRoot(
                idGenerator.newId(), user.getId(), tokenHasher.hash(rawRefreshToken));
        refreshTokenRepository.save(refreshToken);

        return new LoginUserOutput(
                accessToken,
                rawRefreshToken
        );
    }
}
