package com.vointika.rendering.presentation.controller;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;
import com.vointika.rendering.application.dto.output.ExperienceRenderContext;
import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.usecase.GetExperienceListRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetExperienceRenderContextUseCase;
import com.vointika.rendering.application.dto.output.PageRenderContext;
import com.vointika.rendering.application.usecase.GetPageRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetShopRenderContextUseCase;
import com.vointika.rendering.application.usecase.VerifyStorefrontPasswordUseCase;
import com.vointika.rendering.infrastructure.security.RenderingPublicRoutes;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.StorefrontExperienceView;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.port.StorefrontPageView;
import com.vointika.shared.web.security.InternalApiSecretFilter;
import com.vointika.shared.web.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The shared secret is pinned here rather than inherited from the environment:
// `app.internal.shared-secret` has no default in application.yml (it is supplied
// by APP_INTERNAL_SHARED_SECRET at runtime), and a test that authenticates
// against it must not depend on whoever's shell is running the suite.
@WebMvcTest(controllers = {RenderContextController.class, StorefrontController.class},
        properties = "app.internal.shared-secret=test-internal-secret")
@ExtendWith(RestDocumentationExtension.class)
// RenderingPublicRoutes is imported deliberately, not incidentally: without it
// the chain's anyRequest().authenticated() rejects these paths with 401 even
// when the shared secret is correct, because the secret filter authenticates
// the caller without populating a SecurityContext. The registrar is what makes
// the internal surface reachable at all — this test would pass vacuously
// (401 everywhere) if it were left out.
@Import({SecurityConfig.class, RenderingPublicRoutes.class})
class RenderContextControllerDocumentationTest {

    private static final String SLUG = "acme";
    private static final String SECRET = "test-internal-secret";

    private MockMvc mockMvc;

