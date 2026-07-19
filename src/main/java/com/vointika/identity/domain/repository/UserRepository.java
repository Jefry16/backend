package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.valueobject.Email;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(Email email);
}