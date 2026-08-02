package com.vointika.rendering.presentation.controller;

import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.SeoResolver;
import com.vointika.rendering.application.usecase.GetShopRenderContextUseCase;
import com.vointika.rendering.application.usecase.VerifyStorefrontPasswordUseCase;
import com.vointika.rendering.infrastructure.security.RenderingPublicRoutes;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.web.security.StorefrontApiSecretFilter;
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
// `app.storefront.shared-secret` has no default in application.yml (it is supplied
// by APP_STOREFRONT_SHARED_SECRET at runtime), and a test that authenticates
// against it must not depend on whoever's shell is running the suite.
@WebMvcTest(controllers = {RenderContextController.class, StorefrontController.class},
        properties = "app.storefront.shared-secret=test-storefront-secret")
@ExtendWith(RestDocumentationExtension.class)
// RenderingPublicRoutes is imported deliberately, not incidentally: without it
// the chain's anyRequest().authenticated() rejects these paths with 401 even
// when the shared secret is correct, because the secret filter authenticates
// the caller without populating a SecurityContext. The registrar is what makes
// the storefront surface reachable at all — this test would pass vacuously
// (401 everywhere) if it were left out.
@Import({SecurityConfig.class, RenderingPublicRoutes.class})
class RenderContextControllerDocumentationTest {

    private static final String SLUG = "acme";
    private static final String SECRET = "test-storefront-secret";

    private MockMvc mockMvc;

    @MockitoBean private GetShopRenderContextUseCase getShopUseCase;
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
                null,
                "Acme Tours — dive trips in Santo Domingo",
                "Small-group diving and snorkelling on the south coast.",
                "https://media.staging.vointika.com/og.png",
                java.util.Map.of());
    }

    private ShopRenderContext shopContext() {
        StorefrontOperatorView shop = operatorView();
        // Built through the resolver, not hand-assembled: the documented payload
        // should be what the chain actually produces.
        return new ShopRenderContext(
                shop, "en", List.of(),
                SeoResolver.forHome(shop, "en"),
                SeoResolver.passwordMessage(shop, "en"));
    }

    @Test
    void getShopRenderContext() throws Exception {
        when(getShopUseCase.execute(eq(SLUG), isNull())).thenReturn(shopContext());

        mockMvc.perform(get("/api/storefront/render-context/{tenantSlug}", SLUG)
                        .header(StorefrontApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.slug").value(SLUG))
                .andExpect(jsonPath("$.request.locale").value("en"))
                .andDo(document("rendering-shop-render-context",
                        requestHeaders(
                                headerWithName(StorefrontApiSecretFilter.HEADER_NAME)
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
                                                + "present on every render context"),
                                fieldWithPath("seo.title")
                                        .description("Resolved page title: the shop's translated SEO title, then its "
                                                + "canonical one, then the shop name. Never suffixed with the shop "
                                                + "name — that is a theme's choice").optional(),
                                fieldWithPath("seo.description")
                                        .description("Resolved meta description: translated, then canonical, else "
                                                + "null when the operator has set none").optional(),
                                fieldWithPath("seo.imageUrl")
                                        .description("Resolved social image: the shop's OG image, else its logo, "
                                                + "else null").optional())));
    }

    @Test
    void getShopRenderContextInARequestedLocale() throws Exception {
        when(getShopUseCase.execute(SLUG, "es")).thenReturn(shopContext());

        mockMvc.perform(get("/api/storefront/render-context/{tenantSlug}?locale=es", SLUG)
                        .header(StorefrontApiSecretFilter.HEADER_NAME, SECRET))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsACallWithoutTheSharedSecret() throws Exception {
        mockMvc.perform(get("/api/storefront/render-context/{tenantSlug}", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsACallWithTheWrongSharedSecret() throws Exception {
        mockMvc.perform(get("/api/storefront/render-context/{tenantSlug}", SLUG)
                        .header(StorefrontApiSecretFilter.HEADER_NAME, "not-the-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyStorefrontPassword() throws Exception {
        when(verifyPasswordUseCase.execute(SLUG, "opensesame")).thenReturn(true);

        mockMvc.perform(post("/api/storefront/{tenantSlug}/verify-password", SLUG)
                        .header(StorefrontApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"opensesame\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andDo(document("rendering-verify-storefront-password",
                        requestHeaders(
                                headerWithName(StorefrontApiSecretFilter.HEADER_NAME)
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

        mockMvc.perform(post("/api/storefront/{tenantSlug}/verify-password", SLUG)
                        .header(StorefrontApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"guess\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }
}
