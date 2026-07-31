package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.RegisterUserInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.AccountAlreadyRegisteredEvent;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.exception.UniqueConstraintViolationException;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.Password;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.service.IdGenerator;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final RateLimiterPort rateLimiter;
    private final Set<String> supportedLanguages;

    public RegisterUserUseCase(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordHasherPort passwordHasher,
            TokenGeneratorPort tokenGenerator,
            TokenHasherPort tokenHasher,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter,
            Set<String> supportedLanguages
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
        this.supportedLanguages = supportedLanguages;
    }

    public void execute(RegisterUserInput input) {
        // 1. Validate and construct value objects
        Email email = new Email(input.email());
        UserName name = new UserName(input.name());
        Password password = new Password(input.password());

        // 2. Anti-enumeration (§7.6): an already-registered email gets the SAME
        //    generic 201 as a fresh one — the truthful answer goes only to the
        //    mailbox owner, by email. The hash below still runs first so the
        //    two paths cost the same (§7.5 timing parity; BCrypt dominates).
        Optional<User> existing = userRepository.findByEmail(email);

        // 3. Hash password (both paths — see timing note above)
        String hashedPassword = passwordHasher.hash(password.value());

        // 3a. Per-email cooldown (§7.9): at most 3 "account already registered"
        //     notices/hour/mailbox — the same anti-bombing ceiling as
        //     resend-verification and request-password-reset (the per-IP filter
        //     alone doesn't protect the mailbox: the attacker picks the IPs,
        //     the victim's address is fixed). Acquired on BOTH paths so
        //     duplicate and fresh registrations stay indistinguishable (§7.5);
        //     over the limit the notice is dropped silently — the generic 201
        //     stays (§7.6).
        boolean notifyOwnerAllowed = rateLimiter.tryAcquire(
                "rl:register:email:" + email.value(), 3, Duration.ofHours(1));

        if (existing.isPresent()) {
            if (notifyOwnerAllowed) {
                eventPublisher.publish(new AccountAlreadyRegisteredEvent(
                        existing.get().getEmail().value(), existing.get().getName().value(),
                        existing.get().getLanguage()));
            }
            return;
        }

        // 4. Build user + verification token. Raw token never persists — only its hash.
        User user = new User(idGenerator.newId(), email, name, hashedPassword);
        user.changeLanguage(resolveLanguage(input.language()));
        String rawToken = tokenGenerator.generateVerificationToken();
        VerificationToken verificationToken = VerificationToken.issue(
                idGenerator.newId(), user.getId(), tokenHasher.hash(rawToken));

        // 5. Persist both atomically. DIV can surface at flush/commit inside the tx,
        //    so catch around the whole runner.
        try {
            transactionRunner.run(() -> {
                userRepository.save(user);
                verificationTokenRepository.save(verificationToken);
            });
        } catch (UniqueConstraintViolationException e) {
            // Race with a concurrent registration of the same email: the DB
            // unique constraint won. Same anti-enumeration outcome as the
            // fast path — notify the mailbox owner (within the §7.9 cooldown),
            // respond generically.
            if (notifyOwnerAllowed) {
                userRepository.findByEmail(email).ifPresent(winner ->
                        eventPublisher.publish(new AccountAlreadyRegisteredEvent(
                                winner.getEmail().value(), winner.getName().value(),
                                winner.getLanguage())));
            }
            return;
        }

        // 6. Publish verification email event — AFTER the tx commits, so we don't
        //    enqueue an email for a registration that was rolled back.
        eventPublisher.publish(new VerificationEmailRequestedEvent(
                email.value(), name.value(), rawToken, user.getLanguage()));
    }

    /** The requested UI language if provided and supported, else "en". Never fails registration. */
    private String resolveLanguage(String requested) {
        if (requested == null || requested.isBlank()) {
            return "en";
        }
        String lang = requested.trim().toLowerCase(Locale.ROOT);
        return supportedLanguages.contains(lang) ? lang : "en";
    }
}
