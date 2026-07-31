package com.vointika.audit.infrastructure.integration;

import com.vointika.audit.domain.entity.AuditLogEntry;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The adapter every audited mutation goes through (70 call sites) and which had
 * no test of its own. What matters here is that freezing the actor's name is
 * <em>best effort</em>: an unresolvable account must store a null name rather
 * than fail the append, because the append shares the caller's transaction and
 * failing it would roll back the business action it records.
 */
class AuditTrailPortImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID ENTITY = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private AuditLogEntryRepository repository;
    private UserAccountQuery userAccountQuery;
    private AuditTrailPortImpl port;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogEntryRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        port = new AuditTrailPortImpl(repository, userAccountQuery, idGenerator);
    }

    private NewAuditEntry entry(AuditActor actor) {
        return new NewAuditEntry(OP, actor, "PAGE", ENTITY, "page.updated", Map.of("handle", "about"));
    }

    private AuditLogEntry appended() {
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).append(captor.capture());
        return captor.getValue();
    }

    @Test
    void freezesTheActingUsersNameAtWriteTime() {
        when(userAccountQuery.findContact(USER))
                .thenReturn(Optional.of(new UserContactView("ada@example.com", "Ada Lovelace", "en")));

        port.append(entry(AuditActor.user(USER)));

        assertThat(appended().getActorName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void storesANullNameWhenTheAccountCannotBeResolved() {
        when(userAccountQuery.findContact(USER)).thenReturn(Optional.empty());

        port.append(entry(AuditActor.user(USER)));

        assertThat(appended().getActorName()).isNull();
    }

    @Test
    void doesNotLookUpAnythingForASystemActor() {
        port.append(entry(AuditActor.system()));

        assertThat(appended().getActorName()).isNull();
        verify(userAccountQuery, never()).findContact(any());
    }
}
