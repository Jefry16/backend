package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.LocalizedHandles;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontExperienceQuery.CategoryView;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceDetailView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontExperienceOutput;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetStorefrontExperienceUseCase;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The experience detail page — the address the listing's cards and every
 * {@code EXPERIENCE} menu link have promised since before it existed.
 *
 * <p>No test sends an {@code Authorization} header and every one expects a body:
 * the storefront is public, and importing {@link StorefrontPublicRoutes} is what
 * proves it. Omit that import and every request 401s, so the assertions pass
 * without testing anything (PATTERNS §8c).
 */
@WebMvcTest(StorefrontExperienceDetailController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontExperienceDetailControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb4");
    private static final UUID THUMB = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb5");
    private static final UUID PHOTO = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb6");
    private static final UUID CATEGORY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ed1");
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001", "ES", "Spain");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontExperienceUseCase getStorefrontExperienceUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any())).thenReturn(Map.of(
                THUMB, new MediaAsset("acme/sunset.png", "A boat at sunset", 1200, 800),
                PHOTO, new MediaAsset("acme/deck.png", null, null, null)));
        when(mediaUrlResolver.toUrl(anyString()))
                .thenAnswer(call -> "https://media.vointika.test/" + call.getArgument(0));
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.UNLOCKED);
    }

    private static ExperienceDetailView view(String handle, CategoryView category) {
        return new ExperienceDetailView(EXPERIENCE, handle, "Sunset sail", "Sail into the sunset",
                "The long copy.", new BigDecimal("95.00"), THUMB, List.of(THUMB, PHOTO),
                null, null, true, 12, Instant.parse("2026-07-21T10:00:00Z"), category,
                new LocalizedHandles("sunset-sail", Map.of("es", "paseo-al-atardecer")));
    }

    private static StorefrontGlobals globals(String current, String primary, List<String> supported) {
        TourOperatorView operator = new TourOperatorView(OPERATOR, "Acme Tours", "acme", ADDRESS, null, null,
                "Sailing day trips", "The best sailing in Mallorca", null, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
        return new StorefrontGlobals(operator, "Sunset sail", "Sail into the sunset", null,
                List.of(), List.of(), List.of(), new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale, String handle, ExperienceDetailView view,
                        List<MetafieldView> metafields) {
        when(getStorefrontExperienceUseCase.execute("acme", pathLocale, handle))
                .thenReturn(Optional.of(new StorefrontExperienceOutput(
                        globals(pathLocale == null ? "en" : pathLocale, "en", List.of("en", "es")),
                        view, metafields)));
    }

    private void served(String pathLocale, String handle) {
        served(pathLocale, handle, view(handle, null), List.of());
    }

    /** The globals plus one object, which is the whole shape of a page route. */
    @Test
    void anExperiencePageIsTheGlobalsPlusTheExperience() throws Exception {
        served(null, "sunset-sail");

        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.experience.id").value(EXPERIENCE.toString()))
                .andExpect(jsonPath("$.experience.handle").value("sunset-sail"))
                .andExpect(jsonPath("$.experience.name").value("Sunset sail"))
                .andExpect(jsonPath("$.experience.longDescription").value("The long copy."))
                .andExpect(jsonPath("$.experience.startingPrice").value("95.00"))
                .andExpect(jsonPath("$.experience.url").value("/experiences/sunset-sail"))
                // The listing's object belongs to the listing, and page to the CMS route.
                .andExpect(jsonPath("$.experiences").doesNotExist())
                .andExpect(jsonPath("$.page").doesNotExist());
    }

    @Test
    void thePageTypeIsTheExperienceAndTheCanonicalIsItsOwnAddress() throws Exception {
        served(null, "sunset-sail");

        mockMvc.perform(get("/experiences/sunset-sail?utm_source=newsletter")
                        .header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.pageType").value("experience"))
                .andExpect(jsonPath("$.canonicalUrl")
                        .value("http://acme.localhost:8080/experiences/sunset-sail"));
    }

    /**
     * <b>The gallery must ride the globals' media batch.</b> Resolved separately it
     * would be one lookup per photo; missed entirely it would render as an
     * experience with no photos rather than as an error, because the gallery drops
     * ids that do not resolve.
     */
    @Test
    void theGalleryAndThumbnailAreResolvedInOneBatch() throws Exception {
        served(null, "sunset-sail");

        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.experience.thumbnail.url")
                        .value("https://media.vointika.test/acme/sunset.png"))
                .andExpect(jsonPath("$.experience.thumbnail.alt").value("A boat at sunset"))
                .andExpect(jsonPath("$.experience.gallery.length()").value(2))
                .andExpect(jsonPath("$.experience.gallery[1].url")
                        .value("https://media.vointika.test/acme/deck.png"));
    }

    @Test
    void aCategoryIsServedByNameAndAnUncategorizedExperienceCarriesNull() throws Exception {
        served(null, "sunset-sail", view("sunset-sail", new CategoryView(CATEGORY, "Sea trips")), List.of());
        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.experience.category.name").value("Sea trips"))
                .andExpect(jsonPath("$.experience.category.id").value(CATEGORY.toString()));

        served(null, "kayak-trip");
        mockMvc.perform(get("/experiences/kayak-trip").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.experience.category").doesNotExist());
    }

    /** Addressed the way `tourOperator.metafields` is: namespace, then key. */
    @Test
    void metafieldsAreNestedByNamespace() throws Exception {
        served(null, "sunset-sail", view("sunset-sail", null), List.of(
                new MetafieldView("custom", "difficulty", "single_line_text", "Easy", null),
                new MetafieldView("custom", "min-age", "number_integer", "8", null)));

        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.experience.metafields.custom.difficulty.value").value("Easy"))
                .andExpect(jsonPath("$.experience.metafields.custom.difficulty.type")
                        .value("single_line_text"))
                .andExpect(jsonPath("$.experience.metafields.custom['min-age'].value").value("8"));
    }

    /** Columns with no renderer yet, in because §2a says a field with a column goes in. */
    @Test
    void theOperationalColumnsAreServed() throws Exception {
        served(null, "sunset-sail");

        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.experience.featured").value(true))
                .andExpect(jsonPath("$.experience.bookingCutoffHours").value(12))
                .andExpect(jsonPath("$.experience.createdAt").value("2026-07-21T10:00:00Z"));
    }

    @Test
    void aLocalizedAddressCarriesThePrefix() throws Exception {
        served("es", "paseo-al-atardecer", view("paseo-al-atardecer", null), List.of());

        mockMvc.perform(get("/es/experiences/paseo-al-atardecer").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience.url").value("/es/experiences/paseo-al-atardecer"))
                .andExpect(jsonPath("$.canonicalUrl")
                        .value("http://acme.localhost:8080/es/experiences/paseo-al-atardecer"));
    }

    /**
     * Five different misses, one answer. A handle nothing answers to, a draft, an
     * unpublished locale, an unknown host, and the canonical handle of an
     * experience this locale renames all look identical from outside.
     */
    @Test
    void everyMissIsTheSame404() throws Exception {
        when(getStorefrontExperienceUseCase.execute("acme", null, "nope")).thenReturn(Optional.empty());
        mockMvc.perform(get("/experiences/nope").header("Host", "acme.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));

        when(tenantHandleResolver.resolve("nobody.localhost")).thenReturn(Optional.empty());
        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "nobody.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /**
     * Spring MVC serves HEAD from a {@code @GetMapping} for free; Spring Security
     * does not, and rejects an unlisted method at the filter chain as a 401 before
     * MVC is reached. Registration is by construction via {@code PAGE_ROUTES} —
     * this test is not.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null, "sunset-sail");
        served("es", "paseo-al-atardecer", view("paseo-al-atardecer", null), List.of());

        mockMvc.perform(head("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/es/experiences/paseo-al-atardecer").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    /** The gate covers this address like every other page route. */
    @Test
    void aLockedStoreRedirectsTheExperiencePage() throws Exception {
        served(null, "sunset-sail");
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/experiences/sunset-sail").header("Host", "acme.localhost:8080"))
                .andExpect(status().is3xxRedirection());
    }
}
