package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.application.dto.input.UpsertOperatorTranslationInput;
import com.vointika.touroperator.application.dto.output.OperatorTranslationView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
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

/**
 * The operator's own translation overlay — the four use cases mirroring
 * {@code Upsert/Get/List/DeleteExperienceTranslationUseCase}.
 */
class OperatorTranslationUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private TourOperatorRepository operatorRepository;
    private TourOperatorTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        translationRepository = mock(TourOperatorTranslationRepository.class);
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
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Somewhere 1", null, "Palma", null, null, UUID.randomUUID()),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en"), LocaleCode.of("es")));
    }

    private UpsertOperatorTranslationUseCase upsert() {
        return new UpsertOperatorTranslationUseCase(operatorRepository, translationRepository,
                operatorLocalesQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    private DeleteOperatorTranslationUseCase delete() {
        return new DeleteOperatorTranslationUseCase(operatorRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    // ---- upsert ----

    @Test
    void upsertStoresTheOverlayAndAudits() {
        upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput("Título", "Descripción", "Copia", "Lema", "Descripción corta"), USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<TourOperatorTranslation> saved =
                ArgumentCaptor.forClass(TourOperatorTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().locale().value()).isEqualTo("es");
        assertThat(saved.getValue().seoTitle().value()).isEqualTo("Título");
        assertThat(saved.getValue().passwordMessage()).isEqualTo("Copia");
        assertThat(saved.getValue().slogan().value()).isEqualTo("Lema");
        assertThat(saved.getValue().shortDescription().value()).isEqualTo("Descripción corta");

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.translation_updated");
        assertThat(audit.getValue().details()).containsEntry("locale", "es");
    }

    @Test
    void upsertTreatsABlankFieldAsUntranslated() {
        // Null, not "" — the storefront reads null as "fall back to canonical",
        // and an empty string would be a title of zero characters. One field is
        // kept so the row still has content; blanking ALL of them is a delete,
        // which is the test below.
        upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput("Título", "", null, "   ", ""), USER);

        ArgumentCaptor<TourOperatorTranslation> saved =
                ArgumentCaptor.forClass(TourOperatorTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().seoTitle().value()).isEqualTo("Título");
        assertThat(saved.getValue().seoDescription()).isNull();
        assertThat(saved.getValue().passwordMessage()).isNull();
        assertThat(saved.getValue().slogan()).isNull();
        assertThat(saved.getValue().shortDescription()).isNull();
    }

    @Test
    void blankingEveryFieldDeletesTheRowInsteadOfStoringAnEmptyOne() {
        // An overlay with nothing in it falls back for every field, so it is
        // indistinguishable from no overlay — except that it shows up in the
        // translations list as a locale someone has worked on.
        when(translationRepository.deleteByTourOperatorIdAndLocale(OP, "es")).thenReturn(true);

        upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput("  ", "", null, "   ", ""), USER);

        verify(translationRepository).deleteByTourOperatorIdAndLocale(OP, "es");
        verify(translationRepository, never()).upsert(any());

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.translation_deleted");
        assertThat(audit.getValue().details()).containsEntry("locale", "es");
    }

    @Test
    void savingAnAlreadyBlankFormWritesNothingAndAuditsNothing() {
        // Nothing to remove: the delete reports no row, so there is no event.
        when(translationRepository.deleteByTourOperatorIdAndLocale(OP, "es")).thenReturn(false);

        upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput(null, null, null, null, null), USER);

        verify(translationRepository, never()).upsert(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void anEmptyTranslationKnowsItIsEmpty() {
        // empty() and isEmpty() are two halves of one idea; if a translatable
        // field is added to the record and not to isEmpty(), this fails.
        assertThat(TourOperatorTranslation.empty(OP, LocaleCode.of("es")).isEmpty()).isTrue();
        assertThat(new TourOperatorTranslation(OP, LocaleCode.of("es"),
                new OperatorSeoTitle("t"), null, null, null, null).isEmpty()).isFalse();
    }

    @Test
    void upsertRejectsALocaleTheOperatorDoesNotSupport() {
        assertThatThrownBy(() -> upsert().execute(OP, "fr",
                new UpsertOperatorTranslationInput("t", null, null, null, null), USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void upsertRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput("t", null, null, null, null), USER))
                .isInstanceOf(ForbiddenException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void upsertOfAMissingOperatorIs404() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upsert().execute(OP, "es",
                new UpsertOperatorTranslationInput("t", null, null, null, null), USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- read ----

    @Test
    void getReturnsAnEmptyOverlayForAnUntranslatedLocale() {
        // The admin editor always needs a form, even for a locale with no row.
        when(translationRepository.findByTourOperatorIdAndLocale(OP, "es")).thenReturn(Optional.empty());

        OperatorTranslationView view = new GetOperatorTranslationUseCase(
                operatorRepository, translationRepository, membershipCheck).execute(OP, "es", USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.locale()).isEqualTo("es");
        assertThat(view.seoTitle()).isNull();
        assertThat(view.seoDescription()).isNull();
        assertThat(view.passwordMessage()).isNull();
    }

    @Test
    void listReturnsOneRowPerTranslatedLocale() {
        when(translationRepository.findAllByTourOperatorId(OP)).thenReturn(List.of(
                new TourOperatorTranslation(OP, LocaleCode.of("es"),
                        new OperatorSeoTitle("Título"), null, null, null, null)));

        List<OperatorTranslationView> views = new ListOperatorTranslationsUseCase(
                operatorRepository, translationRepository, membershipCheck).execute(OP, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(views).singleElement()
                .satisfies(v -> assertThat(v.seoTitle()).isEqualTo("Título"));
    }

    // ---- delete ----

    @Test
    void deleteRemovesTheOverlayAndAudits() {
        when(translationRepository.findByTourOperatorIdAndLocale(OP, "es")).thenReturn(
                Optional.of(TourOperatorTranslation.empty(OP, LocaleCode.of("es"))));

        delete().execute(OP, "es", USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(translationRepository).deleteByTourOperatorIdAndLocale(OP, "es");
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.translation_deleted");
    }

    @Test
    void deletingAnAbsentOverlayIsIdempotentAndRecordsNothing() {
        when(translationRepository.findByTourOperatorIdAndLocale(OP, "es")).thenReturn(Optional.empty());

        delete().execute(OP, "es", USER);

        verify(translationRepository, never()).deleteByTourOperatorIdAndLocale(any(), any());
        verify(auditTrailPort, never()).append(any());
    }
}
