package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ClearAvatarInput;
import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearAvatarUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private AvatarStoragePort avatarStoragePort;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private ClearAvatarUseCase useCase;
    private UUID userId;

    @BeforeEach
    void setUp() {
        useCase = new ClearAvatarUseCase(userRepository, avatarStoragePort, transactionRunner);
        userId = UUID.randomUUID();
    }

    @Test
    void shouldClearAvatarAndDeleteObjectAfterSave() {
        User user = userWithAvatar("users/" + userId + "/abc-avatar.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ClearAvatarInput(userId.toString()));

        assertNull(user.getAvatarKey());
        InOrder inOrder = inOrder(userRepository, avatarStoragePort);
        inOrder.verify(userRepository).save(user);
        inOrder.verify(avatarStoragePort).deleteObject("users/" + userId + "/abc-avatar.png");
    }

    @Test
    void shouldBeNoOpWhenNoAvatarSet() {
        User user = userWithAvatar(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ClearAvatarInput(userId.toString()));

        verify(userRepository, never()).save(user);
        verifyNoInteractions(avatarStoragePort);
    }

    /** Deleting the object is delegated, not conditional: the port is best-effort by
     *  contract, so resilience is asserted in S3AvatarStoragePortImplTest, not here. */
    @Test
    void shouldDelegateTheObjectDeleteAfterClearing() {
        String key = "users/" + userId + "/abc-avatar.png";
        User user = userWithAvatar(key);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ClearAvatarInput(userId.toString()));

        assertNull(user.getAvatarKey());
        verify(userRepository).save(user);
        verify(avatarStoragePort).deleteObject(key);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new ClearAvatarInput(userId.toString())));
        verifyNoInteractions(avatarStoragePort);
    }

    private User userWithAvatar(String avatarKey) {
        return new User(userId, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, avatarKey, "en", Instant.now(), Instant.now());
    }
}
