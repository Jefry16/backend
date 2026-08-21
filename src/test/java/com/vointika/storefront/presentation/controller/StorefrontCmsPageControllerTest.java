package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.LocalizedHandles;
import com.vointika.shared.port.StorefrontPageQuery.PageView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.dto.output.StorefrontPageOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetStorefrontCmsPageUseCase;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /pages/&#123;handle&#125;} — the second route with an object of its own,
 * and the first address a menu item has been able to link to.
 *
 * <p>Importing {@link StorefrontPublicRoutes} is what proves the route is public;
 * omit it and every request 401s, which is exactly what these addresses did
 * before this slice.
 */
@WebMvcTest(StorefrontCmsPageController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontCmsPageControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID PAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e11");
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001",
            "ES", "Spain");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontCmsPageUseCase getStorefrontCmsPageUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any())).thenReturn(Map.of());
        when(mediaUrlResolver.toUrl(anyString())).thenAnswer(call -> "https://media.test/" + call.getArgument(0));
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.UNLOCKED);
    }

    private static StorefrontPageOutput output(String current, String primary, String handle, String title) {
        TourOperatorView operator = new TourOperatorView(OPERATOR, "Acme Tours", "acme", ADDRESS, null, null,
                "Sailing day trips", "The best sailing in Mallorca", null, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
        StorefrontGlobals globals = new StorefrontGlobals(operator, title, "About this operator", null,
                List.of(), List.of(), List.of(),
                // The served locale has to be one the operator publishes. It was
                // List.of(primary) — so every localized case here declared a
                // locale it did not support, which orElse(null) quietly absorbed
                // and the switcher's orElseThrow now refuses.
                new LocalizationData(current, primary,
                        current.equals(primary) ? List.of(primary) : List.of(primary, current)));
        return new StorefrontPageOutput(globals,
                new PageView(PAGE, handle, title, "<p>Since 2011.</p>", null, null,
                        new LocalizedHandles(handle, Map.of())));
    }

    private void served(String pathLocale, String handle, StorefrontPageOutput output) {
        when(getStorefrontCmsPageUseCase.execute("acme", pathLocale, handle))
                .thenReturn(Optional.of(output));
    }

    /** The globals are still all there — a page route is globals plus one object. */
    @Test
    void aPageIsTheGlobalsPlusThePage() throws Exception {
        served(null, "about-us", output("es", "es", "about-us", "About us"));

        mockMvc.perform(get("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.localization.language.code").value("es"))
                .andExpect(jsonPath("$.page.id").value(PAGE.toString()))
                .andExpect(jsonPath("$.page.handle").value("about-us"))
                .andExpect(jsonPath("$.page.title").value("About us"))
                .andExpect(jsonPath("$.page.body").value("<p>Since 2011.</p>"))
                .andExpect(jsonPath("$.page.url").value("/pages/about-us"))
                // The listing's object belongs to the listing — a page gets the
                // globals plus its own object, and no other route's.
                .andExpect(jsonPath("$.experiences").doesNotExist())
                .andExpect(jsonPath("$.experience").doesNotExist());
    }

    /**
     * The page's own title becomes the title tag. {@code pageTitle} is where that
     * lives — {@code page} is the CMS object, exactly as in Shopify.
     */
    @Test
    void thePagesTitleBecomesThePageTitle() throws Exception {
        served(null, "about-us", output("es", "es", "about-us", "About us"));

        mockMvc.perform(get("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.pageTitle").value("About us"));
    }

    /**
     * <b>The switcher offers each language its own slug, not this one under a
     * different prefix.</b> That mistake — English prefix, Spanish slug — is a
     * 404, because a locale that renames a page makes the canonical a 404 there.
     * It was the shape of the bug for as long as every url was `/` or `/{code}`.
     */
    @Test
    void theSwitcherOffersEachLanguageItsOwnHandle() throws Exception {
        StorefrontPageOutput out = output("en", "en", "about-us", "About us");
        StorefrontPageOutput renamed = new StorefrontPageOutput(
                new StorefrontGlobals(out.globals().tourOperator(), "About us", "About this operator", null,
                        List.of(), List.of(), List.of(),
                        new LocalizationData("en", "en", List.of("en", "es"))),
                new PageView(PAGE, "about-us", "About us", "<p>Since 2011.</p>", null, null,
                        new LocalizedHandles("about-us", Map.of("es", "sobre-nosotros"))));
        served(null, "about-us", renamed);

        mockMvc.perform(get("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.localization.languages[0].url").value("/pages/about-us"))
                .andExpect(jsonPath("$.localization.languages[1].url").value("/es/pages/sobre-nosotros"))
                // The current language's entry and the canonical are one string.
                .andExpect(jsonPath("$.localization.language.url").value("/pages/about-us"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/pages/about-us"));
    }

    /** A localized address carries its prefix into the page's own url. */
    @Test
    void aLocalizedPageCarriesThePrefix() throws Exception {
        served("en", "about-us", output("en", "es", "about-us", "About us"));

        mockMvc.perform(get("/en/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.url").value("/en/pages/about-us"));
    }

    /**
     * The page's own address, absolute — and {@code pageType} is Shopify's name
     * for this template, not a value inferred from "a page object is present".
     * Each route names its own, so a later route whose object is absent does not
     * silently become the index.
     */
    @Test
    void aPageCarriesItsOwnCanonicalUrlAndPageType() throws Exception {
        served(null, "about-us", output("es", "es", "about-us", "Sobre nosotros"));

        mockMvc.perform(get("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/pages/about-us"))
                .andExpect(jsonPath("$.pageType").value("page"));
    }

    /** And it follows the locale prefix, like every other URL the page hands out. */
    @Test
    void aLocalizedPagesCanonicalUrlCarriesThePrefix() throws Exception {
        served("en", "about-us", output("en", "es", "about-us", "About us"));

        mockMvc.perform(get("/en/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/en/pages/about-us"));
    }

    /**
     * <b>The gate covers this route too, and nothing said so until now.</b> The
     * lock interceptor is a pattern allowlist; a page route missing from it is
     * served ungated, so a store the operator locked hands its CMS pages to
     * anonymous visitors. The page still answers, which is why no other test
     * noticed.
     *
     * <p>The handle is deliberately one the page query would answer <b>empty</b>
     * for: a 302 here proves nothing downstream of the gate ran, the same shape
     * {@code aLockedStoreRedirectsEvenForALocaleItDoesNotPublish} uses.
     */
    @Test
    void aLockedStoreRedirectsACmsPageBeforeLookingItUp() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getStorefrontCmsPageUseCase.execute("acme", null, "about-us"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /** Every miss is the same 404, so none of them says which kind it was. */
    @Test
    void anUnknownHandleIs404() throws Exception {
        when(getStorefrontCmsPageUseCase.execute("acme", null, "nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/pages/nope").header("Host", "acme.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    @Test
    void aHostNoOperatorOwnsIs404() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/pages/about-us").header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound());
    }

    /** A page route is a public page, and crawlers HEAD it. */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null, "about-us", output("es", "es", "about-us", "About us"));

        mockMvc.perform(head("/pages/about-us").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    /**
     * The handle variable is constrained to the {@code Handle} shape, so a
     * segment that is not handle-shaped is not this route at all — 401 is the
     * pass condition, the same way {@code /error} is for the locale pattern.
     */
    @Test
    void aSegmentThatIsNotHandleShapedIsNotAPageRoute() throws Exception {
        mockMvc.perform(get("/pages/Not_A_Handle").header("Host", "acme.localhost:8080"))
                .andExpect(status().isUnauthorized());
    }
}
