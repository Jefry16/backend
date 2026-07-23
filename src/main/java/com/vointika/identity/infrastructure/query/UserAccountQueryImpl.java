package com.vointika.identity.infrastructure.query;

import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.vointika.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;
import com.vointika.shared.port.UserContactView;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
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

    @Override
    public List<UserAccountView> findAccounts(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByIdIn(userIds).stream()
                .map(u -> new UserAccountView(u.getId(), u.getEmail(), u.getName()))
                .toList();
    }
}
