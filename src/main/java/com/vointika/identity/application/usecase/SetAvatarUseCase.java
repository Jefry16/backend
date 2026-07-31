package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.SetAvatarInput;
import com.vointika.identity.application.dto.output.SetAvatarOutput;
import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SetAvatarUseCase {


    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    // Raster images only — SVG is excluded for the same script-injection
    // reason the media library excludes it (§7.8).
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final UserRepository userRepository;
    private final AvatarStoragePort avatarStoragePort;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;

    public SetAvatarUseCase(
            UserRepository userRepository,
            AvatarStoragePort avatarStoragePort,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.avatarStoragePort = avatarStoragePort;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
    }

    public SetAvatarOutput execute(SetAvatarInput input) {
        // 1. Validate content type and size
        String contentType = normalizeContentType(input.contentType());
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new InvalidFieldException(
                    "Unsupported avatar content type: allowed image/jpeg, image/png, image/webp");
        }
        if (input.sizeBytes() <= 0) {
            throw new InvalidFieldException("File is empty");
        }
        if (input.sizeBytes() > MAX_AVATAR_BYTES) {
            throw new InvalidFieldException("File too large: max 5 MB");
        }

        User user = userRepository.findById(UUID.fromString(input.userId()))
                .orElseThrow(() -> new UnauthorizedException("Invalid authenticated user"));
        String oldKey = user.getAvatarKey();

        // 2. Unique key per upload, so a changed avatar is never served from a
        // stale browser cache of the previous URL.
        String newKey = "users/" + user.getId() + "/" + idGenerator.newId() + "-avatar." + extension;

        // 3. Upload OUTSIDE the transaction — it can't roll back anyway (a
        // failed save strands the object, never a row without an object).
        avatarStoragePort.putObject(newKey, contentType, input.sizeBytes(), input.body());

        user.changeAvatar(newKey);
        transactionRunner.run(() -> userRepository.save(user));

        // 4. AFTER commit: best-effort cleanup of the replaced object.
        deleteQuietly(oldKey);

        return new SetAvatarOutput(newKey);
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        avatarStoragePort.deleteObject(key);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidFieldException("Content type is required");
        }
        // Strip MIME parameters ("image/png; charset=utf-8" -> "image/png")
        int semicolon = contentType.indexOf(';');
        String bare = (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType);
        return bare.trim().toLowerCase(Locale.ROOT);
    }
}
