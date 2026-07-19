package com.vointika.identity.infrastructure.port;

import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.Password;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.InvitedUserProvisioning;
import com.vointika.shared.service.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Identity's side of the invitation accept flow. The create branch mirrors
 * {@code RegisterUserUseCase}'s construction (same VOs, same hashing) with
 * two deliberate differences: the user is verified immediately (the accept
 * link proved the mailbox — decision 3) and NO verification event is
 * published. Session issuance mirrors {@code LoginUserUseCase} steps 5–6:
 * access JWT + a root refresh token stored only as its hash.
 */
@Component
public class InvitedUserProvisioningImpl implements InvitedUserProvisioning {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final IdGenerator idGenerator;

    public InvitedUserProvisioningImpl(UserRepository userRepository,
                                       RefreshTokenRepository refreshTokenRepository,
                                       PasswordHasherPort passwordHasher,
                                       TokenGeneratorPort tokenGenerator,
                                       TokenHasherPort tokenHasher,
                                       IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.idGenerator = idGenerator;
    }

    @Override
    public ProvisionedUser findOrCreateVerifiedUser(String email, String name, String rawPassword) {
        Email emailVo = new Email(email);
        Optional<User> existing = userRepository.findByEmail(emailVo);
        if (existing.isPresent()) {
            return new ProvisionedUser(existing.get().getId(), false);
        }

        UserName userName = new UserName(name);
        Password password = new Password(rawPassword);
        User user = new User(idGenerator.newId(), emailVo, userName,
                passwordHasher.hash(password.value()));
        // The emailed accept link proved mailbox ownership — verified from
        // birth, no verification email (decision 3).
        user.verify();
        userRepository.save(user);
        return new ProvisionedUser(user.getId(), true);
    }

    @Override
    public SessionTokens issueSession(UUID userId) {
        String rawRefreshToken = tokenGenerator.generateRefreshToken();
        String accessToken = tokenGenerator.generateAccessToken(userId.toString());
        RefreshToken refreshToken = RefreshToken.newRoot(
                idGenerator.newId(), userId, tokenHasher.hash(rawRefreshToken));
        refreshTokenRepository.save(refreshToken);
        return new SessionTokens(accessToken, rawRefreshToken);
    }
}
