package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ChangePasswordInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Password;
import com.vointika.shared.port.TransactionRunner;

import java.util.UUID;

public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasher,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    public void execute(ChangePasswordInput input) {
        // 1. Validate new password rules
        Password newPassword = new Password(input.newPassword());

        // 2. Find user
        User user = userRepository
                .findById(UUID.fromString(input.userId()))
                .orElseThrow(() -> new UnauthorizedException("Invalid authenticated user"));

        // 3. Verify current password
        if (!passwordHasher.matches(input.currentPassword(), user.getHashedPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        // 4. Guard — new password must differ from current
        if (passwordHasher.matches(newPassword.value(), user.getHashedPassword())) {
            throw new InvalidFieldException("New password must be different from current password");
        }

        // 5. Hash new password and update user
        String hashedNewPassword = passwordHasher.hash(newPassword.value());
        user.changePassword(hashedNewPassword);

        // 6. Save user + revoke all refresh tokens atomically
        transactionRunner.run(() -> {
            userRepository.save(user);
            refreshTokenRepository.revokeAllByUserId(user.getId());
        });

        // 7. Publish password changed event — AFTER commit
        eventPublisher.publish(new PasswordChangedEvent(
                user.getEmail().value(), user.getName().value(), user.getLanguage()));
    }
}
