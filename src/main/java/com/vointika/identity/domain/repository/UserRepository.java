package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.valueobject.Email;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(Email email);

    /**
     * The authenticated principal's own account. Five self-service use cases start
     * this way, and an absent row means the token outlived its user — a 401, not a
     * 404 (see {@link UserAccountQuery#INVALID_PRINCIPAL}).
     *
     * <p>Not for looking up <em>another</em> user: nothing here checks that the id
     * came from the principal, so a caller passing an arbitrary id would get that
     * user back. Every current caller passes {@code input.userId()}.
     */
    default User requireById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new UnauthorizedException(UserAccountQuery.INVALID_PRINCIPAL));
    }
}