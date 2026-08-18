package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private final Email email;
    private UserName name;
    private String hashedPassword;
    private UserStatus status;
    private String avatarKey;
    private String language;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new user
    public User(UUID id, Email email, UserName name, String hashedPassword) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.status = UserStatus.UNVERIFIED;
        this.avatarKey = null;
        this.language = "en";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for reconstituting from persistence
    public User(UUID id, Email email, UserName name, String hashedPassword,
                UserStatus status, String avatarKey, String language,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.status = status;
        this.avatarKey = avatarKey;
        this.language = language;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void verify() {
        if (this.status == UserStatus.VERIFIED) {
            throw new InvalidFieldException("User is already verified");
        }
        this.status = UserStatus.VERIFIED;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newHashedPassword) {
        this.hashedPassword = newHashedPassword;
        this.updatedAt = Instant.now();
    }

    public void changeAvatar(String avatarKey) {
        this.avatarKey = avatarKey;
        this.updatedAt = Instant.now();
    }

    public void clearAvatar() {
        this.avatarKey = null;
        this.updatedAt = Instant.now();
    }

    public void changeLanguage(String language) {
        this.language = language;
        this.updatedAt = Instant.now();
    }

    public boolean isVerified() {
        return this.status == UserStatus.VERIFIED;
    }

    public UUID getId() { return id; }
    public Email getEmail() { return email; }
    public UserName getName() { return name; }
    public String getHashedPassword() { return hashedPassword; }
    public UserStatus getStatus() { return status; }
    public String getAvatarKey() { return avatarKey; }
    public String getLanguage() { return language; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}