    @MockitoBean private GetShopRenderContextUseCase getShopUseCase;
    @MockitoBean private GetExperienceListRenderContextUseCase getExperienceListUseCase;
    @MockitoBean private GetExperienceRenderContextUseCase getExperienceUseCase;
    @MockitoBean private GetPageRenderContextUseCase getPageUseCase;
    @MockitoBean private VerifyStorefrontPasswordUseCase verifyPasswordUseCase;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .apply(springSecurity())
                .build();
    }

    private StorefrontOperatorView operatorView() {
        return new StorefrontOperatorView(
                java.util.UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2"),
                "Acme Tours",
                SLUG,
                "https://media.staging.vointika.com/logo.png",
                "en",
                List.of("en", "es"),
                "USD",
                "America/Santo_Domingo",
                false,
                null);
    }

    private ShopRenderContext shopContext() {
        return new ShopRenderContext(operatorView(), "en", List.of());
    }

    private StorefrontExperienceView experienceView() {
        return new StorefrontExperienceView(
                "morning-dive",
                "Morning dive",
                "A guided reef dive",
                "A longer description of the dive.",
                List.of("Small group"),
                List.of("Gear"),
                List.of("Lunch"),
                List.of("diving"),
                "https://media.staging.vointika.com/thumb.jpg",
                List.of("https://media.staging.vointika.com/thumb.jpg"),
                90,
                true,
                "morning-dive",
                java.util.Map.of("es", "buceo-matutino"));
    }

    @Test
    void getExperienceListRenderContext() throws Exception {
        when(getExperienceListUseCase.execute(eq(SLUG), isNull(), isNull())).thenReturn(
                new ExperienceListRenderContext(operatorView(), "en",
                        new CursorPage<>(List.of(experienceView()), null), List.of()));

        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/experience-list", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiences[0].slug").value("morning-dive"))
                .andDo(document("rendering-experience-list-render-context",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        responseFields(
                                subsectionWithPath("shop")
                                        .description("The tenant block, identical on every render context"),
                                fieldWithPath("request.locale")
                                        .description("The locale this render actually uses, after fallback"),
                                subsectionWithPath("navigation")
                                        .description("The operator's menus, resolved for this locale — chrome, so "
                                                + "present on every render context. Items carry `linkType` + "
                                                + "`handle`; the BFF turns those into paths, and an item whose "
                                                + "target is not published is already absent"),
                                fieldWithPath("experiences[]")
                                        .description("Published experiences, newest first; empty when none"),
                                fieldWithPath("experiences[].slug")
                                        .description("The handle for this locale — localized when the operator set one"),
                                fieldWithPath("experiences[].name").description("Translated name"),
                                fieldWithPath("experiences[].description").description("Translated short description").optional(),
                                fieldWithPath("experiences[].longDescription").description("Translated long description").optional(),
                                fieldWithPath("experiences[].highlights").description("Translated highlights"),
                                fieldWithPath("experiences[].included").description("Translated inclusions"),
                                fieldWithPath("experiences[].notIncluded").description("Translated exclusions"),
                                fieldWithPath("experiences[].tags").description("Facets — deliberately not translated"),
                                fieldWithPath("experiences[].thumbnailUrl").description("Resolved thumbnail URL").optional(),
                                fieldWithPath("experiences[].mediaUrls").description("Resolved gallery URLs, in order"),
                                fieldWithPath("experiences[].durationMinutes").description("Duration in minutes"),
                                fieldWithPath("experiences[].featured").description("Whether the operator features it"),
                                fieldWithPath("experiences[].canonicalSlug")
                                        .description("The original handle, addressable in every locale"),
                                subsectionWithPath("experiences[].handles")
                                        .description("Locale → localized handle, for the locales that have one"),
                                fieldWithPath("nextCursor")
                                        .description("Cursor for the next page, or null on the last")
                                        .optional())));
    }

    @Test
    void getExperienceRenderContext() throws Exception {
        when(getExperienceUseCase.execute(eq(SLUG), eq("morning-dive"), isNull())).thenReturn(
                new ExperienceRenderContext(operatorView(), "en", experienceView(), List.of()));

        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/experience/{experienceSlug}",
                                SLUG, "morning-dive")
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience.name").value("Morning dive"))
                .andDo(document("rendering-experience-render-context",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        responseFields(
                                subsectionWithPath("shop")
                                        .description("The tenant block, identical on every render context"),
                                fieldWithPath("request.locale").description("The locale this render actually uses"),
                                subsectionWithPath("navigation")
                                        .description("The operator's menus, resolved for this locale — chrome, so "
                                                + "present on every render context. Items carry `linkType` + "
                                                + "`handle`; the BFF turns those into paths, and an item whose "
                                                + "target is not published is already absent"),
                                subsectionWithPath("experience")
                                        .description("The experience, resolved for this locale — same shape as "
                                                + "an entry in the experience-list context"))));
    }

    @Test
    void getPageRenderContext() throws Exception {
        when(getPageUseCase.execute(eq(SLUG), eq("about-us"), isNull())).thenReturn(
                new PageRenderContext(operatorView(), "en", new StorefrontPageView(
                        "about-us",
                        "About us",
                        "<p>We run boats out of the old port.</p>",
                        "About | Acme Tours",
                        "Meet the crew behind Acme Tours",
                        null,
                        "about-us",
                        java.util.Map.of("es", "sobre-nosotros")), List.of()));

        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/page/{pageHandle}",
                                SLUG, "about-us")
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.title").value("About us"))
                .andDo(document("rendering-page-render-context",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        responseFields(
                                subsectionWithPath("shop")
                                        .description("The tenant block, identical on every render context"),
                                fieldWithPath("request.locale").description("The locale this render actually uses"),
                                subsectionWithPath("navigation")
                                        .description("The operator's menus, resolved for this locale — chrome, so "
                                                + "present on every render context. Items carry `linkType` + "
                                                + "`handle`; the BFF turns those into paths, and an item whose "
                                                + "target is not published is already absent"),
                                fieldWithPath("page.handle")
                                        .description("The handle for this locale — localized when the operator set one"),
                                fieldWithPath("page.title").description("Translated title"),
                                fieldWithPath("page.body")
                                        .description("Operator-authored raw HTML — the one value a theme marks `| raw`"),
                                fieldWithPath("page.seoTitle").description("SEO title override").optional(),
                                fieldWithPath("page.seoDescription").description("SEO description override").optional(),
                                fieldWithPath("page.templateSuffix")
                                        .description("Theme template variant, never translated").optional(),
                                fieldWithPath("page.canonicalHandle")
                                        .description("The original handle, addressable in every locale"),
                                subsectionWithPath("page.handles")
                                        .description("Locale → localized handle, for the locales that have one"))));
    }

    @Test
    void experienceListRejectsACallWithoutTheSharedSecret() throws Exception {
        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/experience-list", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShopRenderContext() throws Exception {
        when(getShopUseCase.execute(eq(SLUG), isNull())).thenReturn(shopContext());

        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/shop", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.slug").value(SLUG))
                .andExpect(jsonPath("$.request.locale").value("en"))
                .andDo(document("rendering-shop-render-context",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        responseFields(
                                fieldWithPath("shop.name").description("The operator's display name"),
                                fieldWithPath("shop.slug").description("The storefront slug (its subdomain label)"),
                                fieldWithPath("shop.logoUrl").description("Resolved logo URL, or null when unset")
                                        .optional(),
                                fieldWithPath("shop.primaryLocale")
                                        .description("The locale served on the bare, prefix-less path"),
                                fieldWithPath("shop.supportedLocales")
                                        .description("Published content locales, primary first then alphabetical"),
                                fieldWithPath("shop.currency").description("ISO currency code for money formatting"),
                                fieldWithPath("shop.timezone").description("IANA timezone for date formatting"),
                                fieldWithPath("shop.passwordEnabled")
                                        .description("Whether the storefront password gate is on"),
                                fieldWithPath("shop.passwordMessage")
                                        .description("The operator's message on the gate page, or null").optional(),
                                fieldWithPath("request.locale")
                                        .description("The locale this render actually uses, after fallback"),
                                subsectionWithPath("navigation")
                                        .description("The operator's menus, resolved for this locale — chrome, so "
                                                + "present on every render context"))));
    }

    @Test
    void getShopRenderContextInARequestedLocale() throws Exception {
        when(getShopUseCase.execute(SLUG, "es")).thenReturn(shopContext());

        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/shop?locale=es", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsACallWithoutTheSharedSecret() throws Exception {
        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/shop", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsACallWithTheWrongSharedSecret() throws Exception {
        mockMvc.perform(get("/api/internal/render-context/{tenantSlug}/shop", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, "not-the-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyStorefrontPassword() throws Exception {
        when(verifyPasswordUseCase.execute(SLUG, "opensesame")).thenReturn(true);

        mockMvc.perform(post("/api/internal/storefront/{tenantSlug}/verify-password", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"opensesame\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andDo(document("rendering-verify-storefront-password",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        requestFields(
                                fieldWithPath("password").description("The visitor's attempt at the gate password")),
                        responseFields(
                                fieldWithPath("verified")
                                        .description("Whether the attempt unlocks the storefront. False also "
                                                + "covers an unknown tenant and a disabled gate, so the endpoint "
                                                + "reveals nothing beyond the answer"))));
    }

    @Test
    void aWrongPasswordIsStillTwoHundred() throws Exception {
        when(verifyPasswordUseCase.execute(SLUG, "guess")).thenReturn(false);

        mockMvc.perform(post("/api/internal/storefront/{tenantSlug}/verify-password", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"guess\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }
}
