package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.CategoryJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.CategoryJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.CategoryTranslationJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceDetailView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Which address serves which experience — the six rules
 * {@code StorefrontPageAddressTest} pins for pages, on the experience side.
 *
 * <p>The rule worth naming: <b>an experience a locale renames has one address in
 * that locale</b>, and its canonical handle is not a second one. Same rule that
 * makes {@code /{primary}} a 404 when the primary already lives at {@code /}.
 *
 * <p><b>This is also the first read that can observe a §4d shadowing handle.</b>
 * The write guards have existed since 2026-08-01 with nothing consulting both
 * namespaces in precedence order; {@code findByHandle} does, so a canonical
 * handle that another experience publishes as a localized one now resolves to the
 * wrong experience rather than being merely storable.
 *
 * <p><b>Mockito trap, recorded by the page sibling and it bites the same way
 * here:</b> a helper that stubs a mock must be called <em>before</em> the
 * {@code when(...)} it feeds, never nested inside {@code thenReturn(helper())} —
 * Mockito reads the nested {@code when()} as unfinished and blames the helper.
 */
class StorefrontExperienceAddressTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");
    private static final UUID OTHER_OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ec9");

    private ExperienceJpaRepository experienceRepository;
    private ExperienceTranslationJpaRepository translationRepository;
    private CategoryJpaRepository categoryRepository;
    private CategoryTranslationJpaRepository categoryTranslationRepository;
    private StorefrontExperienceQueryImpl query;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceJpaRepository.class);
        translationRepository = mock(ExperienceTranslationJpaRepository.class);
        categoryRepository = mock(CategoryJpaRepository.class);
        categoryTranslationRepository = mock(CategoryTranslationJpaRepository.class);
        query = new StorefrontExperienceQueryImpl(experienceRepository, translationRepository,
                categoryRepository, categoryTranslationRepository, mock(CriteriaListExecutor.class));
    }

    private ExperienceJpaEntity experience(boolean published, UUID owner) {
        ExperienceJpaEntity entity = mock(ExperienceJpaEntity.class);
        when(entity.getId()).thenReturn(EXPERIENCE);
        when(entity.getTourOperatorId()).thenReturn(owner);
        when(entity.getHandle()).thenReturn("sunset-sail");
        when(entity.getName()).thenReturn("Sunset sail");
        when(entity.getDescription()).thenReturn("Sail into the sunset");
        when(entity.getLongDescription()).thenReturn("The long English copy.");
        when(entity.getStartingPrice()).thenReturn(new BigDecimal("95.00"));
        when(entity.getMediaIds()).thenReturn(List.of());
        when(entity.isPublished()).thenReturn(published);
        when(entity.getCreatedAt()).thenReturn(Instant.parse("2026-07-21T10:00:00Z"));
        return entity;
    }

    private ExperienceTranslationJpaEntity translation(String locale, String handle, String name) {
        ExperienceTranslationJpaEntity t = mock(ExperienceTranslationJpaEntity.class);
        when(t.getExperienceId()).thenReturn(EXPERIENCE);
        when(t.getLocale()).thenReturn(locale);
        when(t.getHandle()).thenReturn(handle);
        when(t.getName()).thenReturn(name);
        return t;
    }

    @Test
    void theCanonicalHandleAddressesAnExperienceWithNoTranslation() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "en", "sunset-sail"))
                .thenReturn(Optional.empty());
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceIdAndLocale(EXPERIENCE, "en")).thenReturn(Optional.empty());
        when(translationRepository.findByExperienceId(EXPERIENCE)).thenReturn(List.of());

        ExperienceDetailView view = query.findByHandle(OPERATOR, "sunset-sail", "en").orElseThrow();

        assertThat(view.handle()).isEqualTo("sunset-sail");
        assertThat(view.name()).isEqualTo("Sunset sail");
        assertThat(view.handles().in("es")).isEqualTo("sunset-sail");
    }

    @Test
    void aTranslatedHandleAddressesItAndOverlaysTheContent() {
        ExperienceTranslationJpaEntity es = translation("es", "paseo-al-atardecer", "Paseo al atardecer");
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "paseo-al-atardecer"))
                .thenReturn(Optional.of(es));
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        when(experienceRepository.findById(EXPERIENCE)).thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceId(EXPERIENCE)).thenReturn(List.of(es));

        ExperienceDetailView view = query.findByHandle(OPERATOR, "paseo-al-atardecer", "es").orElseThrow();

        assertThat(view.handle()).isEqualTo("paseo-al-atardecer");
        assertThat(view.name()).isEqualTo("Paseo al atardecer");
        // Untranslated columns fall back rather than blanking.
        assertThat(view.longDescription()).isEqualTo("The long English copy.");
        assertThat(view.handles().in("es")).isEqualTo("paseo-al-atardecer");
        assertThat(view.handles().in("en")).isEqualTo("sunset-sail");
    }

    /** The duplicate-content rule: one address per locale, not two. */
    @Test
    void theCanonicalHandleIsNotASecondAddressInALocaleThatRenamesIt() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "sunset-sail"))
                .thenReturn(Optional.empty());
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        ExperienceTranslationJpaEntity es = translation("es", "paseo-al-atardecer", "Paseo al atardecer");
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceIdAndLocale(EXPERIENCE, "es")).thenReturn(Optional.of(es));

        assertThat(query.findByHandle(OPERATOR, "sunset-sail", "es")).isEmpty();
    }

    /** A translation row without a handle is not a rename. */
    @Test
    void aTranslationWithoutAHandleKeepsTheCanonicalAddress() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "sunset-sail"))
                .thenReturn(Optional.empty());
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        ExperienceTranslationJpaEntity es = translation("es", null, "Paseo al atardecer");
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceIdAndLocale(EXPERIENCE, "es")).thenReturn(Optional.of(es));
        when(translationRepository.findByExperienceId(EXPERIENCE)).thenReturn(List.of(es));

        ExperienceDetailView view = query.findByHandle(OPERATOR, "sunset-sail", "es").orElseThrow();

        assertThat(view.handle()).isEqualTo("sunset-sail");
        assertThat(view.name()).isEqualTo("Paseo al atardecer");
        // Not in byLocale, so `in` falls back — a null handle must not become /experiences/null.
        assertThat(view.handles().in("es")).isEqualTo("sunset-sail");
    }

    @Test
    void aDraftExperienceHasNoAddress() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "en", "sunset-sail"))
                .thenReturn(Optional.empty());
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.empty());

        assertThat(query.findByHandle(OPERATOR, "sunset-sail", "en")).isEmpty();
    }

    /**
     * The localized path arrives by id, so it re-checks what the derived query
     * would have enforced: a handle is unique per operator, not globally.
     */
    @Test
    void aTranslatedHandleStillChecksTenantAndPublishState() {
        ExperienceTranslationJpaEntity es = translation("es", "paseo-al-atardecer", "Paseo");
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "paseo-al-atardecer"))
                .thenReturn(Optional.of(es));

        ExperienceJpaEntity draft = experience(false, OPERATOR);
        when(experienceRepository.findById(EXPERIENCE)).thenReturn(Optional.of(draft));
        assertThat(query.findByHandle(OPERATOR, "paseo-al-atardecer", "es")).isEmpty();

        ExperienceJpaEntity foreign = experience(true, OTHER_OPERATOR);
        when(experienceRepository.findById(EXPERIENCE)).thenReturn(Optional.of(foreign));
        assertThat(query.findByHandle(OPERATOR, "paseo-al-atardecer", "es")).isEmpty();
    }

    @Test
    void anUncategorizedExperienceHasANullCategoryAndAsksNothing() {
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "en", "sunset-sail"))
                .thenReturn(Optional.empty());
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceIdAndLocale(EXPERIENCE, "en")).thenReturn(Optional.empty());
        when(translationRepository.findByExperienceId(EXPERIENCE)).thenReturn(List.of());

        assertThat(query.findByHandle(OPERATOR, "sunset-sail", "en").orElseThrow().category()).isNull();
        org.mockito.Mockito.verifyNoInteractions(categoryRepository, categoryTranslationRepository);
    }

    @Test
    void aCategoryNameIsOverlaidIntoTheRenderedLocale() {
        UUID categoryId = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ed1");
        ExperienceJpaEntity entity = experience(true, OPERATOR);
        when(entity.getCategoryId()).thenReturn(categoryId);
        when(translationRepository.findByTourOperatorIdAndLocaleAndHandle(OPERATOR, "es", "sunset-sail"))
                .thenReturn(Optional.empty());
        when(experienceRepository.findByTourOperatorIdAndHandleAndPublishedTrue(OPERATOR, "sunset-sail"))
                .thenReturn(Optional.of(entity));
        when(translationRepository.findByExperienceIdAndLocale(EXPERIENCE, "es")).thenReturn(Optional.empty());
        when(translationRepository.findByExperienceId(EXPERIENCE)).thenReturn(List.of());

        CategoryJpaEntity category = mock(CategoryJpaEntity.class);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Sea trips");
        when(categoryRepository.findByIdAndTourOperatorId(categoryId, OPERATOR))
                .thenReturn(Optional.of(category));
        com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationJpaEntity translated =
                mock(com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationJpaEntity.class);
        when(translated.getName()).thenReturn("Salidas al mar");
        when(categoryTranslationRepository.findByCategoryIdAndLocale(categoryId, "es"))
                .thenReturn(Optional.of(translated));

        assertThat(query.findByHandle(OPERATOR, "sunset-sail", "es").orElseThrow().category().name())
                .isEqualTo("Salidas al mar");
    }
}
