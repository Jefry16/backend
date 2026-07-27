package com.vointika.page.application.usecase;

import com.vointika.page.application.dto.input.UpsertPageTranslationInput;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.SlugGenerator;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageTranslationUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PAGE = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001");

    private PageRepository pageRepository;
    private PageTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private SlugGenerator slugGenerator;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageRepository.class);
        translationRepository = mock(PageTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        slugGenerator = mock(SlugGenerator.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(pageRepository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(
                new Page(PAGE, OP, new PageTitle("About us"), new Slug("about-us"),
                        new PageBody("<p>Hello</p>"), null, null, USER)));
        when(operatorLocalesQuery.findSupportedLocales(OP)).thenReturn(Set.of("en", "es"));
    }

    private UpsertPageTranslationUseCase upsert() {
        return new UpsertPageTranslationUseCase(pageRepository, translationRepository,
                operatorLocalesQuery, slugGenerator, membershipCheck, transactionRunner, auditTrailPort);
    }

    private UpsertPageTranslationInput input(String title, String slug) {
        return new UpsertPageTranslationInput(USER, OP, PAGE, "es",
                title, "<p>Hola</p>", null, null, slug);
    }

    @Test
    void upsertStoresOverlayWithExplicitSlugAndAudits() {
        when(translationRepository.existsBySlug(OP, new LocaleCode("es"), "sobre-nosotros", PAGE))
                .thenReturn(false);

        upsert().execute(input("Sobre nosotros", "sobre-nosotros"));

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<PageTranslation> captor = ArgumentCaptor.forClass(PageTranslation.class);
        verify(translationRepository).upsert(captor.capture());
        assertThat(captor.getValue().slug().value()).isEqualTo("sobre-nosotros");
        assertThat(captor.getValue().title().value()).isEqualTo("Sobre nosotros");
        verify(auditTrailPort).append(any());
    }

    @Test
    void upsertExplicitSlugCollisionIs409() {
        when(translationRepository.existsBySlug(OP, new LocaleCode("es"), "sobre-nosotros", PAGE))
                .thenReturn(true);

        assertThatThrownBy(() -> upsert().execute(input("Sobre nosotros", "sobre-nosotros")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void upsertDerivesSlugFromTranslatedTitleWhenAbsent() {
        when(slugGenerator.generateUnique(eq("Sobre nosotros"), any())).thenReturn(new Slug("sobre-nosotros"));

        upsert().execute(input("Sobre nosotros", null));

        ArgumentCaptor<PageTranslation> captor = ArgumentCaptor.forClass(PageTranslation.class);
        verify(translationRepository).upsert(captor.capture());
        assertThat(captor.getValue().slug().value()).isEqualTo("sobre-nosotros");
    }

    @Test
    void upsertWithoutTitleOrSlugStoresNoLocalizedHandle() {
        upsert().execute(input(null, null));

        ArgumentCaptor<PageTranslation> captor = ArgumentCaptor.forClass(PageTranslation.class);
        verify(translationRepository).upsert(captor.capture());
        assertThat(captor.getValue().slug()).isNull();
        verify(slugGenerator, never()).generateUnique(anyString(), any());
    }

    @Test
    void upsertUnsupportedLocaleIs422() {
        assertThatThrownBy(() -> upsert().execute(new UpsertPageTranslationInput(
                USER, OP, PAGE, "fr", "Titre", "<p>Bonjour</p>", null, null, null)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void upsertMissingPageIs404() {
        when(pageRepository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upsert().execute(input("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- get / delete ----

    @Test
    void getUntranslatedReturnsEmptyOverlay() {
        when(translationRepository.find(PAGE, new LocaleCode("es"))).thenReturn(Optional.empty());

        PageTranslation t = new GetPageTranslationUseCase(
                pageRepository, translationRepository, membershipCheck)
                .execute(OP, PAGE, "es", USER);

        assertThat(t.title()).isNull();
        assertThat(t.locale().value()).isEqualTo("es");
        verify(membershipCheck).ensureMember(USER, OP);
    }

    @Test
    void deleteRemovesExistingOverlayAndAudits() {
        when(translationRepository.find(PAGE, new LocaleCode("es")))
                .thenReturn(Optional.of(PageTranslation.empty(PAGE, OP, new LocaleCode("es"))));

        delete().execute(OP, PAGE, "es", USER);

        verify(translationRepository).delete(PAGE, new LocaleCode("es"));
        verify(auditTrailPort).append(any());
    }

    @Test
    void deleteOfAbsentOverlayIsIdempotentAndRecordsNothing() {
        when(translationRepository.find(PAGE, new LocaleCode("es"))).thenReturn(Optional.empty());

        delete().execute(OP, PAGE, "es", USER);

        verify(translationRepository, never()).delete(any(), any());
        verify(auditTrailPort, never()).append(any());
    }

    private DeletePageTranslationUseCase delete() {
        return new DeletePageTranslationUseCase(pageRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }
}
