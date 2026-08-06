package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.BrandData;
import com.vointika.storefront.application.dto.output.BrandData.ColorData;
import com.vointika.storefront.application.dto.output.BrandData.ColorsData;
import com.vointika.storefront.application.dto.output.BrandData.SocialLinkData;
import com.vointika.storefront.application.dto.output.ImageData;
import com.vointika.storefront.application.dto.output.LocalizationData;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
import com.vointika.storefront.application.dto.output.PageData;
import com.vointika.storefront.application.dto.output.PolicyData;
import com.vointika.storefront.application.dto.output.ShopData;
import com.vointika.storefront.application.dto.output.ShopData.CurrencyData;
import com.vointika.storefront.application.dto.output.ShopData.TimezoneData;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import com.vointika.storefront.application.policy.LocaleResolver;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.infrastructure.config.StorefrontMustacheConfig;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import com.vointika.storefront.infrastructure.web.StorefrontWebConfig;
import jakarta.servlet.http.Cookie;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No test here sends an {@code Authorization} header, and every one of them
 * expects a page — the storefront is public, and that is part of what this class
 * proves. The rest is that the compiler configured in
 * {@link StorefrontMustacheConfig} renders operator-authored text safely, and
 * that the gate registered by {@link StorefrontWebConfig} runs in front of the
 * locale rule rather than after it.
 *
 * <p>Since the home page <em>is</em> the global envelope, these assertions are
 * also the readable specification of what {@code shop}, {@code page},
 * {@code routes} and {@code localization} put on a page.
 */
@WebMvcTest(StorefrontHomeController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class,
        StorefrontWebConfig.class, ThemeContextDump.class})
class StorefrontHomeControllerTest {

