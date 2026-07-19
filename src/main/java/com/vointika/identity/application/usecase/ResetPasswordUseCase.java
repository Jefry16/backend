package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ResetPasswordInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Password;
import com.vointika.shared.port.TransactionRunner;

public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenHasherPort tokenHasher;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;

    public ResetPasswordUseCase(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasher,
            TokenHasherPort tokenHasher,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    public void execute(ResetPasswordInput input) {
        // 1. Validate new password rules
        Password newPassword = new Password(input.newPassword());

        // 2. Find reset token (hash incoming raw value)
        PasswordResetToken passwordResetToken = passwordResetTokenRepository
                .findByTokenHash(tokenHasher.hash(input.token()))
                .orElseThrow(() -> new UnauthorizedException("Invalid password reset token"));

        // 3. Validate token — domain enforces its own rules
        passwordResetToken.use();

        // 4. Find user
        User user = userRepository
                .findById(passwordResetToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid password reset token"));

        // 5. Guard — new password must differ from current
        if (passwordHasher.matches(newPassword.value(), user.getHashedPassword())) {
            throw new InvalidFieldException("New password must be different from current password");
        }

        // 6. Hash new password and update user
        String hashedNewPassword = passwordHasher.hash(newPassword.value());
        user.changePassword(hashedNewPassword);

        // 7. Persist token, user, and revoke all refresh tokens atomically
        transactionRunner.run(() -> {
            passwordResetTokenRepository.save(passwordResetToken);
            userRepository.save(user);
            refreshTokenRepository.revokeAllByUserId(user.getId());
        });

        // 8. Publish password changed event — AFTER commit
        eventPublisher.publish(new PasswordChangedEvent(
                user.getEmail().value(), user.getName().value()));
    }
}
