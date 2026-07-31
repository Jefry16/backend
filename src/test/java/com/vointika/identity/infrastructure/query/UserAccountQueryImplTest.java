package com.vointika.identity.infrastructure.query;

import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.vointika.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.vointika.shared.port.UserContactView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * identity's side of the cross-context account seam. {@code findContact} is the
 * single-id lookup every other context resolves a user through — including the
 * audit adapter, which freezes the actor's name on every audited mutation.
 */
@ExtendWith(MockitoExtension.class)
class UserAccountQueryImplTest {

    @Mock private UserJpaRepository jpa;

    private UserAccountQueryImpl query;

    @BeforeEach
    void setUp() {
        query = new UserAccountQueryImpl(jpa);
    }

    /**
     * Build the entity mock BEFORE the outer {@code when(...)} at every call
     * site — stubbing a mock inside another's {@code thenReturn} is nested
     * stubbing and Mockito raises UnfinishedStubbing.
     */
    private UserJpaEntity user(UUID id, String email, String name, String language) {
        UserJpaEntity u = mock(UserJpaEntity.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getEmail()).thenReturn(email);
        lenient().when(u.getName()).thenReturn(name);
        lenient().when(u.getLanguage()).thenReturn(language);
        return u;
    }

    @Test
    void findContactReturnsEmailNameAndLanguage() {
        UUID id = UUID.randomUUID();
        UserJpaEntity u = user(id, "ada@example.com", "Ada", "en");
        when(jpa.findById(id)).thenReturn(Optional.of(u));

        assertEquals(Optional.of(new UserContactView("ada@example.com", "Ada", "en")),
                query.findContact(id));
    }

    @Test
    void findContactIsEmptyForAnUnknownId() {
        UUID id = UUID.randomUUID();
        when(jpa.findById(id)).thenReturn(Optional.empty());

        assertTrue(query.findContact(id).isEmpty());
    }

    @Test
    void findUserIdByEmailReturnsTheId() {
        UUID id = UUID.randomUUID();
        UserJpaEntity u = user(id, "ada@example.com", "Ada", "en");
        when(jpa.findByEmail("ada@example.com")).thenReturn(Optional.of(u));

        assertEquals(Optional.of(id), query.findUserIdByEmail("ada@example.com"));
    }

    @Test
    void findUserIdByEmailIsEmptyForAnUnknownEmail() {
        when(jpa.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertTrue(query.findUserIdByEmail("nobody@example.com").isEmpty());
    }
}
