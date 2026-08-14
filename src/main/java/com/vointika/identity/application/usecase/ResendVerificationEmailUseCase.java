package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ResendVerificationEmailInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.service.IdGenerator;

import java.time.Duration;

import java.util.Optional;

public class ResendVerificationEmailUseCase {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final RateLimiterPort rateLimiter;

    public ResendVerificationEmailUseCase(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            TokenGeneratorPort tokenGenerator,
            TokenHasherPort tokenHasher,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
    }

    public void execute(ResendVerificationEmailInput input) {
        // 1. Validate email format
        Email email = new Email(input.email());

        // Per-email cooldown (PATTERNS §8a): 3 sends/hour. Dropping silently keeps the
        // endpoint's unconditional 204 — no enumeration, no bombing, no SES burn.
        if (!rateLimiter.tryAcquire("rl:resend:email:" + email.value(), 3, Duration.ofHours(1))) {
            return;
        }

        // 2. Silently no-op if user doesn't exist or is already verified
        //    (prevents account enumeration). Timing parity: both no-op paths first do
        //    the same token work + a DB round-trip as the real path, so
        //    response timing doesn't reveal which case was hit.
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            equalizeTiming();
            return;
        }
        User user = maybeUser.get();
        if (user.isVerified()) {
            equalizeTiming();
            return;
        }

        // 3. Expire old + persist new atomically. Raw token only flows through the event.
        String rawToken = tokenGenerator.generateVerificationToken();
        VerificationToken verificationToken = VerificationToken.issue(
                idGenerator.newId(), user.getId(), tokenHasher.hash(rawToken));

        transactionRunner.run(() -> {
            verificationTokenRepository.expireAllByUserId(user.getId());
            verificationTokenRepository.save(verificationToken);
        });

        // 4. Publish verification email event — AFTER commit
        eventPublisher.publish(new VerificationEmailRequestedEvent(
                user.getEmail().value(), user.getName().value(), rawToken, user.getLanguage()));
    }

    /** The no-op counterpart of the real path's token gen + hash + tx. */
    private void equalizeTiming() {
        tokenHasher.hash(tokenGenerator.generateVerificationToken());
        transactionRunner.run(() -> { });
    }
}
