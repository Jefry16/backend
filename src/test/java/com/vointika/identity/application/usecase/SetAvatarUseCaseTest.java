package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.SetAvatarInput;
import com.vointika.identity.application.dto.output.SetAvatarOutput;
import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetAvatarUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private AvatarStoragePort avatarStoragePort;
    @Mock private IdGenerator idGenerator;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private SetAvatarUseCase useCase;
    private UUID userId;
    private UUID generatedId;

    @BeforeEach
    void setUp() {
        useCase = new SetAvatarUseCase(userRepository, avatarStoragePort, idGenerator, transactionRunner);
        userId = UUID.randomUUID();
        generatedId = UUID.randomUUID();
    }

    @Test
    void shouldUploadAvatarAndSaveKey() {
        User user = userWithAvatar(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(idGenerator.newId()).thenReturn(generatedId);
        InputStream body = new ByteArrayInputStream(new byte[]{1});

        SetAvatarOutput output = useCase.execute(input("image/png", 1024, body));

        String expectedKey = "users/" + userId + "/" + generatedId + "-avatar.png";
        assertEquals(expectedKey, output.avatarKey());
        assertEquals(expectedKey, user.getAvatarKey());
        verify(avatarStoragePort).putObject(expectedKey, "image/png", 1024, body);
        verify(userRepository).save(user);
        verify(avatarStoragePort, never()).deleteObject(anyString());
    }

    @Test
    void shouldDeleteReplacedObjectAfterSave() {
        User user = userWithAvatar("users/" + userId + "/old-avatar.jpg");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(idGenerator.newId()).thenReturn(generatedId);

        useCase.execute(input("image/jpeg", 1024, new ByteArrayInputStream(new byte[]{1})));

        InOrder inOrder = inOrder(userRepository, avatarStoragePort);
        inOrder.verify(userRepository).save(user);
        inOrder.verify(avatarStoragePort).deleteObject("users/" + userId + "/old-avatar.jpg");
        assertEquals("users/" + userId + "/" + generatedId + "-avatar.jpg", user.getAvatarKey());
    }

    @Test
    void shouldDelegateDeleteOfTheReplacedObject() {
        String oldKey = "users/" + userId + "/old-avatar.jpg";
        User user = userWithAvatar(oldKey);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(idGenerator.newId()).thenReturn(generatedId);

        SetAvatarOutput output = useCase.execute(input("image/webp", 1024, new ByteArrayInputStream(new byte[]{1})));

        assertEquals("users/" + userId + "/" + generatedId + "-avatar.webp", output.avatarKey());
        verify(userRepository).save(user);
        verify(avatarStoragePort).deleteObject(oldKey);
    }

    @Test
    void shouldStripContentTypeParameters() {
        User user = userWithAvatar(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(idGenerator.newId()).thenReturn(generatedId);

        useCase.execute(input("image/jpeg; charset=utf-8", 1024, new ByteArrayInputStream(new byte[]{1})));

        verify(avatarStoragePort).putObject(
                eq("users/" + userId + "/" + generatedId + "-avatar.jpg"),
                eq("image/jpeg"), anyLong(), any());
    }

    @Test
    void shouldRejectUnsupportedContentTypes() {
        for (String contentType : new String[]{"image/gif", "application/pdf", "image/svg+xml", null}) {
            assertThrows(InvalidFieldException.class,
                    () -> useCase.execute(input(contentType, 1024, new ByteArrayInputStream(new byte[]{1}))));
        }
        verifyNoInteractions(avatarStoragePort);
    }

    @Test
    void shouldRejectEmptyFile() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(input("image/png", 0, new ByteArrayInputStream(new byte[0]))));
        verifyNoInteractions(avatarStoragePort);
    }

    @Test
    void shouldRejectFileOverSizeLimit() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(input("image/png", 5L * 1024 * 1024 + 1, new ByteArrayInputStream(new byte[]{1}))));
        verifyNoInteractions(avatarStoragePort);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(input("image/png", 1024, new ByteArrayInputStream(new byte[]{1}))));
        verifyNoInteractions(avatarStoragePort);
    }

    private SetAvatarInput input(String contentType, long sizeBytes, InputStream body) {
        return new SetAvatarInput(userId, contentType, sizeBytes, body);
    }

    private User userWithAvatar(String avatarKey) {
        return new User(userId, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, avatarKey, "en", Instant.now(), Instant.now());
    }
}
