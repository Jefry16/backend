package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.input.UpsertPolicyInput;
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

    private TourOperatorRepository operatorRepository;
    private TourOperatorPolicyRepository policyRepository;
    private TourOperatorPolicyTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        policyRepository = mock(TourOperatorPolicyRepository.class);
        translationRepository = mock(TourOperatorPolicyTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(operatorLocalesQuery.findSupportedLocales(OP)).thenReturn(Set.of("en", "es"));
    }

    private TourOperator operator() {
        return new TourOperator(OP, new TourOperatorName("Acme"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Somewhere 1"),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en"), LocaleCode.of("es")));
    }

    private Policy existing() {
        return new Policy(OP, PolicyType.CANCELLATION,
                new PolicyTitle("Cancellation policy"), new PolicyBody("<p>Old</p>"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private UpsertPolicyUseCase upsert() {
        return new UpsertPolicyUseCase(operatorRepository, policyRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    private DeletePolicyUseCase delete() {
        return new DeletePolicyUseCase(operatorRepository, policyRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    private UpsertPolicyTranslationUseCase upsertTranslation() {
        return new UpsertPolicyTranslationUseCase(policyRepository, translationRepository,
                operatorLocalesQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    // ---- upsert ----

    @Test
    void upsertWritesANewPolicyAndAudits() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.empty());

        upsert().execute(OP, "CANCELLATION",
                new UpsertPolicyInput("Cancellation policy", "<p>Free up to 48h before.</p>"), USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).upsert(saved.capture());
        assertThat(saved.getValue().type()).isEqualTo(PolicyType.CANCELLATION);
        assertThat(saved.getValue().title().value()).isEqualTo("Cancellation policy");
        assertThat(saved.getValue().body().value()).isEqualTo("<p>Free up to 48h before.</p>");

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.policy_updated");
        assertThat(audit.getValue().details()).containsEntry("type", "CANCELLATION");
    }

    @Test
    void theAuditEntryNeverCarriesTheBody() {
        // The trail is not a revision store, and a policy body is up to 256 KiB of
        // HTML. Same rule the contact context follows when it audits by summary.
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.PRIVACY))
                .thenReturn(Optional.empty());

        upsert().execute(OP, "PRIVACY",
                new UpsertPolicyInput("Privacy", "<p>SECRET-MARKER</p>"), USER);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().details().toString()).doesNotContain("SECRET-MARKER");
    }

    @Test
    void rewritingKeepsTheOriginalCreatedAt() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        upsert().execute(OP, "CANCELLATION",
                new UpsertPolicyInput("Cancellation policy", "<p>New</p>"), USER);

        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).upsert(saved.capture());
        assertThat(saved.getValue().createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(saved.getValue().updatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(saved.getValue().body().value()).isEqualTo("<p>New</p>");
    }

    @Test
    void upsertRejectsAnUnknownType() {
        // 404 rather than valueOf's IllegalArgumentException, which is a 500.
        assertThatThrownBy(() -> upsert().execute(OP, "REFUNDS",
                new UpsertPolicyInput("t", "<p>b</p>"), USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(policyRepository, never()).upsert(any());
    }

    @Test
    void upsertRejectsABlankBody() {
        assertThatThrownBy(() -> upsert().execute(OP, "TERMS",
                new UpsertPolicyInput("Terms", "   "), USER))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void upsertRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> upsert().execute(OP, "TERMS",
                new UpsertPolicyInput("Terms", "<p>b</p>"), USER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void upsertOfAMissingOperatorIs404() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upsert().execute(OP, "TERMS",
                new UpsertPolicyInput("Terms", "<p>b</p>"), USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- get / list ----

    @Test
    void getReturnsThePolicy() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        var view = new GetPolicyUseCase(policyRepository, membershipCheck)
                .execute(OP, "CANCELLATION", USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.type()).isEqualTo("CANCELLATION");
        assertThat(view.body()).isEqualTo("<p>Old</p>");
    }

    @Test
    void getAnUnwrittenPolicyIs404NotAnEmptyDocument() {
        // Unlike a translation overlay, absence is meaningful here: it is what the
        // storefront serves as a 404, so the admin API says the same thing.
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.LEGAL_NOTICE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetPolicyUseCase(policyRepository, membershipCheck)
                .execute(OP, "LEGAL_NOTICE", USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsOnlyWrittenPolicies() {
        when(policyRepository.findAllByTourOperatorId(OP)).thenReturn(List.of(existing()));

        var views = new ListPoliciesUseCase(policyRepository, membershipCheck).execute(OP, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(views).singleElement()
                .satisfies(v -> assertThat(v.type()).isEqualTo("CANCELLATION"));
    }

    // ---- delete ----

    @Test
    void deleteRemovesThePolicyAndAudits() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        delete().execute(OP, "CANCELLATION", USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(policyRepository).deleteByTourOperatorIdAndType(OP, PolicyType.CANCELLATION);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.policy_deleted");
    }

    @Test
    void deletingAnUnwrittenPolicyIsIdempotentAndRecordsNothing() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.TERMS))
                .thenReturn(Optional.empty());

        delete().execute(OP, "TERMS", USER);

        verify(policyRepository, never()).deleteByTourOperatorIdAndType(any(), any());
        verify(auditTrailPort, never()).append(any());
    }

    // ---- translations ----

    @Test
    void translationUpsertStoresTheOverlayAndAudits() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        upsertTranslation().execute(OP, "CANCELLATION", "es",
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
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        upsertTranslation().execute(OP, "CANCELLATION", "es",
                new UpsertPolicyTranslationInput("Política", "  "), USER);

        ArgumentCaptor<PolicyTranslation> saved = ArgumentCaptor.forClass(PolicyTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().title().value()).isEqualTo("Política");
        assertThat(saved.getValue().body()).isNull();
    }

    @Test
    void translatingAnUnwrittenPolicyIs404() {
        // Otherwise the row is unreachable: the storefront looks the page up by its
        // canonical policy, so an overlay with no owner could never render.
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.TERMS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> upsertTranslation().execute(OP, "TERMS", "es",
                new UpsertPolicyTranslationInput("T", null), USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void translationRejectsALocaleTheOperatorDoesNotSupport() {
        when(policyRepository.findByTourOperatorIdAndType(OP, PolicyType.CANCELLATION))
                .thenReturn(Optional.of(existing()));

        assertThatThrownBy(() -> upsertTranslation().execute(OP, "CANCELLATION", "fr",
                new UpsertPolicyTranslationInput("T", null), USER))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void deletingAnAbsentOverlayIsIdempotentAndRecordsNothing() {
        when(translationRepository.find(OP, PolicyType.CANCELLATION, "es"))
                .thenReturn(Optional.empty());

        new DeletePolicyTranslationUseCase(translationRepository, membershipCheck,
                transactionRunner, auditTrailPort).execute(OP, "CANCELLATION", "es", USER);

        verify(translationRepository, never()).delete(any(), any(), any());
        verify(auditTrailPort, never()).append(any());
    }
}
