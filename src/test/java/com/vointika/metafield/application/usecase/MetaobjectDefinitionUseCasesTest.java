package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.AddMetaobjectFieldInput;
import com.vointika.metafield.application.dto.input.CreateMetaobjectDefinitionInput;
import com.vointika.metafield.application.dto.input.RenameMetaobjectFieldInput;
import com.vointika.metafield.application.dto.input.UpdateMetaobjectDefinitionInput;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaobjectDefinitionUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DEF = UUID.fromString("dddddddd-0000-4000-8000-000000000001");

    private MetaobjectDefinitionRepository repository;
    private MetafieldDefinitionRepository metafieldDefinitionRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        repository = mock(MetaobjectDefinitionRepository.class);
        metafieldDefinitionRepository = mock(MetafieldDefinitionRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.saveField(any())).thenAnswer(i -> i.getArgument(0));
    }

    private MetaobjectDefinition definition() {
        return new MetaobjectDefinition(DEF, OP, new MetaobjectType("size-chart"),
                new MetafieldDefinitionName("Size chart"), null, USER);
    }

    private MetaobjectField field(String key) {
        return new MetaobjectField(UUID.randomUUID(), DEF, new MetafieldKey(key),
                MetafieldType.SINGLE_LINE_TEXT, new MetafieldDefinitionName("Heading"), 1);
    }

    @Test
    void createSavesDefinitionWithFieldsAndAudits() {
        CreateMetaobjectDefinitionUseCase useCase = new CreateMetaobjectDefinitionUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        useCase.execute(new CreateMetaobjectDefinitionInput(USER, OP, "size-chart", "Size chart", null,
                List.of(new CreateMetaobjectDefinitionInput.FieldSpec("heading", "single_line_text", "Heading"),
                        new CreateMetaobjectDefinitionInput.FieldSpec("rows", "json", "Rows"))));

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
        verify(repository, times(2)).saveField(any());
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().entityType()).isEqualTo("METAOBJECT_DEFINITION");
        assertThat(captor.getValue().action()).isEqualTo("metaobject_definition.created");
    }

    @Test
    void createWithNoFieldsIs422() {
        CreateMetaobjectDefinitionUseCase useCase = new CreateMetaobjectDefinitionUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new CreateMetaobjectDefinitionInput(
                USER, OP, "size-chart", "Size chart", null, List.of())))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void createWithDuplicateFieldKeysIs422() {
        CreateMetaobjectDefinitionUseCase useCase = new CreateMetaobjectDefinitionUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new CreateMetaobjectDefinitionInput(
                USER, OP, "size-chart", "Size chart", null,
                List.of(new CreateMetaobjectDefinitionInput.FieldSpec("heading", "single_line_text", "A"),
                        new CreateMetaobjectDefinitionInput.FieldSpec("heading", "json", "B")))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Duplicate field key");
    }

    @Test
    void createDuplicateTypeIs409() {
        when(repository.existsByTourOperatorIdAndType(OP, "size-chart")).thenReturn(true);
        CreateMetaobjectDefinitionUseCase useCase = new CreateMetaobjectDefinitionUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new CreateMetaobjectDefinitionInput(
                USER, OP, "size-chart", "Size chart", null,
                List.of(new CreateMetaobjectDefinitionInput.FieldSpec("heading", "single_line_text", "A")))))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void updateDiffsOnlyRealChanges() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        UpdateMetaobjectDefinitionUseCase useCase = new UpdateMetaobjectDefinitionUseCase(
                repository, membershipCheck, transactionRunner, auditTrailPort);

        // No-op replace records nothing.
        useCase.execute(new UpdateMetaobjectDefinitionInput(USER, OP, DEF, "Size chart", null));
        verify(auditTrailPort, never()).append(any());

        useCase.execute(new UpdateMetaobjectDefinitionInput(USER, OP, DEF, "Sizing chart", null));
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject_definition.updated");
        assertThat(captor.getValue().changes()).hasSize(1);
    }

    @Test
    void addFieldDuplicateKeyIs409() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        when(repository.existsField(DEF, "heading")).thenReturn(true);
        AddMetaobjectFieldUseCase useCase = new AddMetaobjectFieldUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new AddMetaobjectFieldInput(
                USER, OP, DEF, "heading", "single_line_text", "Heading")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void addFieldAppendsAfterTheHighestPositionNotTheCount() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        // Two surviving fields at positions 1 and 4 (2 and 3 were removed):
        // the next position must be 5, not count+1=3 (which would collide).
        MetaobjectField last = new MetaobjectField(UUID.randomUUID(), DEF,
                new MetafieldKey("rows"), MetafieldType.JSON,
                new MetafieldDefinitionName("Rows"), 4);
        when(repository.fieldsOf(DEF)).thenReturn(List.of(field("heading"), last));
        AddMetaobjectFieldUseCase useCase = new AddMetaobjectFieldUseCase(
                repository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);

        useCase.execute(new AddMetaobjectFieldInput(USER, OP, DEF, "note", "multi_line_text", "Note"));

        ArgumentCaptor<MetaobjectField> saved = ArgumentCaptor.forClass(MetaobjectField.class);
        verify(repository).saveField(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo(5);
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject_definition.field_added");
    }

    @Test
    void renameFieldNoOpRecordsNothing() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        when(repository.findField(DEF, "heading")).thenReturn(Optional.of(field("heading")));
        RenameMetaobjectFieldUseCase useCase = new RenameMetaobjectFieldUseCase(
                repository, membershipCheck, transactionRunner, auditTrailPort);

        useCase.execute(new RenameMetaobjectFieldInput(USER, OP, DEF, "heading", "Heading"));

        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void removeLastFieldIs409() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        when(repository.findField(DEF, "heading")).thenReturn(Optional.of(field("heading")));
        when(repository.countFields(DEF)).thenReturn(1L);
        RemoveMetaobjectFieldUseCase useCase = new RemoveMetaobjectFieldUseCase(
                repository, membershipCheck, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(OP, DEF, "heading", USER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("at least one field");
    }

    @Test
    void deleteIsRefusedWhileAReferenceMetafieldPinsTheType() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        when(metafieldDefinitionRepository.existsPinningMetaobjectDefinition(DEF)).thenReturn(true);
        DeleteMetaobjectDefinitionUseCase useCase = new DeleteMetaobjectDefinitionUseCase(
                repository, metafieldDefinitionRepository, membershipCheck, transactionRunner, auditTrailPort);

        // Asked, not caught: the FK raises DataIntegrityViolationException, which
        // SpringTransactionRunner deliberately leaves untranslated — so the
        // catch this replaced could never fire and the delete 500'd.
        assertThatThrownBy(() -> useCase.execute(OP, DEF, USER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("references this metaobject type");

        verify(repository, never()).delete(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void deleteAuditsWithIdentityInDetails() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));
        DeleteMetaobjectDefinitionUseCase useCase = new DeleteMetaobjectDefinitionUseCase(
                repository, metafieldDefinitionRepository, membershipCheck, transactionRunner, auditTrailPort);

        useCase.execute(OP, DEF, USER);

        verify(repository).delete(DEF);
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("metaobject_definition.deleted");
        assertThat(captor.getValue().details()).containsEntry("type", "size-chart");
    }
}
