package com.vointika.page.infrastructure.query;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import com.vointika.shared.port.StorefrontPageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontPageQueryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID PAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");

    private PageJpaRepository pageRepository;
    private PageTranslationJpaRepository translationRepository;
    private StorefrontPageQueryImpl query;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageJpaRepository.class);
        translationRepository = mock(PageTranslationJpaRepository.class);
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(translationRepository.findByPageId(any())).thenReturn(List.of());
        query = new StorefrontPageQueryImpl(pageRepository, translationRepository);
    }

    private PageJpaEntity page(PageStatus status) {
        return new PageJpaEntity(
                PAGE, OP, "About us", "about-us", "<p>We run boats.</p>",
                "About | Acme", "Meet the crew", status, null,
                UUID.randomUUID(), Instant.now(), Instant.now());
    }

    private PageTranslationJpaEntity translation(String slug, String title) {
        return new PageTranslationJpaEntity(PAGE, "es", OP, slug, title, null, null, null);
    }

    @Test
    void finds_a_published_page_by_its_canonical_handle() {
        when(pageRepository.findByTourOperatorIdAndHandleAndStatus(OP, "about-us", PageStatus.PUBLISHED))
                .thenReturn(Optional.of(page(PageStatus.PUBLISHED)));

        StorefrontPageView view = query.findPublishedByHandle(OP, "about-us", "en").orElseThrow();

        assertThat(view.title()).isEqualTo("About us");
        assertThat(view.body()).isEqualTo("<p>We run boats.</p>");
        assertThat(view.canonicalHandle()).isEqualTo("about-us");
    }

    @Test
    void a_draft_page_is_not_reachable() {
        when(pageRepository.findByTourOperatorIdAndHandleAndStatus(OP, "secret", PageStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThat(query.findPublishedByHandle(OP, "secret", "en")).isEmpty();
    }

    @Test
    void finds_a_page_by_its_localized_handle_and_overlays_the_translation() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(OP, "es", "sobre-nosotros"))
                .thenReturn(Optional.of(translation("sobre-nosotros", "Sobre nosotros")));
        when(pageRepository.findById(PAGE)).thenReturn(Optional.of(page(PageStatus.PUBLISHED)));
        when(translationRepository.findByPageId(PAGE))
                .thenReturn(List.of(translation("sobre-nosotros", "Sobre nosotros")));

        StorefrontPageView view =
                query.findPublishedByHandle(OP, "sobre-nosotros", "es").orElseThrow();

        assertThat(view.handle()).isEqualTo("sobre-nosotros");
        assertThat(view.title()).isEqualTo("Sobre nosotros");
        // Untranslated body falls back rather than going blank.
        assertThat(view.body()).isEqualTo("<p>We run boats.</p>");
    }

    @Test
    void a_localized_handle_pointing_at_a_draft_is_refused() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(OP, "es", "sobre-nosotros"))
                .thenReturn(Optional.of(translation("sobre-nosotros", "Sobre nosotros")));
        when(pageRepository.findById(PAGE)).thenReturn(Optional.of(page(PageStatus.DRAFT)));
        when(pageRepository.findByTourOperatorIdAndHandleAndStatus(OP, "sobre-nosotros", PageStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThat(query.findPublishedByHandle(OP, "sobre-nosotros", "es")).isEmpty();
    }

    @Test
    void a_localized_handle_from_another_tenant_is_refused() {
        PageJpaEntity foreign = new PageJpaEntity(
                PAGE, UUID.randomUUID(), "About", "about-us", "<p>Other.</p>",
                null, null, PageStatus.PUBLISHED, null,
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(OP, "es", "sobre-nosotros"))
                .thenReturn(Optional.of(translation("sobre-nosotros", "Sobre nosotros")));
        when(pageRepository.findById(PAGE)).thenReturn(Optional.of(foreign));
        when(pageRepository.findByTourOperatorIdAndHandleAndStatus(OP, "sobre-nosotros", PageStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThat(query.findPublishedByHandle(OP, "sobre-nosotros", "es")).isEmpty();
    }

    @Test
    void exposes_every_localized_handle_so_a_page_can_link_its_translations() {
        when(pageRepository.findByTourOperatorIdAndHandleAndStatus(OP, "about-us", PageStatus.PUBLISHED))
                .thenReturn(Optional.of(page(PageStatus.PUBLISHED)));
        when(translationRepository.findByPageId(PAGE))
                .thenReturn(List.of(translation("sobre-nosotros", "Sobre nosotros")));

        StorefrontPageView view = query.findPublishedByHandle(OP, "about-us", "en").orElseThrow();

        assertThat(view.handles()).containsExactly(Map.entry("es", "sobre-nosotros"));
        assertThat(view.canonicalHandle()).isEqualTo("about-us");
    }
}