    private static final UUID SHOP_ID = UUID.fromString("01900000-0000-7000-8000-000000000002");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetHomePageUseCase getHomePageUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void unlockedByDefault() {
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.UNLOCKED);
    }

    /**
     * The stubbed host has <em>no port</em> while the request's does: the
     * controller reads {@code getServerName()}, not the raw {@code Host} header,
     * and {@code MockHttpServletRequest} derives the former from the latter with
     * the port stripped. Read the raw header instead and this stub misses, the
     * mock returns empty, and the assertions below fail on a 404 — so this test
     * is the regression guard for that choice.
     */
    @Test
    void rendersTheShopForAKnownHost() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours - day trips", "Boat tours and day trips",
                "tour-operators/1/logo.png", "tour-operators/1/og.png")));
        when(mediaUrlResolver.toUrl("tour-operators/1/logo.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/logo.png");
        when(mediaUrlResolver.toUrl("tour-operators/1/og.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/og.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"es\">"),
                        containsString("<title>Acme Tours - day trips</title>"),
                        containsString("<h1>Acme Tours</h1>"),
                        containsString("Boat tours and day trips"),
                        containsString("<img src=\"http://localhost:9000/avatars/tour-operators/1/logo.png\""),
                        containsString("<meta property=\"og:type\" content=\"website\">"),
                        containsString("<meta property=\"og:image\" "
                                + "content=\"http://localhost:9000/avatars/tour-operators/1/og.png\">"))));
    }

    /**
     * The three things the object model added to every page: the logo is a link
     * home, the footer carries the operator's address, and the switcher lists
     * every locale the shop publishes with the current one marked rather than
     * linked.
     */
    @Test
    void theChromeCarriesTheHomeLinkTheAddressAndTheSwitcher() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours", null, "logo.png", null)));
        when(mediaUrlResolver.toUrl("logo.png")).thenReturn("http://localhost:9000/logo.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<a href=\"/\"><img src=\"http://localhost:9000/logo.png\" "
                                + "alt=\"Acme Tours\"></a>"),
                        containsString("<span lang=\"es\">es</span>"),
                        containsString("<a href=\"/en\" lang=\"en\">en</a>"),
                        containsString("<a href=\"/fr\" lang=\"fr\">fr</a>"),
                        containsString("<p>Calle Mayor 1, 28013 Madrid</p>"))));
    }

    /**
     * <b>The logo comes from {@code shop.brand.logo}</b> — V10 moved the column
     * onto the brand and {@code shop.logoUrl} is gone with it, because Shopify's
     * shop object has no logo of its own. Point the layout back at a
     * {@code shop.*} key and the {@code <img>} disappears silently: a missing
     * variable renders empty rather than throwing, which is the compiler's
     * {@code defaultValue("")} doing what it is there for.
     */
    @Test
    void theLogoRendersFromTheBrand() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours", null, "brand/logo.png", null)));
        when(mediaUrlResolver.toUrl("brand/logo.png")).thenReturn("http://localhost:9000/brand/logo.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<a href=\"/\"><img src=\"http://localhost:9000/brand/logo.png\" "
                                + "alt=\"Acme Tours\"></a>")));
    }

    /**
     * <b>An operator with no brand row at all</b> — every field null, both colour
     * lists empty. V10 backfilled a row per operator but nothing writes one at
     * creation, so this is a real state and not a hypothetical. It must emit no
     * empty {@code <img>} and no stray anchor, which is the section guard rather
     * than a null check anywhere in Java.
     */
    @Test
    void aBrandWithNothingSetEmitsNoMarkup() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(pageWithoutBrand()));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        not(containsString("<img")),
                        not(containsString("<a href=\"/\">")),
                        containsString("    <header>\n"
                                + "        <nav>"))));
    }

    /**
     * The contact details V9 added. They are linked rather than printed because a
     * phone number a visitor cannot tap is a worse storefront, and the scheme is a
     * literal prefix so an operator-authored value can never choose it.
     */
    @Test
    void theFooterLinksThePhoneAndTheEmail() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<a href=\"tel:+34 910 000 000\">+34 910 000 000</a>"),
                        containsString("<a href=\"mailto:hola@acme.test\">hola@acme.test</a>"))));
    }

    /**
     * <b>Both columns are nullable and nothing writes them yet, so this is the
     * case every real operator is in today.</b> It asserts the absent shape to the
     * byte: the footer collapses to the address alone, with no empty
     * {@code <a>} and — the part {@code containsString} could never see — no
     * blank lines where the two sections were. That is the hugging rule doing the
     * work; write the guards on their own lines and this is what breaks.
     */
    @Test
    void omitsTheContactLinesEntirelyWhenNeitherIsSet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null))
                .thenReturn(Optional.of(pageWithoutContactDetails("es", "Acme Tours")));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("tel:")),
                        not(containsString("mailto:")),
                        containsString("    <footer>\n"
                                + "        <p>Calle Mayor 1, 28013 Madrid</p>\n"
                                + "    </footer>\n</body>\n</html>"))));
    }

    /**
     * <b>The footer lists the policies that exist and nothing else.</b> A policy
     * has no draft state — the row's absence <em>is</em> the unpublished one — so
     * the list the port answers is exactly what to link, and the two the seeded
     * operator has not written must leave no markup behind at all.
     */
    @Test
    void theFooterLinksOnlyThePoliciesThatExist() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<a href=\"/policies/cancellation\">Cancellation policy</a>"),
                        containsString("<a href=\"/policies/legal-notice\">Legal notice</a>"),
                        not(containsString("/policies/privacy")),
                        not(containsString("/policies/terms")))));
    }

    /**
     * Under a locale prefix the logo goes home <em>in that locale</em> and the
     * switcher's own links do not move: each language always points at its own
     * address, and the primary's is the bare one.
     */
    @Test
    void theChromeFollowsTheLocaleThePathArrivedUnder() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(page(
                "en", "Acme Tours", null, "logo.png", null,
                new LanguageData("es", false, null),
                new LanguageData("en", true, "en"),
                new LanguageData("fr", false, "fr"))));
        when(mediaUrlResolver.toUrl("logo.png")).thenReturn("http://localhost:9000/logo.png");

        mockMvc.perform(get("/en").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<a href=\"/en\"><img"),
                        containsString("<a href=\"/\" lang=\"es\">es</a>"),
                        containsString("<span lang=\"en\">en</span>"),
                        containsString("<a href=\"/fr\" lang=\"fr\">fr</a>"),
                        // The footer's policy links carry the prefix too, and they get it
                        // from Routes rather than from a second copy of the rule.
                        containsString("<a href=\"/en/policies/cancellation\">"))));
    }

    /**
     * A published secondary is served under its own prefix, in its own language,
     * and says so in {@code lang} — the reason {@code localization.locale} reaches
     * the view at all.
     */
    @Test
    void rendersASecondaryLocaleUnderItsPrefix() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(page(
                "en", "Acme Tours - day trips", null, null, null)));

        mockMvc.perform(get("/en").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"en\">"),
                        containsString("<title>Acme Tours - day trips</title>"))));
    }

    /**
     * The primary locale and an unsupported one are the same answer, and the use
     * case decides which is which — this pins that the controller passes the path
     * locale through and renders the not-found page for an empty result.
     */
    @Test
    void answersNotFoundForALocaleThatAddressesNoPage() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "es")).thenReturn(Optional.empty());
        when(getHomePageUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/es").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
        mockMvc.perform(get("/de").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    /**
     * An operator who has filled in nothing optional still gets a page. Without
     * the compiler's {@code defaultValue("")} this is a 500, not a bare page.
     */
    @Test
    void omitsTheOptionalTagsWhenNothingIsSet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                page("es", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        not(containsString("og:image")),
                        not(containsString("<img")),
                        not(containsString("name=\"description\"")))));
    }

    /**
     * <b>The one assertion in this class that can see whitespace.</b> Every other
     * one is {@code containsString}, so none of them can observe what the layout
     * actually risks: whitespace between a block tag and its content is output
     * verbatim (STACK.md), and a reformat of {@code home.mustache} that looks
     * tidier ships blank lines into every page. Pinning the seam — where the
     * block's last line meets the layout's footer — is what catches that.
     *
     * <p>It pins the header seam for the same reason: the switcher is written as
     * one long line precisely because a readable one would ship its own
     * indentation into every rendered page.
     */
    @Test
    void theLayoutJoinsThePageWithoutLeakingWhitespace() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(page(
                "es", "Acme Tours", null, "logo.png", null)));
        when(mediaUrlResolver.toUrl("logo.png")).thenReturn("http://localhost:9000/logo.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        startsWith("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n"),
                        // The logo sits behind a standalone {{! }} comment explaining
                        // where it reads from. A standalone comment line is stripped
                        // whole; write it inline and the blank line ships on every page.
                        containsString("    <header>\n"
                                + "        <a href=\"/\"><img src=\"http://localhost:9000/logo.png\" "
                                + "alt=\"Acme Tours\"></a>\n"
                                + "        <nav>"),
                        containsString("</header>\n    <h1>Acme Tours</h1>\n    <footer>\n"
                                + "        <p>Calle Mayor 1, 28013 Madrid</p>\n"
                                + "        <p><a href=\"tel:+34 910 000 000\">+34 910 000 000</a></p>\n"
                                + "        <p><a href=\"mailto:hola@acme.test\">hola@acme.test</a></p>\n"
                                // The policy list is the fourth guard on this seam and the
                                // first that repeats: it opens at the end of the email line
                                // and closes hugging its own, so N policies are N lines and
                                // none is a blank one.
                                + "        <p><a href=\"/policies/cancellation\">Cancellation policy</a></p>\n"
                                + "        <p><a href=\"/policies/legal-notice\">Legal notice</a></p>\n"
                                + "    </footer>\n</body>\n</html>"))));
    }

    /**
     * HEAD has to be registered alongside GET in {@link StorefrontPublicRoutes}:
     * Spring MVC serves it from the {@code @GetMapping} for free, but Spring
     * Security matches on the exact method and would reject it at the filter
     * chain — a 401 in a JSON shape, on a public page. Drop either HEAD entry and
     * this fails on the status. Crawlers, link checkers and uptime monitors all
     * send HEAD, and the home slice shipped without noticing because every other
     * test and curl used GET.
     */
    /**
     * <b>The canonical address is the origin plus this page's path</b>, and both
     * halves are needed: {@code shop.url} alone cannot say which page you are on,
     * and {@code routes} says where each page lives rather than which one is
     * being rendered. Mustache concatenates them by juxtaposition.
     *
     * <p>It matters most on a multi-locale site, where {@code /} and {@code /en}
     * serve near-identical pages — without a canonical, they compete.
     */
    @Test
    void theCanonicalAndOgUrlAreAbsoluteAndPerLocale() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                page("es", "Acme Tours", null, null, null)));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(
                page("en", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(content().string(allOf(
                        containsString("<link rel=\"canonical\" href=\"http://acme.localhost/\">"),
                        containsString("<meta property=\"og:url\" content=\"http://acme.localhost/\">"))));

        mockMvc.perform(get("/en").header("Host", "acme.localhost"))
                .andExpect(content().string(allOf(
                        containsString("<link rel=\"canonical\" href=\"http://acme.localhost/en\">"),
                        containsString("<meta property=\"og:url\" content=\"http://acme.localhost/en\">"))));
    }

    /**
     * The default. {@code ?format=json} is not a parameter unless
     * {@code app.storefront.context-endpoint} says so, so a storefront serves a
     * page and nothing else — including the fields the envelope carries with no
     * renderer, which is most of {@code shop}.
     */
    @Test
    void theContextDumpIsOffSoTheParameterStillRendersThePage() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                page("es", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/?format=json").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<h1>Acme Tours</h1>")));
    }

    @Test
    void servesHeadAsWellAsGet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                page("es", "Acme Tours", null, null, null)));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(
                page("en", "Acme Tours", null, null, null)));

        mockMvc.perform(head("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(head("/en").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    /**
     * <b>The locale route is a security pattern as well as a route.</b> It is
     * registered verbatim as a {@code PublicRoute}, so a bare {@code /{locale}}
     * would {@code permitAll} every single-segment path in the application —
     * {@code /error} and {@code /favicon.ico} today, and whatever {@code /health}
     * or {@code /metrics} someone adds later, silently and with nothing failing.
     * Loosen {@link LocaleResolver#PATH_TEMPLATE} to {@code /&#123;locale&#125;}
     * and these stop being 401.
     */
    @Test
    void theLocaleRouteDoesNotOpenEveryOtherSingleSegmentPath() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.of("acme"));

        for (String path : new String[]{"/error", "/favicon.ico", "/health", "/metrics", "/api"}) {
            mockMvc.perform(get(path).header("Host", "acme.localhost"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /** A locale-shaped path still routes, including the regional form the template leaves room for. */
    @Test
    void theConstrainedTemplateStillMatchesARealLocale() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "pt-br")).thenReturn(Optional.of(
                page("pt-br", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/pt-br").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"pt-br\">")));
    }

    /**
     * The gate covers the storefront's own pages, not the container's error
     * dispatch: a genuine 500 on a locked store has to stay a 500 rather than
     * being rewritten into a redirect to {@code /password}. Widen the
     * interceptor's patterns back to {@code /*} and this becomes a 302.
     */
    @Test
    void aLockedStoreDoesNotRedirectTheContainersErrorDispatch() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/error").header("Host", "acme.localhost"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersNotFoundForAnUnknownHandle() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.of("nope"));
        when(getHomePageUseCase.execute("nope", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "nope.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void answersNotFoundWhenTheHostResolvesToNoTenant() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    /**
     * <b>The contact details are the first operator-authored values to land in an
     * attribute rather than in text</b>, and the escaper is HTML-only — it does
     * not make an {@code href} safe by itself. What does is that {@code tel:} and
     * {@code mailto:} are literal prefixes, so the value can only ever be a
     * suffix and never chooses the scheme, and that the seven-pair escape turns
     * the quote that would close the attribute into {@code &#38;quot;}. Both halves
     * are asserted here: nothing breaks out, and no event handler appears.
     */
    @Test
    void escapesOperatorAuthoredText() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(new StorefrontPageData(
                new ShopData(SHOP_ID, "<script>alert(1)</script>", "<img onerror=x>",
                        "\"><script>alert(1)</script>", "x\" onmouseover=\"alert(1)",
                        "A shop description",
                        "Opening soon — ask us for the password.",
                        noBrand(), List.of(),
                        new CurrencyData("EUR", "€"),
                        new TimezoneData("Europe/Madrid", "Madrid")),
                new PageData("<script>alert(1)</script>", null, null),
                new LocalizationData("es", List.of(new LanguageData("es", true, null))))));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("<script>")),
                        not(containsString("<img onerror")),
                        not(containsString("onmouseover=\"")),
                        containsString("href=\"tel:&quot;&gt;&lt;script&gt;"),
                        containsString("&lt;script&gt;"))));
    }

    @Test
    void aLockedStoreRedirectsToThePasswordPage() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * <b>The ordering test.</b> A locked store asked for a locale it does not
     * publish must redirect, not 404 — resolve the locale first and {@code /es}
     * would 404 while {@code /fr} redirected, which tells an anonymous visitor
     * that the store exists and which locales it has, from in front of the gate.
     * Move the gate after locale resolution and this is the test that goes red.
     */
    @Test
    void aLockedStoreRedirectsEvenForALocaleItDoesNotPublish() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getHomePageUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * The interceptor reads the unlock cookie by name and hands its value to the
     * gate. Rename the cookie on either side and the gate stops seeing it: the
     * value below arrives as null and the request redirects instead of rendering.
     */
    @Test
    void aValidUnlockCookieGetsThroughTheGate() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(checkStorefrontLockUseCase.execute("acme", "a-valid-token")).thenReturn(LockState.UNLOCKED);
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                page("es", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/")
                        .header("Host", "acme.localhost")
                        .cookie(new Cookie(UnlockTokenPort.COOKIE_NAME, "a-valid-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Acme Tours</h1>")));
    }

    /** A host that addresses no tenant never reaches the gate, so it 404s rather than redirecting. */
    @Test
    void theGateIsNotConsultedForAHostThatAddressesNoTenant() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound());
        verify(checkStorefrontLockUseCase, never()).execute(any(), any());
    }

    /** The shop this test class renders: `es` primary, `en` and `fr` published beside it. */
    private static StorefrontPageData page(String locale, String title, String description,
                                           String logoKey, String ogImageKey, LanguageData... languages) {
        List<LanguageData> switcher = languages.length > 0 ? List.of(languages) : List.of(
                new LanguageData("es", true, null),
                new LanguageData("en", false, "en"),
                new LanguageData("fr", false, "fr"));
        return new StorefrontPageData(
                new ShopData(SHOP_ID, "Acme Tours", "Calle Mayor 1, 28013 Madrid",
                        "+34 910 000 000", "hola@acme.test",
                        "A shop description",
                        "Opening soon — ask us for the password.",
                        brand(logoKey), policies(),
                        new CurrencyData("EUR", "€"),
                        new TimezoneData("Europe/Madrid", "Madrid")),
                new PageData(title, description, ogImageKey),
                new LocalizationData(locale, switcher));
    }

    /** An operator with no brand row — the state anything created since V10 is in. */
    private static StorefrontPageData pageWithoutBrand() {
        return new StorefrontPageData(
                new ShopData(SHOP_ID, "Acme Tours", "Calle Mayor 1, 28013 Madrid", null, null,
                        null, null, noBrand(), List.of(),
                        new CurrencyData("EUR", "€"),
                        new TimezoneData("Europe/Madrid", "Madrid")),
                new PageData("Acme Tours", null, null),
                new LocalizationData("es", List.of(new LanguageData("es", true, null))));
    }

    /** An operator who has published neither contact detail — both columns are nullable. */
    private static StorefrontPageData pageWithoutContactDetails(String locale, String title) {
        return new StorefrontPageData(
                new ShopData(SHOP_ID, "Acme Tours", "Calle Mayor 1, 28013 Madrid", null, null,
                        "A shop description",
                        "Opening soon — ask us for the password.",
                        brand(null), List.of(),
                        new CurrencyData("EUR", "€"),
                        new TimezoneData("Europe/Madrid", "Madrid")),
                new PageData(title, null, null),
                new LocalizationData(locale, List.of(new LanguageData("es", true, null))));
    }

    /**
     * The brand these pages render through. <b>The logo is
     * {@code shop.brand.logo}</b> since V10 — {@code shop.logoUrl} is gone,
     * because Shopify's shop object never had a logo of its own.
     *
     * <p>{@code alt} and the dimensions are non-null here and null on every real
     * row: the media columns exist and nothing populates them yet, which is why
     * the layout writes the shop's name into the {@code alt} the way Dawn does.
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

    /**
     * <b>Two of the four</b>, which is the case that matters: a policy has no
     * draft state, so the rows that exist are the links, and an operator who has
     * written two must get two.
     */
    private static List<PolicyData> policies() {
        return List.of(
                new PolicyData("CANCELLATION", "Cancellation policy", "cancellation"),
                new PolicyData("LEGAL_NOTICE", "Legal notice", "legal-notice"));
    }

    /** An operator with no brand row at all — everything null, both lists empty. */
    private static BrandData noBrand() {
        return new BrandData(null, null, new ColorsData(List.of(), List.of()),
                null, null, null, null, List.of());
    }
}
