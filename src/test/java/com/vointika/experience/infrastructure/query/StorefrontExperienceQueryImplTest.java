package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.StorefrontExperienceView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontExperienceQueryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea3");
    private static final UUID THUMB = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea4");
    private static final UUID PHOTO = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea5");

    private ExperienceJpaRepository experienceRepository;
    private ExperienceTranslationJpaRepository translationRepository;
    private CriteriaListExecutor listExecutor;
    private StorefrontExperienceQueryImpl query;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceJpaRepository.class);
        translationRepository = mock(ExperienceTranslationJpaRepository.class);
        MediaUrlBatchResolver mediaUrlBatchResolver = mock(MediaUrlBatchResolver.class);

        when(mediaUrlBatchResolver.resolve(any(), anyList())).thenReturn(Map.of(
                THUMB, "https://media.example.com/thumb.jpg",
                PHOTO, "https://media.example.com/photo.jpg"));
        when(translationRepository.findByExperienceId(any())).thenReturn(List.of());
        when(translationRepository.findByExperienceIdInAndLocale(anyList(), any()))
                .thenReturn(List.of());
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(any(), any(), any()))
                .thenReturn(Optional.empty());

        listExecutor = mock(CriteriaListExecutor.class);
        query = new StorefrontExperienceQueryImpl(
                experienceRepository, translationRepository, listExecutor, mediaUrlBatchResolver);
    }

    private ExperienceJpaEntity experience() {
        return new ExperienceJpaEntity(
                EXPERIENCE, OP, UUID.randomUUID(), "morning-dive", "Morning dive",
                "A dive", "A long dive", false, List.of("diving"), List.of("Gear"),
                List.of("Lunch"), List.of("Reef"), List.of(THUMB, PHOTO), THUMB,
                90, 24, true, Instant.now());
    }

    @SuppressWarnings("unchecked")
    private void givenPage(ExperienceJpaEntity... rows) {
        when(listExecutor.list(any(), any(), any(), any()))
                .thenReturn(new CursorPage<>(List.of(rows), null));
    }

    /** Only name and slug translated — the rest null, which is the normal case. */
    private ExperienceTranslationJpaEntity translation(String slug, String name) {
        return new ExperienceTranslationJpaEntity(
                EXPERIENCE, "es", OP, name, null, null, null, null, null, slug);
    }

    @Test
    void lists_only_published_experiences_resolved_for_the_locale() {
        givenPage(experience());

        List<StorefrontExperienceView> views = query.listPublished(OP, "en", null).data();

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.slug()).isEqualTo("morning-dive");
            assertThat(view.name()).isEqualTo("Morning dive");
            assertThat(view.thumbnailUrl()).isEqualTo("https://media.example.com/thumb.jpg");
            assertThat(view.mediaUrls()).containsExactly(
                    "https://media.example.com/thumb.jpg", "https://media.example.com/photo.jpg");
            assertThat(view.durationMinutes()).isEqualTo(90);
        });
    }

    @Test
    void an_experience_with_no_media_at_all_still_renders() {
        // The batch resolver returns an immutable empty map when there is
        // nothing to resolve, and `Map.of().get(null)` throws — so an operator
        // whose experiences carry no images took the whole list page down.
        ExperienceJpaEntity noMedia = new ExperienceJpaEntity(
                EXPERIENCE, OP, UUID.randomUUID(), "morning-dive", "Morning dive",
                "A dive", "A long dive", false, List.of(), List.of(), List.of(), List.of(),
                List.of(), null, 90, 24, true, Instant.now());
        givenPage(noMedia);

        List<StorefrontExperienceView> views = query.listPublished(OP, "en", null).data();

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.thumbnailUrl()).isNull();
            assertThat(view.mediaUrls()).isEmpty();
        });
    }

    @Test
    void an_operator_with_nothing_published_costs_no_further_queries() {
        when(listExecutor.list(any(), any(), any(), any())).thenReturn(CursorPage.empty());

        assertThat(query.listPublished(OP, "en", null).data()).isEmpty();
    }

    @Test
    void a_translated_field_overlays_the_canonical_one_and_the_rest_falls_back() {
        givenPage(experience());
        when(translationRepository.findByExperienceIdInAndLocale(anyList(), any()))
                .thenReturn(List.of(translation("buceo-matutino", "Buceo matutino")));

        StorefrontExperienceView view = query.listPublished(OP, "es", null).data().getFirst();

        assertThat(view.name()).isEqualTo("Buceo matutino");
        assertThat(view.slug()).isEqualTo("buceo-matutino");
        // Untranslated fields keep the canonical text rather than going blank.
        assertThat(view.description()).isEqualTo("A dive");
        // Tags are facets, never translated.
        assertThat(view.tags()).containsExactly("diving");
    }

    @Test
    void finds_an_experience_by_its_canonical_handle() {
        when(experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(OP, "morning-dive"))
                .thenReturn(Optional.of(experience()));

        assertThat(query.findPublishedBySlug(OP, "morning-dive", "en")).isPresent();
    }

    @Test
    void finds_an_experience_by_its_localized_handle() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(OP, "es", "buceo-matutino"))
                .thenReturn(Optional.of(translation("buceo-matutino", "Buceo matutino")));
        when(experienceRepository.findById(EXPERIENCE)).thenReturn(Optional.of(experience()));
        when(translationRepository.findByExperienceId(EXPERIENCE))
                .thenReturn(List.of(translation("buceo-matutino", "Buceo matutino")));

        assertThat(query.findPublishedBySlug(OP, "buceo-matutino", "es"))
                .get().extracting(StorefrontExperienceView::name).isEqualTo("Buceo matutino");
    }

    @Test
    void a_detail_read_exposes_every_localized_handle() {
        // What S3 lacked: without these a translated page guesses its siblings'
        // handles, and the guess is a URL that 404s.
        when(experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(OP, "morning-dive"))
                .thenReturn(Optional.of(experience()));
        when(translationRepository.findByExperienceId(EXPERIENCE))
                .thenReturn(List.of(translation("buceo-matutino", "Buceo matutino")));

        StorefrontExperienceView view =
                query.findPublishedBySlug(OP, "morning-dive", "en").orElseThrow();

        assertThat(view.canonicalSlug()).isEqualTo("morning-dive");
        assertThat(view.handles()).containsExactly(Map.entry("es", "buceo-matutino"));
    }

    @Test
    void list_rows_carry_no_handle_map() {
        // A card links inside the locale being rendered, so paying for every
        // translation of every row would buy nothing.
        givenPage(experience());

        assertThat(query.listPublished(OP, "en", null).data().getFirst().handles()).isEmpty();
    }

    @Test
    void the_canonical_handle_still_resolves_after_the_operator_translates_it() {
        // Links shared before the translation existed must not rot.
        when(experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(OP, "morning-dive"))
                .thenReturn(Optional.of(experience()));

        assertThat(query.findPublishedBySlug(OP, "morning-dive", "es")).isPresent();
    }

    @Test
    void an_unpublished_handle_is_empty() {
        when(experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(OP, "draft"))
                .thenReturn(Optional.empty());

        assertThat(query.findPublishedBySlug(OP, "draft", "en")).isEmpty();
    }

    @Test
    void a_localized_handle_from_another_tenant_is_refused() {
        ExperienceJpaEntity foreign = new ExperienceJpaEntity(
                EXPERIENCE, UUID.randomUUID(), UUID.randomUUID(), "morning-dive", "Morning dive",
                null, null, false, List.of(), List.of(), List.of(), List.of(), List.of(), null,
                90, 24, true, Instant.now());
        when(translationRepository.findByTourOperatorIdAndLocaleAndSlug(OP, "es", "buceo-matutino"))
                .thenReturn(Optional.of(translation("buceo-matutino", "Buceo matutino")));
        when(experienceRepository.findById(EXPERIENCE)).thenReturn(Optional.of(foreign));
        when(experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(OP, "buceo-matutino"))
                .thenReturn(Optional.empty());

        assertThat(query.findPublishedBySlug(OP, "buceo-matutino", "es")).isEmpty();
    }
}
