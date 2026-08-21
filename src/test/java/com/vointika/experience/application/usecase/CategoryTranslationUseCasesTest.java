package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.shared.valueobject.LocaleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryTranslationUseCasesTest {

    private static final UUID OPERATOR = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c41");
    private static final UUID CATEGORY = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c42");
    private static final UUID CALLER = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c43");
    private static final String ES = "es";

    private CategoryRepository categoryRepository;
    private CategoryTranslationRepository translationRepository;
    private OperatorLocaleCheck operatorLocaleCheck;
    private TourOperatorMembershipCheck membershipCheck;
    private AuditTrailPort auditTrailPort;
    private TransactionRunner tx;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        translationRepository = mock(CategoryTranslationRepository.class);
        operatorLocaleCheck = mock(OperatorLocaleCheck.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        auditTrailPort = mock(AuditTrailPort.class);
        // The existence check is the stub; requireExists runs for real (PATTERNS §9).
        when(categoryRepository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(true);
        doCallRealMethod().when(categoryRepository).requireExists(any(), any());
        tx = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
    }

    private UpsertCategoryTranslationUseCase upsert() {
        return new UpsertCategoryTranslationUseCase(categoryRepository, translationRepository,
                operatorLocaleCheck, membershipCheck, tx, auditTrailPort);
    }

    private CategoryTranslation stored(String name) {
        return new CategoryTranslation(CATEGORY, OPERATOR, new LocaleCode(ES),
                name == null ? null : new CategoryName(name));
    }

    @Test
    void upsertWritesTheOverlayAndAuditsTheLocale() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES)).thenReturn(Optional.empty());

        upsert().execute(OPERATOR, CATEGORY, ES, "Excursiones en barco", CALLER);

        verify(membershipCheck).ensureAdmin(CALLER, OPERATOR);
        verify(operatorLocaleCheck).require(OPERATOR, ES);

        ArgumentCaptor<CategoryTranslation> saved = ArgumentCaptor.forClass(CategoryTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertThat(saved.getValue().name().value()).isEqualTo("Excursiones en barco");

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("category.translation_updated");
        assertThat(entry.getValue().details()).containsEntry("locale", ES);
    }

    /**
     * Name is the only translatable column, so an emptied one leaves an overlay
     * that falls back for everything — indistinguishable from no row except in
     * the translations list. It is deleted rather than stored (PATTERNS §4e).
     */
    @Test
    void blankingTheOnlyFieldDeletesTheRow() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES))
                .thenReturn(Optional.of(stored("Excursiones en barco")));

        upsert().execute(OPERATOR, CATEGORY, ES, "   ", CALLER);

        verify(translationRepository).deleteByCategoryIdAndLocale(CATEGORY, ES);
        verify(translationRepository, never()).upsert(any());

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("category.translation_deleted");
    }

    @Test
    void blankingAnAlreadyAbsentTranslationWritesNothing() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES)).thenReturn(Optional.empty());

        upsert().execute(OPERATOR, CATEGORY, ES, null, CALLER);

        verify(translationRepository, never()).upsert(any());
        verify(translationRepository, never()).deleteByCategoryIdAndLocale(any(), any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void reSavingTheSameValueWritesNothing() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES))
                .thenReturn(Optional.of(stored("Excursiones en barco")));

        upsert().execute(OPERATOR, CATEGORY, ES, "Excursiones en barco", CALLER);

        verify(translationRepository, never()).upsert(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void aLocaleTheOperatorDoesNotSupportIs422() {
        doThrow(new InvalidFieldException("unsupported"))
                .when(operatorLocaleCheck).require(OPERATOR, ES);

        assertThatThrownBy(() -> upsert().execute(OPERATOR, CATEGORY, ES, "Excursiones", CALLER))
                .isInstanceOf(InvalidFieldException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void anUnknownCategoryIs404BeforeTheLocaleIsChecked() {
        when(categoryRepository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(false);

        assertThatThrownBy(() -> upsert().execute(OPERATOR, CATEGORY, ES, "Excursiones", CALLER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CategoryRepository.NOT_FOUND);
        verify(operatorLocaleCheck, never()).require(any(), any());
    }

    @Test
    void getReturnsAnEmptyOverlayForAnUntranslatedLocale() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES)).thenReturn(Optional.empty());

        CategoryTranslation view = new GetCategoryTranslationUseCase(
                categoryRepository, translationRepository, membershipCheck)
                .execute(OPERATOR, CATEGORY, ES, CALLER);

        assertThat(view.name()).isNull();
        assertThat(view.locale().value()).isEqualTo(ES);
        verify(membershipCheck).ensureMember(CALLER, OPERATOR);
    }

    @Test
    void listReturnsOneRowPerTranslatedLocale() {
        when(translationRepository.findAllByCategoryId(CATEGORY))
                .thenReturn(List.of(stored("Excursiones en barco")));

        List<CategoryTranslation> rows = new ListCategoryTranslationsUseCase(
                categoryRepository, translationRepository, membershipCheck)
                .execute(OPERATOR, CATEGORY, CALLER);

        assertThat(rows).hasSize(1);
        verify(membershipCheck).ensureMember(CALLER, OPERATOR);
    }

    @Test
    void deleteRemovesTheOverlayAndAuditsIt() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES))
                .thenReturn(Optional.of(stored("Excursiones en barco")));

        new DeleteCategoryTranslationUseCase(categoryRepository, translationRepository,
                membershipCheck, tx, auditTrailPort).execute(OPERATOR, CATEGORY, ES, CALLER);

        verify(membershipCheck).ensureAdmin(CALLER, OPERATOR);
        verify(translationRepository).deleteByCategoryIdAndLocale(CATEGORY, ES);
        verify(auditTrailPort).append(any());
    }

    /** Idempotent: a repeat removes nothing, so it records nothing either. */
    @Test
    void deletingAnAbsentOverlayIsANoOp() {
        when(translationRepository.findByCategoryIdAndLocale(CATEGORY, ES)).thenReturn(Optional.empty());

        new DeleteCategoryTranslationUseCase(categoryRepository, translationRepository,
                membershipCheck, tx, auditTrailPort).execute(OPERATOR, CATEGORY, ES, CALLER);

        verify(translationRepository, never()).deleteByCategoryIdAndLocale(any(), any());
        verify(auditTrailPort, never()).append(any());
    }
}
