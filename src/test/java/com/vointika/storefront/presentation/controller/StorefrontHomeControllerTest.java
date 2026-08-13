package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.storefront.application.dto.output.MenuData;
import com.vointika.storefront.application.dto.output.MenuData.MenuLinkData;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.ColorView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.SocialLinkView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
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
import jakarta.servlet.http.Cookie;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        // Unlocked unless a test says otherwise: the gate is real in this slice
        // (StorefrontWebConfig is a WebMvcConfigurer, so the slice registers it).
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
                List.of(new MetafieldView("custom", "opening-hours", "single_line_text", "Mon-Sat 09:00-18:00"),
                        new MetafieldView("custom", "meeting-point", "single_line_text", "Muelle 3"),
                        new MetafieldView("legal", "licence", "single_line_text", "TA-1123")),
                List.of(new ExperienceCardView(EXPERIENCE, "sunset-sail", "Sunset sail",
                        "Sail into the sunset", new java.math.BigDecimal("95.00"), THUMB)),
                List.of(new MenuData("main-menu", "Main menu", List.of(
                                new MenuLinkData("Home", "HOME", null, null, List.of()),
                                new MenuLinkData("Trips", "EXPERIENCE_LIST", null, null, List.of(
                                        new MenuLinkData("Sunset sail", "EXPERIENCE", "sunset-sail",
                                                null, List.of()))),
                                new MenuLinkData("About", "PAGE", "about-us", null, List.of()),
                                new MenuLinkData("Blog", "EXTERNAL_URL", null,
                                        "https://example.com/blog", List.of()))),
                        new MenuData("footer", "Footer", List.of())),
                new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale, StorefrontGlobals globals) {
        when(getStorefrontGlobalsUseCase.execute("acme", pathLocale)).thenReturn(Optional.of(globals));
    }

    @Test
    void theHomePageIsTheGlobals() throws Exception {
        served(null, globals("es", "es", List.of("es", "en")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.id").value(OPERATOR.toString()))
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.tourOperator.handle").value("acme"))
                .andExpect(jsonPath("$.tourOperator.description").value("The best sailing in Mallorca"))
                .andExpect(jsonPath("$.tourOperator.phone").value("+34 600 000 000"))
                .andExpect(jsonPath("$.tourOperator.passwordMessage").value("We open on Monday"))
                .andExpect(jsonPath("$.tourOperator.currency.code").value("EUR"))
                .andExpect(jsonPath("$.tourOperator.currency.symbol").value("€"))
                .andExpect(jsonPath("$.tourOperator.timezone.name").value("Europe/Madrid"))
                .andExpect(jsonPath("$.tourOperator.timezone.city").value("Madrid"))
                .andExpect(jsonPath("$.pageTitle").value("Sailing day trips"))
                .andExpect(jsonPath("$.pageDescription").value("The best sailing in Mallorca"));
    }

    /**
     * {@code operator.url} comes from the request, not from configuration, so it stays
     * right behind a proxy. The port rides along when it is not the scheme's
     * default, or every dev URL would be unreachable.
     */
    @Test
    void theShopUrlIsTheRequestsOrigin() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.tourOperator.url").value("http://acme.localhost:8080"));
    }

    /** The palette is an ordered list a theme indexes into, and the brand is Shopify's shape. */
    @Test
    void theBrandCarriesThePaletteAndItsImages() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.tourOperator.brand.slogan").value("Sail with us"))
                .andExpect(jsonPath("$.tourOperator.brand.colors.primary[0].background").value("#0b3d5c"))
                .andExpect(jsonPath("$.tourOperator.brand.colors.primary[0].foreground").value("#ffffff"))
                .andExpect(jsonPath("$.tourOperator.brand.colors.secondary").isEmpty())
                .andExpect(jsonPath("$.tourOperator.brand.socialLinks[0].platform").value("INSTAGRAM"))
                .andExpect(jsonPath("$.tourOperator.brand.logo.url")
                        .value("https://media.vointika.test/acme/logo.png"))
                .andExpect(jsonPath("$.tourOperator.brand.logo.alt").value("Acme logo"))
                .andExpect(jsonPath("$.tourOperator.brand.logo.aspectRatio").value(2.0))
                .andExpect(jsonPath("$.tourOperator.brand.squareLogo").doesNotExist());
    }

    /**
     * An image whose dimensions were never measured has no aspect ratio rather
     * than a made-up one — the value is derived from the pair, never stored.
     */
    @Test
    void anImageWithoutDimensionsHasNoAspectRatio() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.ogImageUrl").value("https://media.vointika.test/acme/og.png"));
    }

    /** {@code LEGAL_NOTICE} addresses {@code /policies/legal-notice}: the enum name is not the slug. */
    @Test
    void aPolicyCarriesItsUrlAndIsAlsoReachableByName() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.tourOperator.policies[0].type").value("LEGAL_NOTICE"))
                .andExpect(jsonPath("$.tourOperator.policies[0].title").value("Aviso legal"))
                .andExpect(jsonPath("$.tourOperator.policies[0].url").value("/policies/legal-notice"))
                .andExpect(jsonPath("$.tourOperator.legalNotice.id").value(POLICY.toString()))
                .andExpect(jsonPath("$.tourOperator.privacyPolicy").doesNotExist());
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
                .andExpect(jsonPath("$.tourOperator.policies[0].url").value("/en/policies/legal-notice"));
    }

    /**
     * Both names are derived rather than curated: {@code name} is the language in
     * the operator's primary locale, {@code endonymName} the language in itself.
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
     * An address the operator does not publish answers exactly like a operator that does
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

    /**
     * <b>The ordering rule, and the reason the gate waited for the locale rule to
     * exist.</b> Resolve the locale first and a locked store answers {@code /de}
     * with a 404 while answering {@code /en} with a redirect — which tells an
     * anonymous visitor, from in front of the gate, that the store is real and
     * exactly which languages it publishes. Locked means every path answers the
     * same.
     *
     * <p>{@code de} is deliberately a locale this operator does not publish: the
     * globals use case would answer empty for it, so a 302 here proves nothing
     * downstream of the gate ran.
     */
    @Test
    void aLockedStoreRedirectsEvenForALocaleItDoesNotPublish() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getStorefrontGlobalsUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de").header("Host", "acme.localhost:8080"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /** And the home page itself, so the gate is not merely a locale-path behaviour. */
    @Test
    void aLockedStoreRedirectsTheHomePage() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * The interceptor reads the unlock cookie by name and hands its value to the
     * gate. Rename it on either side and the value arrives as null, the store
     * stays locked, and this fails on a 302 instead of a body.
     */
    @Test
    void aValidUnlockCookieGetsThroughTheGate() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(checkStorefrontLockUseCase.execute("acme", "a-valid-token")).thenReturn(LockState.UNLOCKED);
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/")
                        .header("Host", "acme.localhost:8080")
                        .cookie(new Cookie(UnlockTokenPort.COOKIE_NAME, "a-valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"));
    }

    /**
     * Shopify's shape — {@code operator.metafields.namespace.key} — because that is
     * the address a theme author types. Ours differs in exactly two ways, both
     * decided rather than accidental: the {@code type} vocabulary is ours
     * ({@code single_line_text}, not {@code single_line_text_field}), and a
     * value is read as {@code .value} because JSON has no drop that renders
     * itself.
     */
    @Test
    void theShopCarriesItsMetafieldsNestedByNamespace() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.metafields.custom['opening-hours'].value")
                        .value("Mon-Sat 09:00-18:00"))
                .andExpect(jsonPath("$.tourOperator.metafields.custom['opening-hours'].type")
                        .value("single_line_text"))
                .andExpect(jsonPath("$.tourOperator.metafields.custom['meeting-point'].value").value("Muelle 3"))
                .andExpect(jsonPath("$.tourOperator.metafields.legal.licence.value").value("TA-1123"));
    }

    /**
     * No {@code list?}, because list types are not in our catalogue — a field
     * that is always false is invention, and the admission rule forbids it.
     */
    @Test
    void aMetafieldCarriesOnlyTypeAndValue() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.tourOperator.metafields.legal.licence.list").doesNotExist())
                .andExpect(jsonPath("$.tourOperator.metafields.legal.licence.name").doesNotExist())
                .andExpect(jsonPath("$.tourOperator.metafields.legal.licence.updatedAt").doesNotExist());
    }

    /** An operator who has filled in nothing gets an empty object, not a null. */
    @Test
    void anOperatorWithNoMetafieldsGetsAnEmptyMap() throws Exception {
        StorefrontGlobals bare = new StorefrontGlobals(
                globals("es", "es", List.of("es")).tourOperator(),
                "Sailing day trips", "The best sailing in Mallorca", OG_IMAGE,
                List.of(), List.of(), List.of(), new LocalizationData("es", "es", List.of("es")));
        served(null, bare);

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.tourOperator.metafields").isEmpty())
                .andExpect(jsonPath("$.tourOperator.metafields").exists());
    }

    /**
     * The cards are top-level, not under {@code operator} — they are catalogue, which
     * is where Shopify keeps them too.
     */
    @Test
    void theGlobalsCarryTheFeaturedExperiences() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredExperiences[0].handle").value("sunset-sail"))
                .andExpect(jsonPath("$.featuredExperiences[0].name").value("Sunset sail"))
                .andExpect(jsonPath("$.featuredExperiences[0].url").value("/experiences/sunset-sail"))
                .andExpect(jsonPath("$.tourOperator.featuredExperiences").doesNotExist());
    }

    /**
     * A decimal amount, as a string. A JSON number is a double on the far side of
     * most parsers, and this is a value a customer is asked to pay.
     */
    @Test
    void aCardsPriceKeepsItsCents() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.featuredExperiences[0].startingPrice").value("95.00"));
    }

    /**
     * The thumbnails ride the same media batch as the brand images — one lookup
     * for the whole response, which is what {@code mediaIds} exists to guarantee.
     */
    @Test
    void aCardsThumbnailIsResolvedLikeEveryOtherImage() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.featuredExperiences[0].thumbnail.url")
                        .value("https://media.vointika.test/acme/sunset.png"))
                .andExpect(jsonPath("$.featuredExperiences[0].thumbnail.alt").value("A boat at sunset"))
                .andExpect(jsonPath("$.featuredExperiences[0].thumbnail.aspectRatio").value(1.5));
    }

    /** On a secondary locale every card link carries the prefix, or it leaves the language. */
    @Test
    void aCardsUrlCarriesTheLocalePrefix() throws Exception {
        served("en", globals("en", "es", List.of("es", "en")));

        mockMvc.perform(get("/en").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.featuredExperiences[0].url").value("/en/experiences/sunset-sail"));
    }

    /** Keyed by handle, the way {@code linklists["main-menu"]} is in Liquid. */
    @Test
    void theGlobalsCarryEveryMenuKeyedByHandle() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linklists['main-menu'].title").value("Main menu"))
                .andExpect(jsonPath("$.linklists.footer.title").value("Footer"))
                .andExpect(jsonPath("$.linklists.footer.links").isEmpty());
    }

    /** Each link type becomes the address it names; an external one passes through. */
    @Test
    void everyLinkTypeBecomesAUrl() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].url").value("/"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[1].url").value("/experiences"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[1].links[0].url")
                        .value("/experiences/sunset-sail"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[2].url").value("/pages/about-us"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[3].url")
                        .value("https://example.com/blog"));
    }

    /**
     * <b>Internal links carry the locale prefix; an external one must not.</b>
     * Prefixing somebody else's domain would produce a path on ours.
     */
    @Test
    void aSecondaryLocalePrefixesInternalLinksOnly() throws Exception {
        served("en", globals("en", "es", List.of("es", "en")));

        mockMvc.perform(get("/en").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].url").value("/en"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[1].url").value("/en/experiences"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[2].url").value("/en/pages/about-us"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[3].url")
                        .value("https://example.com/blog"));
    }

    /**
     * {@code levels} is how deep the tree runs below a link — a theme uses it to
     * decide whether a link needs a dropdown at all. Derived, like aspectRatio.
     */
    @Test
    void aLinkKnowsHowDeepItGoes() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].levels").value(0))
                .andExpect(jsonPath("$.linklists['main-menu'].links[1].levels").value(1))
                .andExpect(jsonPath("$.linklists['main-menu'].links[1].links[0].levels").value(0));
    }

    /** No {@code handle}, and none of the request-dependent active/current family. */
    @Test
    void aLinkCarriesOnlyWhatWeHave() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].handle").doesNotExist())
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].active").doesNotExist())
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].current").doesNotExist())
                .andExpect(jsonPath("$.linklists['main-menu'].links[0].type").value("HOME"));
    }

    /**
     * Shopify's {@code address}, minus the customer-address fields a operator has no
     * use for. {@code street} is derived — their field, their composition — and
     * the country is nested with both a code and a name because the name is
     * English only and a client may want to localize it itself.
     */
    @Test
    void theShopCarriesAStructuredAddress() throws Exception {
        served(null, globals("es", "es", List.of("es")));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.address.address1").value("Calle Mayor 1"))
                .andExpect(jsonPath("$.tourOperator.address.street").value("Calle Mayor 1"))
                .andExpect(jsonPath("$.tourOperator.address.city").value("Palma"))
                .andExpect(jsonPath("$.tourOperator.address.province").value("Illes Balears"))
                .andExpect(jsonPath("$.tourOperator.address.zip").value("07001"))
                .andExpect(jsonPath("$.tourOperator.address.country.code").value("ES"))
                .andExpect(jsonPath("$.tourOperator.address.country.name").value("Spain"))
                // Not Shopify's, and deliberately absent: province_code needs ISO
                // 3166-2 data we do not carry, and summary is a theme's business.
                .andExpect(jsonPath("$.tourOperator.address.province_code").doesNotExist())
                .andExpect(jsonPath("$.tourOperator.address.summary").doesNotExist());
    }

    /**
     * An operator that predates the structured address has none. It serves
     * {@code null} rather than an object of nulls, the rule {@code Image}
     * follows, so a theme guards on the object.
     */
    @Test
    void anOperatorWithNoAddressServesNull() throws Exception {
        TourOperatorView operator = new TourOperatorView(OPERATOR, "Acme Tours", "acme", null, null, null,
                "Sailing day trips", "The best sailing in Mallorca", OG_IMAGE, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
        served(null, new StorefrontGlobals(operator, "Sailing day trips", "…", OG_IMAGE,
                List.of(), List.of(), List.of(),
                new LocalizationData("es", "es", List.of("es"))));

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                // Null, and specifically not an object whose parts are all null —
                // asserting a part is absent is what tells those two apart.
                .andExpect(jsonPath("$.tourOperator.address").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.tourOperator.address.city").doesNotExist());
    }
}
