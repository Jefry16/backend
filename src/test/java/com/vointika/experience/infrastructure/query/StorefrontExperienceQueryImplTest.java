package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private StorefrontExperienceQueryImpl query;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceJpaRepository.class);
        translationRepository = mock(ExperienceTranslationJpaRepository.class);
        query = new StorefrontExperienceQueryImpl(experienceRepository, translationRepository);
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
}
