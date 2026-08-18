package com.vointika.metafield.application.usecase;

import com.vointika.metafield.infrastructure.port.JacksonJsonSyntaxPort;
import com.vointika.shared.exception.UniqueConstraintViolationException;
import com.vointika.metafield.application.dto.input.UpsertMetafieldValueInput;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

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

class MetafieldValueUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DEF = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID OWNER = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final UUID VALUE_ID = UUID.fromString("cccccccc-0000-4000-8000-000000000003");

    private MetafieldDefinitionRepository definitionRepository;
    private MetafieldValueRepository valueRepository;
    private MetaobjectEntryRepository metaobjectEntryRepository;
    private ExperienceOwnershipQuery experienceOwnershipQuery;
    private PageOwnershipQuery pageOwnershipQuery;
    private MetafieldOwnerAccess ownerAccess;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(MetafieldDefinitionRepository.class);
        valueRepository = mock(MetafieldValueRepository.class);
        metaobjectEntryRepository = mock(MetaobjectEntryRepository.class);
        experienceOwnershipQuery = mock(ExperienceOwnershipQuery.class);
        pageOwnershipQuery = mock(PageOwnershipQuery.class);
        ownerAccess = new MetafieldOwnerAccess(experienceOwnershipQuery, pageOwnershipQuery);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(VALUE_ID);
        when(valueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pageOwnershipQuery.existsForTourOperator(OWNER, OP)).thenReturn(true);
        when(experienceOwnershipQuery.existsForTourOperator(OWNER, OP)).thenReturn(true);
        when(definitionRepository.requireByIdentity(OP, MetafieldOwnerType.PAGE, "custom", "subtitle"))
                .thenReturn(definition());
    }

    private MetafieldDefinition definition() {
        return new MetafieldDefinition(DEF, OP, MetafieldOwnerType.PAGE,
                new MetafieldNamespace("custom"), new MetafieldKey("subtitle"),
                MetafieldType.SINGLE_LINE_TEXT, null,
                new MetafieldDefinitionName("Subtitle"), null, USER);
    }

    private UpsertMetafieldValueUseCase upsert() {
        return new UpsertMetafieldValueUseCase(definitionRepository, valueRepository,
                metaobjectEntryRepository, ownerAccess,
                new MetafieldValueValidator(new JacksonJsonSyntaxPort(new ObjectMapper())), membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    private UpsertMetafieldValueInput input(String value) {
        return new UpsertMetafieldValueInput(
                USER, OP, MetafieldOwnerType.PAGE, OWNER, "custom", "subtitle", value);
    }

    @Test
    void firstSetCreatesTheRowAndAuditsOnThePagesTimeline() {
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.empty());

        upsert().execute(input("Boat tours since 1998"));

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(valueRepository).save(any());
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().entityType()).isEqualTo("PAGE");
        assertThat(captor.getValue().action()).isEqualTo("page.metafield_updated");
    }

    @Test
    void sameValueResetRecordsNothing() {
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.of(
                new MetafieldValue(VALUE_ID, DEF, OWNER, "Boat tours since 1998", USER)));

        upsert().execute(input("Boat tours since 1998"));

        verify(valueRepository).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void lostFirstSetRaceIs409NotA500() {
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.empty());
        when(valueRepository.save(any()))
                .thenThrow(new UniqueConstraintViolationException("duplicate", null));

        assertThatThrownBy(() -> upsert().execute(input("Boat tours since 1998")))
                .isInstanceOf(com.vointika.shared.exception.ConflictException.class);
    }

    @Test
    void referenceValueMustBeAnEntryOfThePinnedType() {
        UUID pin = UUID.fromString("cccccccc-0000-4000-8000-0000000000aa");
        UUID entry = UUID.fromString("cccccccc-0000-4000-8000-0000000000bb");
        when(definitionRepository.requireByIdentity(OP, MetafieldOwnerType.PAGE, "custom", "subtitle"))
                .thenReturn(new MetafieldDefinition(DEF, OP, MetafieldOwnerType.PAGE,
                        new MetafieldNamespace("custom"), new MetafieldKey("subtitle"),
                        MetafieldType.METAOBJECT_REFERENCE, pin,
                        new MetafieldDefinitionName("Size chart"), null, USER));
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.empty());

        // Not a UUID at all → 422 from the validator.
        assertThatThrownBy(() -> upsert().execute(input("not-an-id")))
                .isInstanceOf(com.vointika.shared.exception.InvalidFieldException.class);
        // A UUID that isn't an entry of the pinned type → 422.
        when(metaobjectEntryRepository.existsByIdAndDefinitionIdAndTourOperatorId(entry, pin, OP))
                .thenReturn(false);
        assertThatThrownBy(() -> upsert().execute(input(entry.toString())))
                .isInstanceOf(com.vointika.shared.exception.InvalidFieldException.class)
                .hasMessageContaining("pinned type");
        // A real entry of the pinned type saves.
        when(metaobjectEntryRepository.existsByIdAndDefinitionIdAndTourOperatorId(entry, pin, OP))
                .thenReturn(true);
        upsert().execute(input(entry.toString()));
        verify(valueRepository).save(any());
    }

    @Test
    void foreignOwnerIs404() {
        when(pageOwnershipQuery.existsForTourOperator(OWNER, OP)).thenReturn(false);
        assertThatThrownBy(() -> upsert().execute(input("x")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Page not found");
    }

    @Test
    void unknownDefinitionIs404() {
        when(definitionRepository.requireByIdentity(OP, MetafieldOwnerType.PAGE, "custom", "subtitle"))
                .thenThrow(new ResourceNotFoundException("identity lookup missed"));
        assertThatThrownBy(() -> upsert().execute(input("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    private DeleteMetafieldValueUseCase delete() {
        return new DeleteMetafieldValueUseCase(definitionRepository, valueRepository, ownerAccess,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Test
    void deleteClearsAndAuditsOnTheOwnersTimeline() {
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.of(
                new MetafieldValue(VALUE_ID, DEF, OWNER, "old", USER)));

        delete().execute(OP, MetafieldOwnerType.PAGE, OWNER, "custom", "subtitle", USER);

        verify(valueRepository).delete(VALUE_ID);
        ArgumentCaptor<NewAuditEntry> captor = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("page.metafield_cleared");
    }

    @Test
    void deleteOfUnsetValueIsIdempotentAndRecordsNothing() {
        when(valueRepository.findByDefinitionIdAndOwnerId(DEF, OWNER)).thenReturn(Optional.empty());

        delete().execute(OP, MetafieldOwnerType.PAGE, OWNER, "custom", "subtitle", USER);

        verify(valueRepository, never()).delete(any());
        verify(auditTrailPort, never()).append(any());
    }
}
