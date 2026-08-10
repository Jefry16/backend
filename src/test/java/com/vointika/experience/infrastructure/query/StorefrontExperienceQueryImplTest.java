package com.vointika.experience.infrastructure.query;

import java.math.BigDecimal;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.StorefrontExperienceCard;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorefrontExperienceQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SAILING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID KAYAK = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THUMBNAIL = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private ExperienceJpaRepository experienceRepository;
    private ExperienceTranslationJpaRepository translationRepository;
    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private StorefrontExperienceQueryImpl query;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceJpaRepository.class);
        translationRepository = mock(ExperienceTranslationJpaRepository.class);
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        query = new StorefrontExperienceQueryImpl(experienceRepository, translationRepository, mediaAssetBatchQuery);
        when(translationRepository.findByTourOperatorIdAndLocale(any(), any())).thenReturn(List.of());
    }

    @Test
    void aTranslationOverlaysTheNameDescriptionAndHandle() {
        published(experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", null, 150, true));
        translated(translation(SAILING, "es", "Paseo en velero", "Crucero dorado", "paseo-en-velero"));

        StorefrontExperienceCard card = query.findPublished(OPERATOR, "es").getFirst();

        assertThat(card.name()).isEqualTo("Paseo en velero");
        assertThat(card.description()).isEqualTo("Crucero dorado");
        assertThat(card.handle()).isEqualTo("paseo-en-velero");
    }

    /** A row overlays, it does not replace: a null column falls back per column, not per row. */
    @Test
    void aColumnTheTranslationLeftNullFallsBackToTheCanonicalValue() {
        published(experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", null, 150, true));
        translated(translation(SAILING, "es", "Paseo en velero", null, null));

        StorefrontExperienceCard card = query.findPublished(OPERATOR, "es").getFirst();

        assertThat(card.name()).isEqualTo("Paseo en velero");
        assertThat(card.description()).isEqualTo("Golden-hour cruise");
    }

    /**
     * The localized-handle rule carried from {@code page} and {@code experience}:
     * a translation may carry its own handle, and without one the canonical handle
     * addresses the experience in that locale too (PATTERNS §4d).
     */
    @Test
    void theCanonicalHandleServesALocaleWithNoTranslationAtAll() {
        published(experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", null, 150, true));

        assertThat(query.findPublished(OPERATOR, "fr").getFirst().handle()).isEqualTo("sunset-sailing-tour");
    }

    @Test
    void aThumbnailResolvesToItsStorageKeyThroughOneBatchCall() {
        published(
                experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", THUMBNAIL, 150, true),
                experience(KAYAK, "kayak-cave-adventure", "Kayak Cave Adventure", "Sea caves", null, 120, false));
        when(mediaAssetBatchQuery.findAssetsByIds(OPERATOR, Set.of(THUMBNAIL)))
                .thenReturn(Map.of(THUMBNAIL, new MediaAsset("tour-operators/1/sunset.jpg", null, null, null)));

        List<StorefrontExperienceCard> cards = query.findPublished(OPERATOR, "es");

        assertThat(cards).extracting(StorefrontExperienceCard::thumbnailKey)
                .containsExactly("tour-operators/1/sunset.jpg", null);
        verify(mediaAssetBatchQuery).findAssetsByIds(OPERATOR, Set.of(THUMBNAIL));
    }

    /**
     * A media id the operator no longer owns is absent from the batch result,
     * which is a card without a picture rather than an error — and
     * {@code Map.of()} throws on {@code get(null)}, so the no-thumbnail case must
     * never reach the map at all.
     */
    @Test
    void aThumbnailIdThatNoLongerResolvesIsSimplyNoThumbnail() {
        published(experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", THUMBNAIL, 150, true));
        when(mediaAssetBatchQuery.findAssetsByIds(OPERATOR, Set.of(THUMBNAIL))).thenReturn(Map.of());

        assertThat(query.findPublished(OPERATOR, "es").getFirst().thumbnailKey()).isNull();
    }

    @Test
    void anOperatorWithNothingPublishedGetsAnEmptyListAndCostsNoMediaLookup() {
        published();

        assertThat(query.findPublished(OPERATOR, "es")).isEmpty();
        verify(mediaAssetBatchQuery, never()).findAssetsByIds(any(), anySet());
    }

    /** The order is the query's; sorting again here would be a second copy of the rule. */
    @Test
    void theCardsKeepTheOrderTheQueryReturnedThemIn() {
        published(
                experience(SAILING, "sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise", null, 150, true),
                experience(KAYAK, "kayak-cave-adventure", "Kayak Cave Adventure", "Sea caves", null, 120, false));

        assertThat(query.findPublished(OPERATOR, "es"))
                .extracting(StorefrontExperienceCard::name)
                .containsExactly("Sunset Sailing Tour", "Kayak Cave Adventure");
    }

    /**
     * <b>The ordering lives in the derived query's name and nowhere else</b> — the
     * adapter hands the rows straight through, so no assertion over a mocked
     * repository can observe an ORDER BY.
     *
     * <p>So the name is parsed the way Spring Data parses it and the resulting
     * {@code Sort} is compared against the rule written out in properties.
     * Asserting the method name against a string literal would not do: the
     * literal renames along with the method under any mechanical edit, and the
     * test would stay green while the listing reordered.
     */
    @Test
    void theListingOrderIsFeaturedThenOldestThenId() {
        Sort sort = new PartTree(listingQueryName(), ExperienceJpaEntity.class).getSort();

        assertThat(sort)
                .withFailMessage("The storefront listing is ordered featured first, then created_at, then id — "
                        + "deterministic and tie-broken, because a listing that reorders between requests is a "
                        + "bug report. The derived query now sorts: %s", sort)
                .containsExactly(Sort.Order.desc("featured"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    }

    private static String listingQueryName() {
        return Arrays.stream(ExperienceJpaRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("findByTourOperatorIdAndPublishedTrue"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the storefront listing query is gone from the repository"));
    }

    private void published(ExperienceJpaEntity... experiences) {
        when(experienceRepository.findByTourOperatorIdAndPublishedTrueOrderByFeaturedDescCreatedAtAscIdAsc(OPERATOR))
                .thenReturn(List.of(experiences));
    }

    private void translated(ExperienceTranslationJpaEntity... translations) {
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es")).thenReturn(List.of(translations));
    }

    private static ExperienceJpaEntity experience(UUID id, String handle, String name, String description,
                                                  UUID thumbnailMediaId, int durationMinutes, boolean featured) {
        return new ExperienceJpaEntity(
                id, OPERATOR, OPERATOR, handle, name, description, "long description",
                featured, List.of(), List.of(), List.of(),
                thumbnailMediaId, durationMinutes, 12, true, null, null, new BigDecimal("49.50"), Instant.EPOCH);
    }

    private static ExperienceTranslationJpaEntity translation(UUID experienceId, String locale, String name,
                                                              String description, String handle) {
        return new ExperienceTranslationJpaEntity(
                experienceId, locale, OPERATOR, name, description, null,
                null, null, handle, null, null);
    }
}
