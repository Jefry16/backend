package com.vointika.page.infrastructure.query;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Link resolution for a menu item pointing at a page.
 *
 * <p>The published filter is the whole point and it lives in the derived query's
 * name — swap it for an unfiltered read and a menu starts linking to drafts,
 * which is the state an operator chose to hide.
 */
class StorefrontPageQueryImplTest {

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

    private static PageJpaEntity page(UUID id, String handle) {
        PageJpaEntity page = mock(PageJpaEntity.class);
        when(page.getId()).thenReturn(id);
        when(page.getHandle()).thenReturn(handle);
        return page;
    }

    @Test
    void itAsksOnlyForPublishedPagesOfThisOperator() {
        PageJpaEntity about = page(ABOUT, "about-us");
        when(pageRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, Set.of(ABOUT)))
                .thenReturn(List.of(about));
        when(translationRepository.findByPageIdInAndLocale(any(), anyString())).thenReturn(List.of());

        assertThat(query.findPublishedHandles(OPERATOR, Set.of(ABOUT), "es"))
                .containsEntry(ABOUT, "about-us");
        verify(pageRepository).findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, Set.of(ABOUT));
    }

    /**
     * <b>An id that is not in the answer is the answer.</b> Unpublished, deleted
     * and another operator's are the same "no link here" to the caller, which is
     * what lets the menu drop it without asking why.
     */
    @Test
    void anUnpublishedPageIsSimplyAbsent() {
        when(pageRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, Set.of(ABOUT)))
                .thenReturn(List.of());

        assertThat(query.findPublishedHandles(OPERATOR, Set.of(ABOUT), "es")).isEmpty();
        verifyNoInteractions(translationRepository);
    }

    /** A link in a locale points at that locale's address. */
    @Test
    void aTranslatedHandleWins() {
        PageJpaEntity about = page(ABOUT, "about-us");
        PageTranslationJpaEntity spanish = mock(PageTranslationJpaEntity.class);
        when(spanish.getPageId()).thenReturn(ABOUT);
        when(spanish.getHandle()).thenReturn("sobre-nosotros");

        when(pageRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, Set.of(ABOUT)))
                .thenReturn(List.of(about));
        when(translationRepository.findByPageIdInAndLocale(any(), anyString()))
                .thenReturn(List.of(spanish));

        assertThat(query.findPublishedHandles(OPERATOR, Set.of(ABOUT), "es"))
                .containsEntry(ABOUT, "sobre-nosotros");
    }

    /** A translation row with a null handle falls back rather than blanking the URL. */
    @Test
    void aTranslationWithoutAHandleFallsBack() {
        PageJpaEntity about = page(ABOUT, "about-us");
        PageTranslationJpaEntity spanish = mock(PageTranslationJpaEntity.class);
        when(spanish.getPageId()).thenReturn(ABOUT);
        when(spanish.getHandle()).thenReturn(null);

        when(pageRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, Set.of(ABOUT)))
                .thenReturn(List.of(about));
        when(translationRepository.findByPageIdInAndLocale(any(), anyString()))
                .thenReturn(List.of(spanish));

        assertThat(query.findPublishedHandles(OPERATOR, Set.of(ABOUT), "es"))
                .containsEntry(ABOUT, "about-us");
    }

    /** Nothing to resolve, nothing to read. */
    @Test
    void noIdsReadsNothing() {
        assertThat(query.findPublishedHandles(OPERATOR, Set.of(), "es")).isEmpty();
        verifyNoInteractions(pageRepository, translationRepository);
    }
}
