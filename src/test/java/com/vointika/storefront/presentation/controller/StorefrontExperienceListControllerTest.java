package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.ColorView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.SocialLinkView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.MenuData;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
 * The experiences listing serves the globals — the same body as {@code /},
 * differing only in the two fields that say where it is.
 *
 * <p><b>What this file pins is the difference, not the body.</b> Both routes go
 * through one private builder in {@code StorefrontGlobalsResponse}, and
 * {@code StorefrontHomeControllerTest} already asserts that builder's output name
 * by name — 35 ways. Re-asserting the operator, brand, palette and metafields here
 * would pin the same method twice and still miss the only thing that can actually
 * differ per route: {@code pageType} and {@code canonicalUrl}, which are passed in
 * rather than derived. A route that reached for the index overload by habit would
 * publish {@code index} and the home page's canonical, and every body assertion
 * would still pass.
 *
 * <p>The per-route behaviours are here too, because each is genuinely per route
 * rather than per builder: the {@code PublicRoute} entry is method-and-pattern
 * specific, the gate's interceptor matches patterns, and the locale rule runs on
 * this path's own prefix.
 *
 * <p>No test sends an {@code Authorization} header and every one expects a body:
 * the storefront is public, and importing {@link StorefrontPublicRoutes} is what
 * proves it. Omit that import and every request 401s, so the assertions pass
 * without testing anything (PATTERNS §8c).
 */
@WebMvcTest(StorefrontExperienceListController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontExperienceListControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID LOGO = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");
    private static final UUID OG_IMAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb2");
    private static final UUID POLICY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb3");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb4");
    private static final UUID THUMB = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb5");
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001",
            "ES", "Spain");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any())).thenReturn(Map.of(
                LOGO, new MediaAsset("acme/logo.png", "Acme logo", 800, 400),
                OG_IMAGE, new MediaAsset("acme/og.png", null, null, null),
                THUMB, new MediaAsset("acme/sunset.png", "A boat at sunset", 1200, 800)));
        when(mediaUrlResolver.toUrl(anyString()))
                .thenAnswer(call -> "https://media.vointika.test/" + call.getArgument(0));
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.UNLOCKED);
    }

    private static StorefrontGlobals globals(String current, String primary, List<String> supported) {
        TourOperatorView operator = new TourOperatorView(OPERATOR, "Acme Tours", "acme", ADDRESS,
                "+34 600 000 000", "hola@acme.test",
                "Sailing day trips", "The best sailing in Mallorca", OG_IMAGE,
                "We open on Monday",
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView("Sail with us", "Day trips since 2011",
                        LOGO, null, null, null,
                        List.of(new ColorView("#0b3d5c", "#ffffff")),
                        List.of(),
                        List.of(new SocialLinkView("INSTAGRAM", "https://instagram.com/acme"))),
                List.of(new PolicyView(POLICY, "LEGAL_NOTICE", "Aviso legal")));
        return new StorefrontGlobals(operator, "Sailing day trips", "The best sailing in Mallorca",
                OG_IMAGE,
                List.of(),
                List.of(new ExperienceCardView(EXPERIENCE, "sunset-sail", "Sunset sail",
                        "Sail into the sunset", new BigDecimal("95.00"), THUMB)),
                List.of(new MenuData("main-menu", "Main menu", List.of()),
                        new MenuData("footer", "Footer", List.of())),
                new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale, StorefrontGlobals globals) {
        when(getStorefrontGlobalsUseCase.execute("acme", pathLocale)).thenReturn(Optional.of(globals));
    }

    /**
     * The whole point of the slice: this address is on the real render path, not
     * the placeholder's {@code {handle, status}}. Asserting a few globals rather
     * than none is what tells a reader the payload is the real one.
     */
    @Test
    void theListingServesTheGlobals() throws Exception {
        served(null, globals("en", "en", List.of("en")));

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.tourOperator.brand.slogan").value("Sail with us"))
                .andExpect(jsonPath("$.linklists['main-menu'].title").value("Main menu"))
                .andExpect(jsonPath("$.featuredExperiences[0].handle").value("sunset-sail"))
                .andExpect(jsonPath("$.localization.language.code").value("en"))
                // No object of its own yet, and NON_NULL keeps it off the wire.
                .andExpect(jsonPath("$.page").doesNotExist());
    }

    /**
     * <b>Not {@code index}.</b> The listing has no object of its own, so a builder
     * that inferred the type from which objects are present would call this the
     * home page. It names its own instead.
     */
    @Test
    void thePageTypeIsTheListingNotTheIndex() throws Exception {
        served(null, globals("en", "en", List.of("en")));

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.pageType").value("list-experiences"));
    }

    /** The other half of the same mistake: the canonical must be this address, not {@code /}. */
    @Test
    void theCanonicalUrlIsTheListingsOwnAddress() throws Exception {
        served(null, globals("en", "en", List.of("en")));

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/experiences"))
                .andExpect(jsonPath("$.routes.root").value("/"))
                .andExpect(jsonPath("$.routes.experiences").value("/experiences"));
    }

    @Test
    void theCanonicalUrlAndRoutesCarryTheLocalePrefix() throws Exception {
        served("es", globals("es", "en", List.of("en", "es")));

        mockMvc.perform(get("/es/experiences").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageType").value("list-experiences"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/es/experiences"))
                .andExpect(jsonPath("$.routes.root").value("/es"))
                .andExpect(jsonPath("$.routes.experiences").value("/es/experiences"));
    }

    /** Built from the resolved locale and path, never echoed off the request URI. */
    @Test
    void theCanonicalUrlDropsTheQueryString() throws Exception {
        served(null, globals("en", "en", List.of("en")));

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES + "?utm_source=newsletter")
                        .header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/experiences"));
    }

    @Test
    void aHostNoOperatorOwnsIs404() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /** Indistinguishable from the miss above, so neither says which one it was. */
    @Test
    void aLocaleTheShopDoesNotPublishIs404() throws Exception {
        when(getStorefrontGlobalsUseCase.execute("acme", "fr")).thenReturn(Optional.empty());

        mockMvc.perform(get("/fr/experiences").header("Host", "acme.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /**
     * Spring MVC serves HEAD from a {@code @GetMapping} for free; Spring Security
     * does not, and rejects an unlisted method at the filter chain as a 401 before
     * MVC is reached. The {@code PublicRoute} entry is per route <em>and</em> per
     * method, so coverage is per route too — this is the listing's own.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null, globals("en", "en", List.of("en")));
        served("es", globals("es", "en", List.of("en", "es")));

        mockMvc.perform(head(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/es/experiences").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    /**
     * The gate covers this address like every other page — its interceptor matches
     * on patterns, so a route added to {@code PAGE_ROUTES} without one would serve
     * a locked store's listing to anyone.
     */
    @Test
    void aLockedStoreRedirectsTheListing() throws Exception {
        served(null, globals("en", "en", List.of("en")));
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(status().is3xxRedirection());
    }
}
