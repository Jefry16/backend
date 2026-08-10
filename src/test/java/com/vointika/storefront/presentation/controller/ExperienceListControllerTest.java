package com.vointika.storefront.presentation.controller;

import java.math.BigDecimal;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.BrandData;
import com.vointika.storefront.application.dto.output.BrandData.ColorData;
import com.vointika.storefront.application.dto.output.BrandData.ColorsData;
import com.vointika.storefront.application.dto.output.BrandData.SocialLinkData;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput.ExperienceCard;
import com.vointika.storefront.application.dto.output.ImageData;
import com.vointika.storefront.application.dto.output.LocalizationData;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
import com.vointika.storefront.application.dto.output.PageData;
import com.vointika.storefront.application.dto.output.ShopData;
import com.vointika.storefront.application.dto.output.ShopData.CurrencyData;
import com.vointika.storefront.application.dto.output.ShopData.TimezoneData;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetExperienceListPageUseCase;
import com.vointika.storefront.infrastructure.config.StorefrontMustacheConfig;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import com.vointika.storefront.infrastructure.web.StorefrontWebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The storefront's second page, and the first that shares its chrome with
 * another one: everything above {@code <h1>} comes from {@code storefront/layout}
 * through template inheritance, so these assertions cover the layout as much as
 * the listing.
 *
 * <p>As on the home page, no test here sends an {@code Authorization} header —
 * both routes are public, and each of them has to be registered twice over, once
 * per method, in {@code StorefrontPublicRoutes} and once in the gate's patterns.
 */
@WebMvcTest(ExperienceListController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class,
        StorefrontWebConfig.class, ThemeContextDump.class})
class ExperienceListControllerTest {

