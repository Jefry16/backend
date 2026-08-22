package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyDetailView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.dto.output.StorefrontPolicyOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetStorefrontPolicyUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The policy page — the last of the storefront's addresses to stop being a
 * placeholder.
 *
 * <p>No test sends an {@code Authorization} header and every one expects a body:
 * the storefront is public, and importing {@link StorefrontPublicRoutes} is what
 * proves it. Omit that import and every request 401s, so the assertions pass
 * without testing anything (PATTERNS §8c).
 */
@WebMvcTest(StorefrontPolicyController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontPolicyControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID POLICY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb3");
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001", "ES", "Spain");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontPolicyUseCase getStorefrontPolicyUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any())).thenReturn(Map.of());
        when(mediaUrlResolver.toUrl(anyString())).thenAnswer(c -> "https://media.test/" + c.getArgument(0));
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.UNLOCKED);
    }

    private static StorefrontGlobals globals(String current, String primary, List<String> supported) {
        TourOperatorView operator = new TourOperatorView(OPERATOR, "Acme Tours", "acme", ADDRESS, null, null,
                "Sailing day trips", "The best sailing in Mallorca", null, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of(new PolicyView(POLICY, "LEGAL_NOTICE", "Aviso legal")));
        return new StorefrontGlobals(operator, "Terms of service", "The best sailing in Mallorca", null,
                List.of(), List.of(), List.of(), new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale, String slug, String type, String title) {
        when(getStorefrontPolicyUseCase.execute("acme", pathLocale, slug))
                .thenReturn(Optional.of(new StorefrontPolicyOutput(
                        globals(pathLocale == null ? "en" : pathLocale, "en", List.of("en", "es")),
                        new PolicyDetailView(POLICY, type, title, "<p>The terms.</p>"))));
    }

    @Test
    void aPolicyPageIsTheGlobalsPlusTheDocument() throws Exception {
        served(null, "terms", "TERMS", "Terms of service");

        mockMvc.perform(get("/policies/terms").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.policy.id").value(POLICY.toString()))
                .andExpect(jsonPath("$.policy.type").value("TERMS"))
                .andExpect(jsonPath("$.policy.title").value("Terms of service"))
                .andExpect(jsonPath("$.policy.body").value("<p>The terms.</p>"))
                .andExpect(jsonPath("$.policy.url").value("/policies/terms"))
                // Every other route's object stays absent.
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.experience").doesNotExist())
                .andExpect(jsonPath("$.experiences").doesNotExist());
    }

    @Test
    void thePageTypeIsPolicyAndTheCanonicalIsItsOwnAddress() throws Exception {
        served(null, "terms", "TERMS", "Terms of service");

        mockMvc.perform(get("/policies/terms?utm_source=footer").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.pageType").value("policy"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/policies/terms"));
    }

    /** The multi-word type is the one where the slug rule is doing work. */
    @Test
    void aMultiWordTypeIsAddressedWithAHyphen() throws Exception {
        served(null, "legal-notice", "LEGAL_NOTICE", "Legal notice");

        mockMvc.perform(get("/policies/legal-notice").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy.type").value("LEGAL_NOTICE"))
                .andExpect(jsonPath("$.policy.url").value("/policies/legal-notice"));
    }

    /**
     * <b>The slug is not a handle</b> — it derives from a closed enum, not from
     * anything an operator types, so it is the same in every language. That is why
     * this route needs no per-locale handles where the page and experience routes
     * do.
     */
    @Test
    void theSwitcherOffersTheSameSlugUnderEachPrefix() throws Exception {
        served("es", "terms", "TERMS", "Condiciones del servicio");

        mockMvc.perform(get("/es/policies/terms").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy.title").value("Condiciones del servicio"))
                .andExpect(jsonPath("$.policy.url").value("/es/policies/terms"))
                .andExpect(jsonPath("$.localization.languages[0].url").value("/policies/terms"))
                .andExpect(jsonPath("$.localization.languages[1].url").value("/es/policies/terms"))
                .andExpect(jsonPath("$.localization.language.url").value("/es/policies/terms"));
    }

    /**
     * A slug no policy type has and a policy this operator never wrote answer
     * identically — otherwise a visitor could enumerate which of the four an
     * operator has by watching status codes.
     */
    @Test
    void anUnknownSlugAndAnUnwrittenPolicyAreTheSame404() throws Exception {
        when(getStorefrontPolicyUseCase.execute("acme", null, "refunds")).thenReturn(Optional.empty());
        when(getStorefrontPolicyUseCase.execute("acme", null, "privacy")).thenReturn(Optional.empty());

        for (String slug : new String[]{"refunds", "privacy"}) {
            mockMvc.perform(get("/policies/" + slug).header("Host", "acme.localhost:8080"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
        }
    }

    @Test
    void anUnknownHostIsTheSame404() throws Exception {
        when(tenantHandleResolver.resolve("nobody.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/policies/terms").header("Host", "nobody.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /** The slug reaches the use case verbatim; translating it is the use case's job. */
    @Test
    void theSlugIsPassedThroughUntranslated() throws Exception {
        served(null, "legal-notice", "LEGAL_NOTICE", "Legal notice");

        mockMvc.perform(get("/policies/legal-notice").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());

        verify(getStorefrontPolicyUseCase).execute("acme", null, "legal-notice");
    }

    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null, "terms", "TERMS", "Terms of service");
        served("es", "terms", "TERMS", "Condiciones");

        mockMvc.perform(head("/policies/terms").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/es/policies/terms").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    @Test
    void aLockedStoreRedirectsThePolicyPage() throws Exception {
        served(null, "terms", "TERMS", "Terms of service");
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/policies/terms").header("Host", "acme.localhost:8080"))
                .andExpect(status().is3xxRedirection());
    }
}
