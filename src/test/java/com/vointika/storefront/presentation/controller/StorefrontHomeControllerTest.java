package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery.BrandView;
import com.vointika.shared.port.StorefrontShopQuery.ColorView;
import com.vointika.shared.port.StorefrontShopQuery.PolicyView;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;
import com.vointika.shared.port.StorefrontShopQuery.SocialLinkView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
 * The home page is the globals, so this pins the published contract: every name
 * here is one a theme will type, and renaming one later is a breaking change.
 *
 * <p>No test sends an {@code Authorization} header and every one expects a body:
 * the storefront is public, and importing {@link StorefrontPublicRoutes} is what
 * proves it. Omit that import and every request 401s, so the assertions pass
 * without testing anything (PATTERNS §8c).
 */
@WebMvcTest(StorefrontHomeController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontHomeControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID LOGO = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");
    private static final UUID OG_IMAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb2");
    private static final UUID POLICY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb3");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any())).thenReturn(Map.of(
                LOGO, new MediaAsset("acme/logo.png", "Acme logo", 800, 400),
                OG_IMAGE, new MediaAsset("acme/og.png", null, null, null)));
        when(mediaUrlResolver.toUrl(anyString()))
                .thenAnswer(call -> "https://media.vointika.test/" + call.getArgument(0));
    }

    private static StorefrontGlobals globals(String current, String primary, List<String> supported) {
        ShopView shop = new ShopView(OPERATOR, "Acme Tours", "acme", "Palma de Mallorca",
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
        return new StorefrontGlobals(shop, "Sailing day trips", "The best sailing in Mallorca",
                OG_IMAGE, new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale, StorefrontGlobals globals) {
        when(getStorefrontGlobalsUseCase.execute("acme", pathLocale)).thenReturn(Optional.of(globals));
    }

    @Test
    void theHomePageIsTheGlobals() throws Exception {
        served(null, globals("es", "es", List.of("es", "en")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.id").value(OPERATOR.toString()))
                .andExpect(jsonPath("$.shop.name").value("Acme Tours"))
                .andExpect(jsonPath("$.shop.handle").value("acme"))
                .andExpect(jsonPath("$.shop.description").value("The best sailing in Mallorca"))
                .andExpect(jsonPath("$.shop.phone").value("+34 600 000 000"))
                .andExpect(jsonPath("$.shop.passwordMessage").value("We open on Monday"))
                .andExpect(jsonPath("$.shop.currency.code").value("EUR"))
                .andExpect(jsonPath("$.shop.currency.symbol").value("€"))
                .andExpect(jsonPath("$.shop.timezone.name").value("Europe/Madrid"))
                .andExpect(jsonPath("$.shop.timezone.city").value("Madrid"))
                .andExpect(jsonPath("$.pageTitle").value("Sailing day trips"))
                .andExpect(jsonPath("$.pageDescription").value("The best sailing in Mallorca"));
    }

    /**
     * {@code shop.url} comes from the request, not from configuration, so it stays
     * right behind a proxy. The port rides along when it is not the scheme's
     * default, or every dev URL would be unreachable.
     */
    @Test
    void theShopUrlIsTheRequestsOrigin() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.shop.url").value("http://acme.localhost:8080"));
    }

    /** The palette is an ordered list a theme indexes into, and the brand is Shopify's shape. */
    @Test
    void theBrandCarriesThePaletteAndItsImages() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.shop.brand.slogan").value("Sail with us"))
                .andExpect(jsonPath("$.shop.brand.colors.primary[0].background").value("#0b3d5c"))
                .andExpect(jsonPath("$.shop.brand.colors.primary[0].foreground").value("#ffffff"))
                .andExpect(jsonPath("$.shop.brand.colors.secondary").isEmpty())
                .andExpect(jsonPath("$.shop.brand.socialLinks[0].platform").value("INSTAGRAM"))
                .andExpect(jsonPath("$.shop.brand.logo.url")
                        .value("https://media.vointika.test/acme/logo.png"))
                .andExpect(jsonPath("$.shop.brand.logo.alt").value("Acme logo"))
                .andExpect(jsonPath("$.shop.brand.logo.aspectRatio").value(2.0))
                .andExpect(jsonPath("$.shop.brand.squareLogo").doesNotExist());
    }

    /**
     * An image whose dimensions were never measured has no aspect ratio rather
     * than a made-up one — the value is derived from the pair, never stored.
     */
    @Test
    void anImageWithoutDimensionsHasNoAspectRatio() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.pageImageUrl").value("https://media.vointika.test/acme/og.png"));
    }

    /** {@code LEGAL_NOTICE} addresses {@code /policies/legal-notice}: the enum name is not the slug. */
    @Test
    void aPolicyCarriesItsUrlAndIsAlsoReachableByName() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.shop.policies[0].type").value("LEGAL_NOTICE"))
                .andExpect(jsonPath("$.shop.policies[0].title").value("Aviso legal"))
                .andExpect(jsonPath("$.shop.policies[0].url").value("/policies/legal-notice"))
                .andExpect(jsonPath("$.shop.legalNotice.id").value(POLICY.toString()))
                .andExpect(jsonPath("$.shop.privacyPolicy").doesNotExist());
    }

    @Test
    void routesAreBareOnThePrimaryLocale() throws Exception {
        served(null, globals("es", "es", List.of("es", "en")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.routes.root").value("/"))
                .andExpect(jsonPath("$.routes.experiences").value("/experiences"));
    }

    /** Every URL a secondary locale hands a theme carries its prefix, or the links leave the language. */
    @Test
    void routesAndPolicyUrlsCarryTheLocalePrefix() throws Exception {
        served("en", globals("en", "es", List.of("es", "en")));

        mockMvc.perform(get("/en").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.root").value("/en"))
                .andExpect(jsonPath("$.routes.experiences").value("/en/experiences"))
                .andExpect(jsonPath("$.shop.policies[0].url").value("/en/policies/legal-notice"));
    }

    /**
     * Both names are derived rather than curated: {@code name} is the language in
     * the shop's primary locale, {@code endonymName} the language in itself.
     * Shopify's {@code primary} is not "the one being served" — that is
     * {@code localization.language}.
     */
    @Test
    void localizationNamesEachLanguageTwice() throws Exception {
        served("en", globals("en", "es", List.of("es", "en")));

        mockMvc.perform(get("/en").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.localization.language.code").value("en"))
                .andExpect(jsonPath("$.localization.language.primary").value(false))
                .andExpect(jsonPath("$.localization.languages[0].code").value("es"))
                .andExpect(jsonPath("$.localization.languages[0].primary").value(true))
                .andExpect(jsonPath("$.localization.languages[0].endonymName").value("español"))
                .andExpect(jsonPath("$.localization.languages[0].name").value("español"))
                .andExpect(jsonPath("$.localization.languages[0].url").value("/"))
                .andExpect(jsonPath("$.localization.languages[1].code").value("en"))
                .andExpect(jsonPath("$.localization.languages[1].endonymName").value("English"))
                .andExpect(jsonPath("$.localization.languages[1].name").value("inglés"))
                .andExpect(jsonPath("$.localization.languages[1].url").value("/en"));
    }

    @Test
    void aHostNoOperatorOwnsIs404() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /**
     * An address the shop does not publish answers exactly like a shop that does
     * not exist. Telling them apart tells an anonymous visitor which shops are
     * real and which languages they have.
     */
    @Test
    void aLocaleTheShopDoesNotPublishIs404() throws Exception {
        when(getStorefrontGlobalsUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de").header("Host", "acme.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /**
     * Spring MVC serves HEAD from a {@code @GetMapping} for free; Spring Security
     * does not, and rejects an unlisted method at the filter chain before MVC is
     * reached. It took a request against the built stack to find last time.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null, globals("es", "es", List.of("es")));
        served("en", globals("en", "es", List.of("es", "en")));

        mockMvc.perform(head("/").header("Host", "acme.localhost:8080")).andExpect(status().isOk());
        mockMvc.perform(head("/en").header("Host", "acme.localhost:8080")).andExpect(status().isOk());
    }
}
