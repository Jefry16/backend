package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.CreateMetafieldDefinitionInput;
import com.vointika.metafield.application.dto.input.UpdateMetafieldDefinitionInput;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetafieldDefinitionUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DEF = UUID.fromString("cccccccc-0000-4000-8000-000000000001");

    private MetafieldDefinitionRepository repository;
    private MetaobjectDefinitionRepository metaobjectDefinitionRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        repository = mock(MetafieldDefinitionRepository.class);
        metaobjectDefinitionRepository = mock(MetaobjectDefinitionRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        when(transactionRunner.call(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(DEF);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        // Run the real default: Mockito stubs a `default` like any other method,
        // so stubbing require* directly would make the assertions below hold for
        // any value (PATTERNS §9).
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(any(), any());
    }

    private MetafieldDefinition definition() {
        return new MetafieldDefinition(DEF, OP, MetafieldOwnerType.PAGE,
                new MetafieldNamespace("custom"), new MetafieldKey("subtitle"),
                MetafieldType.SINGLE_LINE_TEXT, null,
                new MetafieldDefinitionName("Subtitle"), null, USER);
    }

    private CreateMetafieldDefinitionInput createInput(String ownerType, String type) {
        return createInput(ownerType, type, null);
    }

    private CreateMetafieldDefinitionInput createInput(String ownerType, String type, UUID pin) {
        return new CreateMetafieldDefinitionInput(
                USER, OP, ownerType, "custom", "subtitle", type, pin, "Subtitle", null);
    }

    @Test
    void createPersistsAndAuditsForBothOwnerTypes() {
        when(repository.existsByIdentity(any(), any(), any(), any())).thenReturn(false);

        UUID id = create().execute(createInput("page", "single_line_text"));

        assertThat(id).isEqualTo(DEF);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    @Test
    void createRejectsDuplicateIdentity() {
        when(repository.existsByIdentity(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> create().execute(createInput("experience", "boolean")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownOwnerTypeAndType() {
        assertThatThrownBy(() -> create().execute(createInput("order", "boolean")))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> create().execute(createInput("page", "color")))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void referenceTypeRequiresAndValidatesThePin() {
        // Missing pin → 422.
        assertThatThrownBy(() -> create().execute(createInput("page", "metaobject_reference")))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("must pin");
        // Pin that isn't the operator's → 422.
        UUID pin = UUID.randomUUID();
        when(metaobjectDefinitionRepository.findByIdAndTourOperatorId(pin, OP))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> create().execute(createInput("page", "metaobject_reference", pin)))
                .isInstanceOf(InvalidFieldException.class);
        // A pin on a scalar type → 422.
        assertThatThrownBy(() -> create().execute(createInput("page", "boolean", pin)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("only applies");
    }

    @Test
    void updateChangesNameAndAuditsTheDiff() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));

        update().execute(new UpdateMetafieldDefinitionInput(USER, OP, DEF, "Sub-heading", "Shown under the title"));

        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    @Test
    void noOpUpdateSavesButRecordsNothing() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));

        update().execute(new UpdateMetafieldDefinitionInput(USER, OP, DEF, "Subtitle", null));

        verify(repository).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void deleteRemovesAndRecordsTheIdentity() {
        when(repository.findByIdAndTourOperatorId(DEF, OP)).thenReturn(Optional.of(definition()));

        new DeleteMetafieldDefinitionUseCase(repository, membershipCheck, transactionRunner, auditTrailPort)
                .execute(OP, DEF, USER);

        verify(repository).delete(DEF);
        verify(auditTrailPort).append(any());
    }

    private CreateMetafieldDefinitionUseCase create() {
        return new CreateMetafieldDefinitionUseCase(repository, metaobjectDefinitionRepository,
                membershipCheck, idGenerator, transactionRunner, auditTrailPort);
    }

    private UpdateMetafieldDefinitionUseCase update() {
        return new UpdateMetafieldDefinitionUseCase(repository, membershipCheck,
                transactionRunner, auditTrailPort);
    }
}
