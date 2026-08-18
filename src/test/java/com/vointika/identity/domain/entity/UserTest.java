package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateNewUserWithUnverifiedStatus() {
        User user = newUser();
        assertNotNull(user.getId());
        assertEquals("test@example.com", user.getEmail().value());
        assertEquals("Test User", user.getName().value());
        assertEquals(UserStatus.UNVERIFIED, user.getStatus());
        assertFalse(user.isVerified());
        assertNull(user.getAvatarKey());
        assertEquals("en", user.getLanguage());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldVerifyUser() {
        User user = newUser();
        user.verify();
        assertEquals(UserStatus.VERIFIED, user.getStatus());
        assertTrue(user.isVerified());
    }

    @Test
    void shouldThrowWhenVerifyingAlreadyVerifiedUser() {
        User user = newUser();
        user.verify();
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, user::verify);
        assertEquals("User is already verified", ex.getMessage());
    }

    @Test
    void shouldChangePassword() {
        User user = newUser();
        var updatedAtBefore = user.getUpdatedAt();
        user.changePassword("newHashedPassword");
        assertEquals("newHashedPassword", user.getHashedPassword());
        assertTrue(user.getUpdatedAt().isAfter(updatedAtBefore) || user.getUpdatedAt().equals(updatedAtBefore));
    }

    @Test
    void shouldChangeAvatar() {
        User user = newUser();
        var updatedAtBefore = user.getUpdatedAt();
        user.changeAvatar("users/123/abc-avatar.png");
        assertEquals("users/123/abc-avatar.png", user.getAvatarKey());
        assertFalse(user.getUpdatedAt().isBefore(updatedAtBefore));
    }

    @Test
    void shouldClearAvatar() {
        User user = newUser();
        user.changeAvatar("users/123/abc-avatar.png");
        var updatedAtBefore = user.getUpdatedAt();
        user.clearAvatar();
        assertNull(user.getAvatarKey());
        assertFalse(user.getUpdatedAt().isBefore(updatedAtBefore));
    }

    private User newUser() {
        return new User(
                java.util.UUID.randomUUID(),
                new Email("test@example.com"),
                new UserName("Test User"),
                "hashedPassword123"
        );
    }
}