    private static final UUID SHOP_ID = UUID.fromString("01900000-0000-7000-8000-000000000002");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetExperienceListPageUseCase getExperienceListPageUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void unlockedByDefault() {
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.UNLOCKED);
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
    }

    @Test
    void rendersACardPerPublishedExperience() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es",
                new ExperienceCard("sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise",
                        "tour-operators/1/sunset.jpg", 150, new BigDecimal("35.00")),
                new ExperienceCard("kayak-cave-adventure", "Kayak Cave Adventure", "Sea caves", null, 120, BigDecimal.ZERO))));
        when(mediaUrlResolver.toUrl("tour-operators/1/sunset.jpg"))
                .thenReturn("http://localhost:9000/media/tour-operators/1/sunset.jpg");

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"es\">"),
                        containsString("<title>Acme Tours</title>"),
                        containsString("<h2>Sunset Sailing Tour</h2>"),
                        containsString("<a href=\"/experiences/sunset-sailing-tour\">"),
                        containsString("<img src=\"http://localhost:9000/media/tour-operators/1/sunset.jpg\" "
                                + "alt=\"Sunset Sailing Tour\">"),
                        containsString("<p>Golden-hour cruise</p>"),
                        containsString("<p>150 min</p>"),
                        containsString("<a href=\"/experiences/kayak-cave-adventure\">"))));
    }

    /**
     * The price is the operator's claim, and 0 means they have not made one — so
     * the badge is absent rather than reading "From 0". Both halves are asserted
     * here because only the pair pins the rule: the sunset sail is priced at
     * 35.00 and the kayak trip is not, in the same render.
     *
     * <p>The symbol comes from {@code shop.currency}, which until now had no
     * reader anywhere.
     */
    @Test
    void aPricedExperienceShowsAFromBadgeAndAnUnpricedOneShowsNone() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es",
                new ExperienceCard("sunset-sailing-tour", "Sunset Sailing Tour", "Golden-hour cruise",
                        null, 150, new BigDecimal("35.00")),
                new ExperienceCard("kayak-cave-adventure", "Kayak Cave Adventure", "Sea caves",
                        null, 120, BigDecimal.ZERO))));

        String html = mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("<p>From \u20ac35.00</p>");
        assertThat(html).doesNotContain("From \u20ac0");
        assertThat(html).doesNotContain("From \u20ac0.00");
        // one badge for two cards
        assertThat(html.split("From ", -1)).hasSize(2);
    }

    /**
     * The chrome is the layout's and therefore identical on both pages — except
     * for the switcher, whose links are "this page in that language" and so point
     * at the listing rather than at the home page. That difference is the whole
     * reason {@link com.vointika.storefront.presentation.view.Localization} takes
     * a route rather than building one.
     */
    @Test
    void theSwitcherLinksToThisPageInEachLanguage() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page("es")));

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<span lang=\"es\">es</span>"),
                        containsString("<a href=\"/en/experiences\" lang=\"en\">en</a>"),
                        containsString("<a href=\"/fr/experiences\" lang=\"fr\">fr</a>"),
                        containsString("<p>Calle Mayor 1, 28013 Madrid</p>"))));
    }

    /**
     * The localized route renders the locale's own text and links into that
     * locale — including the translation's own handle, which is the reader the
     * localized handle would otherwise not have. The logo goes home in that
     * locale too.
     */
    @Test
    void theLocalizedRouteRendersTheLocalesNamesAndPrefixedLinks() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", "es")).thenReturn(Optional.of(page(
                "es", new ExperienceCard("paseo-en-velero", "Paseo en velero", "Crucero dorado", null, 150, new BigDecimal("35.00")))));
        when(mediaUrlResolver.toUrl("logo.png")).thenReturn("http://localhost:9000/logo.png");

        mockMvc.perform(get("/es/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"es\">"),
                        containsString("<h2>Paseo en velero</h2>"),
                        containsString("<a href=\"/es/experiences/paseo-en-velero\">"),
                        containsString("<a href=\"/es\"><img"))));
    }

    /** An operator with nothing published still has a storefront: a page, with no cards. */
    /**
     * <b>This is where {@code page.path} stops being the same thing as
     * {@code routes.root}.</b> On the home page the two coincide — {@code /} and
     * {@code /} — so a home-page assertion cannot tell them apart, and swapping
     * one for the other would leave every canonical on the listing pointing at
     * the shop's front door instead of at the listing. That is worse than no
     * canonical: it tells a crawler this page is a duplicate of another one.
     */
    @Test
    void theCanonicalIsThisPageRatherThanTheLocaleRoot() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page("es")));
        when(getExperienceListPageUseCase.execute("acme", "en")).thenReturn(Optional.of(page("en")));

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(content().string(allOf(
                        containsString("<link rel=\"canonical\" href=\"http://acme.localhost/experiences\">"),
                        containsString("<meta property=\"og:url\" content=\"http://acme.localhost/experiences\">"))));

        mockMvc.perform(get("/en/experiences").header("Host", "acme.localhost"))
                .andExpect(content().string(
                        containsString("<link rel=\"canonical\" href=\"http://acme.localhost/en/experiences\">")));
    }

    @Test
    void anEmptyListIsAPageWithNoCards() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null))
                .thenReturn(Optional.of(page("es")));

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        containsString("<ul>"),
                        not(containsString("<li>")))));
    }

    /**
     * HEAD is registered per route <em>and</em> per method: Spring MVC serves it
     * from the {@code @GetMapping} for free, Spring Security does not. Drop either
     * new HEAD entry and this fails on a 401 — the shape crawlers, link checkers
     * and uptime monitors would have found in production.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page("es")));
        when(getExperienceListPageUseCase.execute("acme", "en")).thenReturn(Optional.of(page("en")));

        mockMvc.perform(head("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(head("/en/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    /**
     * Both new routes are the gate's business, and nothing fails if one is left
     * out of {@code StorefrontWebConfig} — a locked store would simply serve it.
     * Drop either pattern and one of these two answers 200.
     */
    @Test
    void aLockedStoreRedirectsFromBothRoutes() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page("es")));
        when(getExperienceListPageUseCase.execute("acme", "en")).thenReturn(Optional.of(page("en")));

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
        mockMvc.perform(get("/en/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /** A locale the operator does not publish is no page here either. */
    @Test
    void answersNotFoundForALocaleThatAddressesNoPage() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void answersNotFoundWhenTheHostResolvesToNoTenant() throws Exception {
        when(tenantHandleResolver.resolve("localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/experiences").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void escapesOperatorAuthoredText() throws Exception {
        when(getExperienceListPageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", new ExperienceCard("x", "<script>alert(1)</script>", "<img onerror=x>", null, 60,
                        BigDecimal.ZERO))));

        mockMvc.perform(get("/experiences").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("<script>")),
                        not(containsString("<img onerror")),
                        containsString("&lt;script&gt;"))));
    }

    private static ExperienceListPageOutput page(String locale, ExperienceCard... cards) {
        return new ExperienceListPageOutput(
                new StorefrontPageData(
                        new ShopData(SHOP_ID, "Acme Tours", "Calle Mayor 1, 28013 Madrid",
                                "+34 910 000 000", "hola@acme.test",
                                "A shop description",
                        "Opening soon — ask us for the password.",
                        brand("logo.png"), List.of(),
                        new CurrencyData("EUR", "€"),
                        new TimezoneData("Europe/Madrid", "Madrid")),
                        new PageData("Acme Tours", null, null),
                        new LocalizationData(locale, List.of(
                                new LanguageData("es", "es".equals(locale), null),
                                new LanguageData("en", "en".equals(locale), "en"),
                                new LanguageData("fr", "fr".equals(locale), "fr")))),
                List.of(cards));
    }

    /**
     * The brand every page in this class renders through. The logo is
     * {@code shop.brand.logo} since V10 — {@code shop.logoUrl} is gone, because
     * Shopify's shop object never had one.
     *
     * <p>{@code alt} and the dimensions are non-null here and null on every real
     * row: the media columns exist and nothing populates them yet.
     */
    private static BrandData brand(String logoKey) {
        return new BrandData(
                "Sail the coast, not the crowds.",
                "Small-group sailing since 2011.",
                new ColorsData(
                        List.of(new ColorData("#0b3d5c", "#ffffff"), new ColorData("#1c7ba8", "#ffffff")),
                        List.of(new ColorData("#f2a541", "#1a1a1a"))),
                logoKey == null ? null : new ImageData(logoKey, "The Acme burgee", 400, 200),
                null, null, null,
                List.of(new SocialLinkData("INSTAGRAM", "https://instagram.com/acmetours")));
    }
}
