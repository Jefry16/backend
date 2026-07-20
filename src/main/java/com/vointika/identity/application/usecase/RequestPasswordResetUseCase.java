package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.RequestPasswordResetInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.PasswordResetEmailRequestedEvent;
import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.service.IdGenerator;

import java.time.Duration;

import java.util.Optional;

public class RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final RateLimiterPort rateLimiter;

    public RequestPasswordResetUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            TokenGeneratorPort tokenGenerator,
            TokenHasherPort tokenHasher,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
    }

    public void execute(RequestPasswordResetInput input) {
        // 1. Validate email format
        Email email = new Email(input.email());

        // Per-email cooldown (§7.9): 3 sends/hour. Dropping silently keeps the
        // endpoint's unconditional 204 — no enumeration, no bombing, no SES burn.
        if (!rateLimiter.tryAcquire("rl:pwreset:email:" + email.value(), 3, Duration.ofHours(1))) {
            return;
        }

        // 2. Find user — silently do nothing if not found. §7.5: do the same
        //    token work + a DB round-trip as the real path first, so response
        //    timing doesn't reveal whether the email is registered.
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            equalizeTiming();
            return;
        }
        User user = maybeUser.get();

        // 3. Expire old tokens + persist new one atomically. Raw token only flows through the event.
        String rawToken = tokenGenerator.generatePasswordResetToken();
        PasswordResetToken passwordResetToken = PasswordResetToken.issue(
                idGenerator.newId(), user.getId(), tokenHasher.hash(rawToken));

        transactionRunner.run(() -> {
            passwordResetTokenRepository.expireAllByUserId(user.getId());
            passwordResetTokenRepository.save(passwordResetToken);
        });

        // 4. Publish password reset email event — AFTER commit
        eventPublisher.publish(new PasswordResetEmailRequestedEvent(
                user.getEmail().value(), user.getName().value(), rawToken, user.getLanguage()));
    }

    /** The not-found counterpart of the exists path's token gen + hash + tx. */
    private void equalizeTiming() {
        tokenHasher.hash(tokenGenerator.generatePasswordResetToken());
        transactionRunner.run(() -> { });
    }
}
