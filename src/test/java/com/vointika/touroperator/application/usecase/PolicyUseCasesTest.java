package com.vointika.touroperator.application.usecase;

import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.input.CreatePolicyInput;
import com.vointika.touroperator.application.dto.input.UpdatePolicyInput;
import com.vointika.touroperator.application.dto.input.UpsertPolicyTranslationInput;
import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The seven store-policy use cases — the canonical document and its overlays. */
class PolicyUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID POLICY_ID = UUID.fromString("019f8000-0000-7000-8000-000000000001");

    private TourOperatorRepository operatorRepository;
    private TourOperatorPolicyRepository policyRepository;
    private TourOperatorPolicyTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;
    private IdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        policyRepository = mock(TourOperatorPolicyRepository.class);
        translationRepository = mock(TourOperatorPolicyTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(POLICY_ID);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(operatorLocalesQuery.findSupportedLocales(OP)).thenReturn(Set.of("en", "es"));
    }

    private TourOperator operator() {
        return new TourOperator(OP, new TourOperatorName("Acme"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Somewhere 1", null, "Palma", null, null, UUID.randomUUID()),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en"), LocaleCode.of("es")));
    }

    private Policy existing() {
        return new Policy(POLICY_ID, OP, PolicyType.CANCELLATION,
                new PolicyTitle("Cancellation policy"), new PolicyBody("<p>Old</p>"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private CreatePolicyUseCase create() {
        return new CreatePolicyUseCase(operatorRepository, policyRepository,
                membershipCheck, transactionRunner, auditTrailPort, idGenerator);
    }

    private UpdatePolicyUseCase update() {
        return new UpdatePolicyUseCase(policyRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    private DeletePolicyUseCase delete() {
        return new DeletePolicyUseCase(policyRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    private UpsertPolicyTranslationUseCase upsertTranslation() {
        return new UpsertPolicyTranslationUseCase(policyRepository, translationRepository,
                new OperatorLocaleCheck(operatorLocalesQuery), membershipCheck, transactionRunner, auditTrailPort);
    }


    @Test
    void createWritesThePolicyAndAudits() {
        when(policyRepository.existsByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(false);

        UUID id = create().execute(OP,
                new CreatePolicyInput("CANCELLATION", "Cancellation policy",
                        "<p>Free up to 48h before.</p>"), USER);

        assertThat(id).isEqualTo(POLICY_ID);
        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(POLICY_ID);
        assertThat(saved.getValue().type()).isEqualTo(PolicyType.CANCELLATION);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.policy_created");
        assertThat(audit.getValue().details()).containsEntry("type", "CANCELLATION");
    }

    @Test
    void creatingASecondPolicyOfTheSameTypeIs409() {
        // One per type is a UNIQUE constraint; this is the pre-check answering in
        // the use case's own terms before the database has to.
        when(policyRepository.existsByTourOperatorIdAndType(OP, PolicyType.PRIVACY))
                .thenReturn(true);

        assertThatThrownBy(() -> create().execute(OP,
                new CreatePolicyInput("PRIVACY", "Privacy", "<p>b</p>"), USER))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(policyRepository, never()).save(any());
    }

    @Test
    void anUnknownTypeInTheBodyIs422NotA404() {
        // A body field, unlike a path segment naming no resource.
        assertThatThrownBy(() -> create().execute(OP,
                new CreatePolicyInput("REFUNDS", "t", "<p>b</p>"), USER))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void theAuditEntryNeverCarriesTheBody() {
        // The trail is not a revision store, and a policy body is up to 256 KiB of
        // HTML. Same rule the contact context follows when it audits by summary.
        when(policyRepository.existsByTourOperatorIdAndType(OP, PolicyType.PRIVACY))
                .thenReturn(false);

        create().execute(OP,
                new CreatePolicyInput("PRIVACY", "Privacy", "<p>SECRET-MARKER</p>"), USER);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().details().toString()).doesNotContain("SECRET-MARKER");
    }

    @Test
    void createRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> create().execute(OP,
                new CreatePolicyInput("TERMS", "Terms", "<p>b</p>"), USER))
                .isInstanceOf(ForbiddenException.class);
    }


    @Test
    void updateRewritesTheTextAndKeepsCreatedAt() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        update().execute(OP, POLICY_ID, new UpdatePolicyInput("Cancellation policy",
                "<p>New</p>"), USER);

        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(POLICY_ID);
        assertThat(saved.getValue().createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(saved.getValue().updatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(saved.getValue().body().value()).isEqualTo("<p>New</p>");
    }

    @Test
    void updatingAPolicyOfAnotherOperatorIs404() {
        // The lookup is tenant-scoped, so a valid id from another operator finds
        // nothing — byte-identical to an id that does not exist.
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> update().execute(OP, POLICY_ID,
                new UpdatePolicyInput("t", "<p>b</p>"), USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(policyRepository, never()).save(any());
    }

    @Test
    void updateRejectsABlankBody() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));
        assertThatThrownBy(() -> update().execute(OP, POLICY_ID,
                new UpdatePolicyInput("Terms", "   "), USER))
                .isInstanceOf(InvalidFieldException.class);
    }


    @Test
    void getReturnsThePolicy() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        var view = new GetPolicyUseCase(policyRepository, membershipCheck)
                .execute(OP, POLICY_ID, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.id()).isEqualTo(POLICY_ID);
        assertThat(view.type()).isEqualTo("CANCELLATION");
        assertThat(view.body()).isEqualTo("<p>Old</p>");
    }

    @Test
    void gettingAPolicyOfAnotherOperatorIs404() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetPolicyUseCase(policyRepository, membershipCheck)
                .execute(OP, POLICY_ID, USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsOnlyWrittenPoliciesThroughTheCursorFramework() {
        ListQuery query = new ListQuery(OP, FilterSpec.empty(),
                new SortSpec("type", SortDirection.ASC), null);
        when(policyRepository.list(query))
                .thenReturn(new CursorPage<>(List.of(existing()), null));

        CursorPage<?> page = new ListPoliciesUseCase(policyRepository, membershipCheck)
                .execute(query, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(page.data()).hasSize(1);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theListSchemaIsTenantScopedSoOneOperatorCannotSeeAnother() {
        // The tenant predicate is the framework's, not the use case's — so this is
        // the only place it can be asserted.
        assertThat(ListPoliciesUseCase.SCHEMA.tenantScoped()).isTrue();
        assertThat(ListPoliciesUseCase.SCHEMA.defaultSort().field()).isEqualTo("type");
    }


    @Test
    void deleteRemovesThePolicyAndAudits() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        delete().execute(OP, POLICY_ID, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(policyRepository).deleteById(POLICY_ID);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.policy_deleted");
        assertThat(audit.getValue().details()).containsEntry("type", "CANCELLATION");
    }

    @Test
    void deletingAnUnknownOrForeignPolicyIsIdempotentAndRecordsNothing() {
        // Tenant-scoped probe, so this covers both "no such id" and "another
        // operator's id" — neither can delete anything.
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.empty());

        delete().execute(OP, POLICY_ID, USER);

        verify(policyRepository, never()).deleteById(any());
        verify(auditTrailPort, never()).append(any());
    }


    @Test
    void translationUpsertStoresTheOverlayAndAudits() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        upsertTranslation().execute(OP, POLICY_ID, "es",
                new UpsertPolicyTranslationInput("Política de cancelación", "<p>Gratis</p>"), USER);

        ArgumentCaptor<PolicyTranslation> saved = ArgumentCaptor.forClass(PolicyTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().locale().value()).isEqualTo("es");
        assertThat(saved.getValue().title().value()).isEqualTo("Política de cancelación");

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.policy_translation_updated");
        assertThat(audit.getValue().details()).containsEntry("locale", "es");
    }

    @Test
    void aBlankTranslatedFieldIsStoredAsUntranslated() {
        // Null, not "": the storefront reads null as "fall back to canonical", and
        // an empty string would render a policy with no heading.
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        upsertTranslation().execute(OP, POLICY_ID, "es",
                new UpsertPolicyTranslationInput("Política", "  "), USER);

        ArgumentCaptor<PolicyTranslation> saved = ArgumentCaptor.forClass(PolicyTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().title().value()).isEqualTo("Política");
        assertThat(saved.getValue().body()).isNull();
    }

    @Test
    void blankingBothFieldsDeletesTheOverlayInsteadOfStoringAnEmptyOne() {
        // An overlay with nothing in it falls back for every field, so it is
        // indistinguishable from no overlay — except that it shows up in the
        // translations list as a locale someone has worked on.
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));
        when(translationRepository.delete(OP, PolicyType.CANCELLATION, "es")).thenReturn(true);

        upsertTranslation().execute(OP, POLICY_ID, "es",
                new UpsertPolicyTranslationInput("  ", ""), USER);

        verify(translationRepository).delete(OP, PolicyType.CANCELLATION, "es");
        verify(translationRepository, never()).upsert(any());

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action())
                .isEqualTo("tour_operator.policy_translation_deleted");
        assertThat(audit.getValue().details()).containsEntry("locale", "es");
    }

    @Test
    void savingAnAlreadyBlankPolicyTranslationWritesNothingAndAuditsNothing() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));
        when(translationRepository.delete(OP, PolicyType.CANCELLATION, "es")).thenReturn(false);

        upsertTranslation().execute(OP, POLICY_ID, "es",
                new UpsertPolicyTranslationInput(null, null), USER);

        verify(translationRepository, never()).upsert(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void anEmptyPolicyTranslationKnowsItIsEmpty() {
        // empty() and isEmpty() are two halves of one idea; if a translatable
        // field is added to the record and not to isEmpty(), this fails.
        assertThat(PolicyTranslation.empty(OP, PolicyType.CANCELLATION, new LocaleCode("es"))
                .isEmpty()).isTrue();
        assertThat(new PolicyTranslation(OP, PolicyType.CANCELLATION, new LocaleCode("es"),
                new PolicyTitle("Política"), null).isEmpty()).isFalse();
    }

    @Test
    void translatingAnUnknownOrForeignPolicyIs404() {
        // Otherwise the row is unreachable: the storefront looks the page up by its
        // canonical policy, so an overlay with no owner could never render.
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> upsertTranslation().execute(OP, POLICY_ID, "es",
                new UpsertPolicyTranslationInput("T", null), USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void translationRejectsALocaleTheOperatorDoesNotSupport() {
        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        assertThatThrownBy(() -> upsertTranslation().execute(OP, POLICY_ID, "fr",
                new UpsertPolicyTranslationInput("T", null), USER))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void deletingAnAbsentOverlayIsIdempotentAndRecordsNothing() {
        when(translationRepository.find(OP, PolicyType.CANCELLATION, "es"))
                .thenReturn(Optional.empty());

        when(policyRepository.findByIdAndTourOperatorId(POLICY_ID, OP))
                .thenReturn(Optional.of(existing()));

        new DeletePolicyTranslationUseCase(policyRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort)
                .execute(OP, POLICY_ID, "es", USER);

        verify(translationRepository, never()).delete(any(), any(), any());
        verify(auditTrailPort, never()).append(any());
    }
}
