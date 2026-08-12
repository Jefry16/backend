package com.vointika.page.infrastructure.query;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Addressing a page by handle, which is the read that decides what
 * {@code /pages/&#123;handle&#125;} answers.
 *
 * <p>The case worth the test is the last one: a page a locale renames has
 * <b>one</b> address in that locale, and its canonical handle is not a second
 * one. Same rule that makes {@code /{primary}} a 404 when the primary already
 * lives at {@code /}.
 */
class StorefrontPageAddressTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID ABOUT = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e11");

    private PageJpaRepository pageRepository;
    private PageTranslationJpaRepository translationRepository;
    private StorefrontPageQueryImpl query;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageJpaRepository.class);
        translationRepository = mock(PageTranslationJpaRepository.class);
        query = new StorefrontPageQueryImpl(pageRepository, translationRepository);
    }

    private static PageJpaEntity page() {
        PageJpaEntity page = mock(PageJpaEntity.class);
        when(page.getId()).thenReturn(ABOUT);
        when(page.getTourOperatorId()).thenReturn(OPERATOR);
        when(page.getHandle()).thenReturn("about-us");
        when(page.getTitle()).thenReturn("About us");
        when(page.getBody()).thenReturn("<p>Since 2011.</p>");
        when(page.isPublished()).thenReturn(true);
        return page;
    }

    /**
     * <b>Call this before the {@code when(...)} it feeds, never inside it.</b> It
     * stubs a mock of its own, and Mockito reads a nested {@code when()} as an
     * unfinished one — an {@code UnfinishedStubbing} error pointing at this line
     * rather than at the caller.
     */
    private static PageTranslationJpaEntity translation(String handle, String title) {
        PageTranslationJpaEntity translation = mock(PageTranslationJpaEntity.class);
        when(translation.getPageId()).thenReturn(ABOUT);
        when(translation.getHandle()).thenReturn(handle);
        when(translation.getTitle()).thenReturn(title);
        return translation;
    }

    @Test
    void theCanonicalHandleAddressesAPageWithNoTranslation() {
        PageJpaEntity about = page();
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "about-us"))
                .thenReturn(Optional.empty());
        when(pageRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "about-us"))
                .thenReturn(Optional.of(about));
        when(translationRepository.findByPageIdAndLocale(ABOUT, "es")).thenReturn(Optional.empty());

        assertThat(query.findByHandle(OPERATOR, "about-us", "es")).hasValueSatisfying(view -> {
            assertThat(view.title()).isEqualTo("About us");
            assertThat(view.body()).isEqualTo("<p>Since 2011.</p>");
        });
    }

    @Test
    void aTranslatedHandleAddressesItAndOverlaysTheContent() {
        PageJpaEntity about = page();
        PageTranslationJpaEntity spanish = translation("sobre-nosotros", "Sobre nosotros");
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "sobre-nosotros"))
                .thenReturn(Optional.of(spanish));
        when(pageRepository.findById(ABOUT)).thenReturn(Optional.of(about));

        assertThat(query.findByHandle(OPERATOR, "sobre-nosotros", "es")).hasValueSatisfying(view -> {
            assertThat(view.handle()).isEqualTo("sobre-nosotros");
            assertThat(view.title()).isEqualTo("Sobre nosotros");
            // The body has no Spanish row, so it falls back rather than blanking.
            assertThat(view.body()).isEqualTo("<p>Since 2011.</p>");
        });
    }

    /**
     * <b>The duplicate-content rule.</b> This page is {@code /es/pages/sobre-nosotros};
     * serving it at {@code /es/pages/about-us} too would be two URLs for one page.
     */
    @Test
    void theCanonicalHandleIsNotASecondAddressInALocaleThatRenamesIt() {
        PageJpaEntity about = page();
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "about-us"))
                .thenReturn(Optional.empty());
        when(pageRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "about-us"))
                .thenReturn(Optional.of(about));
        PageTranslationJpaEntity spanish = translation("sobre-nosotros", "Sobre nosotros");
        when(translationRepository.findByPageIdAndLocale(ABOUT, "es")).thenReturn(Optional.of(spanish));

        assertThat(query.findByHandle(OPERATOR, "about-us", "es")).isEmpty();
    }

    /** A locale row that translates the title but not the handle keeps the canonical address. */
    @Test
    void aTranslationWithoutAHandleKeepsTheCanonicalAddress() {
        PageJpaEntity about = page();
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "about-us"))
                .thenReturn(Optional.empty());
        when(pageRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "about-us"))
                .thenReturn(Optional.of(about));
        PageTranslationJpaEntity spanish = translation(null, "Sobre nosotros");
        when(translationRepository.findByPageIdAndLocale(ABOUT, "es")).thenReturn(Optional.of(spanish));

        assertThat(query.findByHandle(OPERATOR, "about-us", "es")).hasValueSatisfying(view -> {
            assertThat(view.handle()).isEqualTo("about-us");
            assertThat(view.title()).isEqualTo("Sobre nosotros");
        });
    }

    /** A draft has no address at all — the filter is in the query name. */
    @Test
    void aDraftPageHasNoAddress() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "about-us"))
                .thenReturn(Optional.empty());
        when(pageRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "about-us"))
                .thenReturn(Optional.empty());

        assertThat(query.findByHandle(OPERATOR, "about-us", "es")).isEmpty();
    }

    /**
     * The translated route reaches the page by id, so the tenant and publish
     * checks have to be made again there — a handle is unique per operator, not
     * globally.
     */
    @Test
    void aTranslatedHandleStillChecksTenantAndPublishState() {
        PageJpaEntity draft = mock(PageJpaEntity.class);
        when(draft.isPublished()).thenReturn(false);
        PageTranslationJpaEntity spanish = translation("sobre-nosotros", "Sobre nosotros");
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "sobre-nosotros"))
                .thenReturn(Optional.of(spanish));
        when(pageRepository.findById(ABOUT)).thenReturn(Optional.of(draft));

        assertThat(query.findByHandle(OPERATOR, "sobre-nosotros", "es")).isEmpty();
    }
}
