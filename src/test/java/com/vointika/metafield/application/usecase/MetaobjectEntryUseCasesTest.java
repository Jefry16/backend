package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.CreateMetaobjectEntryInput;
import com.vointika.metafield.application.dto.input.UpdateMetaobjectEntryInput;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.valueobject.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaobjectEntryUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DEF = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    private static final UUID ENTRY = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID FIELD = UUID.fromString("ffffffff-0000-4000-8000-000000000001");

    private MetaobjectDefinitionRepository definitionRepository;
    private MetaobjectEntryRepository entryRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(MetaobjectDefinitionRepository.class);
        entryRepository = mock(MetaobjectEntryRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(entryRepository.saveValue(any())).thenAnswer(i -> i.getArgument(0));
        when(definitionRepository.findByIdAndTourOperatorId(DEF, OP))
                .thenReturn(Optional.of(definition()));
        when(definitionRepository.fieldsOf(DEF)).thenReturn(List.of(headingField()));
    }

    private MetaobjectDefinition definition() {
        return new MetaobjectDefinition(DEF, OP, new MetaobjectType("size-chart"),
                new MetafieldDefinitionName("Size chart"), null, USER);
    }

    private MetaobjectField headingField() {
        return new MetaobjectField(FIELD, DEF, new MetafieldKey("heading"),
                MetafieldType.SINGLE_LINE_TEXT, new MetafieldDefinitionName("Heading"), 1);
    }

    private MetaobjectEntry entry() {
        return new MetaobjectEntry(ENTRY, OP, DEF, new Slug("beginner-chart"),
                new MetaobjectEntryName("Beginner chart"), USER);
    }

    private CreateMetaobjectEntryUseCase createUseCase() {
        return new CreateMetaobjectEntryUseCase(definitionRepository, entryRepository,
                new MetafieldValueValidator(new ObjectMapper()), membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    private UpdateMetaobjectEntryUseCase updateUseCase() {
        return new UpdateMetaobjectEntryUseCase(definitionRepository, entryRepository,
                new MetafieldValueValidator(new ObjectMapper()), membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Test
    void createSavesEntryWithValuesAndAudits() {
        createUseCase().execute(new CreateMetaobjectEntryInput(
                USER, OP, DEF, "beginner-chart", "Beginner chart",
                Map.of("heading", "Pick your kayak size")));

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(entryRepository).save(any());
        verify(entryRepository).saveValue(any());
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().entityType()).isEqualTo("METAOBJECT");
        assertThat(captor.getValue().action()).isEqualTo("metaobject.created");
    }

    @Test
    void createSkipsBlankValuesAndRejectsUnknownKeys() {
        createUseCase().execute(new CreateMetaobjectEntryInput(
                USER, OP, DEF, "beginner-chart", "Beginner chart", Map.of("heading", "  ")));
        verify(entryRepository, never()).saveValue(any());

        assertThatThrownBy(() -> createUseCase().execute(new CreateMetaobjectEntryInput(
                USER, OP, DEF, "other", "Other", Map.of("nope", "x"))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Unknown metaobject field");
    }

    @Test
    void createDuplicateHandleIs409() {
        when(entryRepository.existsByDefinitionIdAndHandle(DEF, "beginner-chart")).thenReturn(true);

        assertThatThrownBy(() -> createUseCase().execute(new CreateMetaobjectEntryInput(
                USER, OP, DEF, "beginner-chart", "Beginner chart", Map.of())))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void updateDiffsValuesByFieldKey() {
        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OP)).thenReturn(Optional.of(entry()));
        when(entryRepository.findValue(ENTRY, FIELD)).thenReturn(Optional.of(
                new MetaobjectEntryValue(UUID.randomUUID(), ENTRY, FIELD, "Old heading", USER)));

        updateUseCase().execute(new UpdateMetaobjectEntryInput(
                USER, OP, ENTRY, null, null, Map.of("heading", "New heading")));

        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject.updated");
        FieldChange change = captor.getValue().changes().get(0);
        assertThat(change.field()).isEqualTo("heading");
        assertThat(change.from()).isEqualTo("Old heading");
        assertThat(change.to()).isEqualTo("New heading");
    }

    @Test
    void updateClearsWithNullAndSkipsNoOps() {
        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OP)).thenReturn(Optional.of(entry()));
        MetaobjectEntryValue stored =
                new MetaobjectEntryValue(UUID.randomUUID(), ENTRY, FIELD, "Old heading", USER);
        when(entryRepository.findValue(ENTRY, FIELD)).thenReturn(Optional.of(stored));

        Map<String, String> clearing = new HashMap<>();
        clearing.put("heading", null);
        updateUseCase().execute(new UpdateMetaobjectEntryInput(USER, OP, ENTRY, null, null, clearing));
        verify(entryRepository).deleteValue(stored.getId());

        // Same value + same identity → nothing saved as a change, no audit.
        updateUseCase().execute(new UpdateMetaobjectEntryInput(
                USER, OP, ENTRY, "Beginner chart", "beginner-chart", Map.of("heading", "Old heading")));
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture()); // only the clear above
        assertThat(captor.getAllValues()).hasSize(1);
    }

    @Test
    void updateToTakenHandleIs409() {
        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OP)).thenReturn(Optional.of(entry()));
        when(entryRepository.existsByDefinitionIdAndHandle(DEF, "taken")).thenReturn(true);

        assertThatThrownBy(() -> updateUseCase().execute(new UpdateMetaobjectEntryInput(
                USER, OP, ENTRY, null, "taken", null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void publishFlipsOnceThen409s() {
        MetaobjectEntry entry = entry();
        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OP)).thenReturn(Optional.of(entry));
        PublishMetaobjectEntryUseCase useCase = new PublishMetaobjectEntryUseCase(
                entryRepository, membershipCheck, transactionRunner, auditTrailPort);

        useCase.execute(OP, ENTRY, USER);
        assertThat(entry.isPublished()).isTrue();
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject.published");

        assertThatThrownBy(() -> useCase.execute(OP, ENTRY, USER))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteAuditsWithIdentityInDetails() {
        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OP)).thenReturn(Optional.of(entry()));
        DeleteMetaobjectEntryUseCase useCase = new DeleteMetaobjectEntryUseCase(
                entryRepository, membershipCheck, transactionRunner, auditTrailPort);

        useCase.execute(OP, ENTRY, USER);

        verify(entryRepository).delete(ENTRY);
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject.deleted");
        assertThat(captor.getValue().details()).containsEntry("handle", "beginner-chart");
    }
}
