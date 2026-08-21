package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.Filter;
import com.vointika.shared.list.FilterOp;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The featured read, whose filter, order and cap all live in one derived query
 * name — so the tests that matter parse that name rather than trusting it.
 */
class StorefrontExperienceQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID ONE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");

    private static final String QUERY_METHOD =
            "findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc";

    private ExperienceJpaRepository experienceRepository;
    private ExperienceTranslationJpaRepository translationRepository;
    private CriteriaListExecutor listExecutor;
    private StorefrontExperienceQueryImpl query;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceJpaRepository.class);
        translationRepository = mock(ExperienceTranslationJpaRepository.class);
        listExecutor = mock(CriteriaListExecutor.class);
        query = new StorefrontExperienceQueryImpl(experienceRepository, translationRepository, listExecutor);
    }

    /**
     * <b>Parsed, not asserted as a string.</b> A literal would rename along with
     * the method under any mechanical edit and stay green — this reads what
     * Spring Data will actually do with the name.
     */
    @Test
    void theOrderIsTheQueryNamesPromise() {
        PartTree tree = new PartTree(QUERY_METHOD, ExperienceJpaEntity.class);

        assertThat(tree.getSort().toString()).isEqualTo("createdAt: ASC,id: ASC");
    }

    /**
     * The cap is part of the same name, so a merchant who features everything
     * still cannot grow what rides every storefront page — and the port's
     * constant and the query have to agree.
     */
    @Test
    void theCapIsTheQueryNamesPromiseToo() {
        PartTree tree = new PartTree(QUERY_METHOD, ExperienceJpaEntity.class);

        assertThat(tree.isLimiting()).isTrue();
        assertThat(tree.getMaxResults()).isEqualTo(StorefrontExperienceQuery.FEATURED_LIMIT);
    }

    /** Draft and unfeatured rows are excluded by the name as well, not by the caller. */
    @Test
    void theFilterIsPublishedAndFeatured() {
        assertThat(Arrays.stream(ExperienceJpaRepository.class.getMethods())
                .map(Method::getName)
                .filter(QUERY_METHOD::equals))
                .as("the derived query the storefront depends on")
                .hasSize(1);
        assertThat(QUERY_METHOD).contains("PublishedTrue").contains("FeaturedTrue");
    }

    @Test
    void aTranslationOverlaysTheCardIncludingItsHandle() {
        // Both built before either stubbing below: each stubs a mock of its own,
        // and Mockito reads a nested when() as an unfinished one.
        ExperienceJpaEntity experience = experience();
        ExperienceTranslationJpaEntity spanish = mock(ExperienceTranslationJpaEntity.class);
        when(spanish.getExperienceId()).thenReturn(ONE);
        when(spanish.getHandle()).thenReturn("paseo-al-atardecer");
        when(spanish.getName()).thenReturn("Paseo al atardecer");
        when(spanish.getDescription()).thenReturn(null);

        when(experienceRepository.findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc(OPERATOR))
                .thenReturn(List.of(experience));
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(List.of(spanish));

        assertThat(query.findFeatured(OPERATOR, "es")).singleElement().satisfies(card -> {
            assertThat(card.handle()).isEqualTo("paseo-al-atardecer");
            assertThat(card.name()).isEqualTo("Paseo al atardecer");
            assertThat(card.description()).isEqualTo("Sail into the sunset");
            assertThat(card.startingPrice()).isEqualByComparingTo("95.00");
        });
    }

    /** Nothing featured means nothing to translate — the second query never runs. */
    @Test
    void noFeaturedRowsSkipsTheTranslationRead() {
        when(experienceRepository.findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc(OPERATOR))
                .thenReturn(List.of());

        assertThat(query.findFeatured(OPERATOR, "es")).isEmpty();
        verifyNoInteractions(translationRepository);
    }

    private static ExperienceJpaEntity experience() {
        ExperienceJpaEntity entity = mock(ExperienceJpaEntity.class);
        when(entity.getId()).thenReturn(ONE);
        when(entity.getHandle()).thenReturn("sunset-sail");
        when(entity.getName()).thenReturn("Sunset sail");
        when(entity.getDescription()).thenReturn("Sail into the sunset");
        when(entity.getStartingPrice()).thenReturn(new BigDecimal("95.00"));
        return entity;
    }

    /** The link-resolution half: published only, translated handle, absent means "no link". */
    @Test
    void publishedHandlesResolveInTheRenderedLocale() {
        ExperienceJpaEntity experience = experience();
        ExperienceTranslationJpaEntity spanish = mock(ExperienceTranslationJpaEntity.class);
        when(spanish.getExperienceId()).thenReturn(ONE);
        when(spanish.getHandle()).thenReturn("paseo-al-atardecer");

        when(experienceRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, java.util.Set.of(ONE)))
                .thenReturn(List.of(experience));
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(List.of(spanish));

        assertThat(query.findPublishedHandles(OPERATOR, java.util.Set.of(ONE), "es"))
                .containsEntry(ONE, "paseo-al-atardecer");
    }

    @Test
    void anUnpublishedExperienceIsSimplyAbsent() {
        when(experienceRepository.findByTourOperatorIdAndIdInAndPublishedTrue(OPERATOR, java.util.Set.of(ONE)))
                .thenReturn(List.of());

        assertThat(query.findPublishedHandles(OPERATOR, java.util.Set.of(ONE), "es")).isEmpty();
        verifyNoInteractions(translationRepository);
    }

    @Test
    void noIdsReadsNothing() {
        assertThat(query.findPublishedHandles(OPERATOR, java.util.Set.of(), "es")).isEmpty();
        verifyNoInteractions(experienceRepository, translationRepository);
    }

    // ---- listPublished ------------------------------------------------------

    private static final SortSpec NEWEST_FIRST = SortSpec.parse("-createdAt");

    @SuppressWarnings("unchecked")
    private ListQuery executedQuery() {
        ArgumentCaptor<ListQuery> captor = ArgumentCaptor.forClass(ListQuery.class);
        verify(listExecutor).list(eq(ExperienceJpaEntity.class), any(ListSchema.class),
                captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private void executorReturns(CursorPage<ExperienceJpaEntity> page) {
        when(listExecutor.list(eq(ExperienceJpaEntity.class), any(ListSchema.class),
                any(ListQuery.class), any()))
                .thenAnswer(call -> {
                    java.util.function.Function<ExperienceJpaEntity, Object> f = call.getArgument(3);
                    return new CursorPage<>(page.data().stream().map(f).toList(), page.nextCursor());
                });
    }

    /**
     * <b>The hole this closes.</b> {@code published} is absent from the schema the
     * URL is parsed against, so a visitor cannot send it; this asserts the
     * implementation adds it rather than trusting the caller to.
     */
    @Test
    void theListingIsPublishedOnlyBecauseWeSaySo() {
        executorReturns(new CursorPage<>(List.of(experience()), null));

        query.listPublished(OPERATOR, "en", null);

        assertThat(executedQuery().filters().filters())
                .singleElement()
                .isEqualTo(new Filter("published", FilterOp.EQ, Boolean.TRUE));
    }

    /**
     * A visitor never names the tenant. The parser is handed a null id and the
     * operator resolved from the host is what reaches the executor.
     */
    @Test
    void theTenantIsTheHostsOperatorAndNotTheCallers() {
        executorReturns(new CursorPage<>(List.of(experience()), null));

        query.listPublished(OPERATOR, "en", null);

        assertThat(executedQuery().tenantId()).isEqualTo(OPERATOR);
    }

    /** The cursor and sort ride through untouched — paging is the framework's. */
    @Test
    void theCursorIsCarriedThrough() {
        executorReturns(new CursorPage<>(List.of(experience()), "next-page"));

        CursorPage<StorefrontExperienceQuery.ExperienceCardView> page =
                query.listPublished(OPERATOR, "en", "from-here");

        assertThat(executedQuery().cursor()).isEqualTo("from-here");
        assertThat(executedQuery().sort()).isEqualTo(NEWEST_FIRST);
        assertThat(page.nextCursor()).isEqualTo("next-page");
    }

    /** Same overlay as the featured read, handle included. */
    @Test
    void cardsAreOverlaidIntoTheRenderedLocale() {
        executorReturns(new CursorPage<>(List.of(experience()), null));
        ExperienceTranslationJpaEntity translation = mock(ExperienceTranslationJpaEntity.class);
        when(translation.getExperienceId()).thenReturn(ONE);
        when(translation.getHandle()).thenReturn("paseo-al-atardecer");
        when(translation.getName()).thenReturn("Paseo al atardecer");
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(List.of(translation));

        var card = query.listPublished(OPERATOR, "es", null).data().getFirst();

        assertThat(card.handle()).isEqualTo("paseo-al-atardecer");
        assertThat(card.name()).isEqualTo("Paseo al atardecer");
        // Untranslated falls back rather than blanking.
        assertThat(card.description()).isEqualTo("Sail into the sunset");
    }

    /**
     * An operator with nothing published is an empty listing, not a 404 — and it
     * costs no translation query, which is the N+1 guard on the cheapest page.
     */
    @Test
    void anEmptyPageReadsNoTranslations() {
        executorReturns(new CursorPage<>(List.of(), null));

        assertThat(query.listPublished(OPERATOR, "es", null).data()).isEmpty();
        verifyNoInteractions(translationRepository);
    }
}
