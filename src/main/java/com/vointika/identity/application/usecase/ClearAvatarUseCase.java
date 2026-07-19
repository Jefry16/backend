package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ClearAvatarInput;
import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClearAvatarUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClearAvatarUseCase.class);

    private final UserRepository userRepository;
    private final AvatarStoragePort avatarStoragePort;
    private final TransactionRunner transactionRunner;

    public ClearAvatarUseCase(
            UserRepository userRepository,
            AvatarStoragePort avatarStoragePort,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.avatarStoragePort = avatarStoragePort;
        this.transactionRunner = transactionRunner;
    }

    public void execute(ClearAvatarInput input) {
        User user = userRepository.findById(UUID.fromString(input.userId()))
                .orElseThrow(() -> new UnauthorizedException("Invalid authenticated user"));
        String oldKey = user.getAvatarKey();
        if (oldKey == null) {
            return; // idempotent — nothing to clear
        }

        user.clearAvatar();
        transactionRunner.run(() -> userRepository.save(user));

        // AFTER commit: best-effort cleanup of the removed object.
        try {
            avatarStoragePort.deleteObject(oldKey);
        } catch (RuntimeException e) {
            log.warn("Failed to delete cleared avatar object {}", oldKey, e);
        }
    }
}
