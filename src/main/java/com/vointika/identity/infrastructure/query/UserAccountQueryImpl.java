package com.vointika.identity.infrastructure.query;

import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.vointika.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserAccountQueryImpl implements UserAccountQuery {

    private final UserJpaRepository jpa;

    public UserAccountQueryImpl(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::getId);
    }

    @Override
    public Optional<UserContactView> findContact(UUID userId) {
        return jpa.findById(userId)
                .map(u -> new UserContactView(u.getEmail(), u.getName(), u.getLanguage()));
    }
}
