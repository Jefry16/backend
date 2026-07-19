package com.vointika.identity.infrastructure.query;

import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.vointika.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.vointika.shared.port.UserAccountView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountQueryImplTest {

    @Mock private UserJpaRepository jpa;

    private UserAccountQueryImpl query;

    @BeforeEach
    void setUp() {
        query = new UserAccountQueryImpl(jpa);
    }

    private UserJpaEntity user(UUID id, String email, String name) {
        UserJpaEntity u = mock(UserJpaEntity.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getEmail()).thenReturn(email);
        lenient().when(u.getName()).thenReturn(name);
        return u;
    }

    @Test
    void findAccountsReturnsIdEmailAndNamePerResolvedAccount() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        // Build the entity mocks BEFORE the outer when(...) — stubbing a mock
        // inside another's thenReturn is nested stubbing (UnfinishedStubbing).
        UserJpaEntity ua = user(a, "a@example.com", "Ada");
        UserJpaEntity ub = user(b, "b@example.com", "Bea");
        when(jpa.findByIdIn(List.of(a, b))).thenReturn(List.of(ua, ub));

        List<UserAccountView> views = query.findAccounts(List.of(a, b));

        assertEquals(2, views.size());
        assertEquals(new UserAccountView(a, "a@example.com", "Ada"), views.get(0));
        assertEquals(new UserAccountView(b, "b@example.com", "Bea"), views.get(1));
    }

    @Test
    void emptyInputShortCircuitsWithoutHittingTheRepository() {
        assertTrue(query.findAccounts(List.of()).isEmpty());
        verifyNoInteractions(jpa);
    }
}
