package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.StorefrontContactQuery.ContactFormView;
import com.vointika.shared.port.StorefrontContactQuery.FieldView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.StorefrontContactOutput;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetStorefrontContactUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /contact} — the page a visitor writes to the inbox from.
 *
 * <p><b>The only page route with nothing of its own to miss on.</b> Every other
 * one resolves a handle or a slug that can come back empty; the form is the same
 * for every operator, so the single 404 here is the host or the locale.
 */
@WebMvcTest(StorefrontContactController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontContactControllerTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001", "ES", "Spain");

    /** The real shape, so the payload assertions below are about real limits. */
    private static final ContactFormView FORM = new ContactFormView(List.of(
            new FieldView("name", true, 120),
            new FieldView("email", true, 320),
            new FieldView("summary", true, 200),
            new FieldView("content", true, 5000)));

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetStorefrontContactUseCase getStorefrontContactUseCase;
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
                List.of());
        return new StorefrontGlobals(operator, "Acme Tours", "The best sailing in Mallorca", null,
                List.of(), List.of(), List.of(), new LocalizationData(current, primary, supported));
    }

    private void served(String pathLocale) {
        when(getStorefrontContactUseCase.execute("acme", pathLocale))
                .thenReturn(Optional.of(new StorefrontContactOutput(
                        globals(pathLocale == null ? "en" : pathLocale, "en", List.of("en", "es")), FORM)));
    }

    @Test
    void theContactPageIsTheGlobalsPlusTheFormsShape() throws Exception {
        served(null);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourOperator.name").value("Acme Tours"))
                .andExpect(jsonPath("$.pageType").value("contact"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/contact"))
                .andExpect(jsonPath("$.contactForm.fields.length()").value(4))
                .andExpect(jsonPath("$.contactForm.fields[0].name").value("name"))
                .andExpect(jsonPath("$.contactForm.fields[0].required").value(true))
                .andExpect(jsonPath("$.contactForm.fields[0].maxLength").value(120))
                .andExpect(jsonPath("$.contactForm.fields[3].name").value("content"))
                .andExpect(jsonPath("$.contactForm.fields[3].maxLength").value(5000));
    }

    /**
     * <b>Order is part of the contract.</b> A theme renders the fields in the order
     * it is given them, so a list that arrived shuffled would render a form asking
     * for the message before the sender's name.
     */
    @Test
    void theFieldsArriveInTheOrderAFormShouldRenderThem() throws Exception {
        served(null);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactForm.fields[0].name").value("name"))
                .andExpect(jsonPath("$.contactForm.fields[1].name").value("email"))
                .andExpect(jsonPath("$.contactForm.fields[2].name").value("summary"))
                .andExpect(jsonPath("$.contactForm.fields[3].name").value("content"));
    }

    /**
     * <b>No {@code action}.</b> Intake is deleted, so there is nowhere to post, and
     * a contract that named an address nothing serves would be worse than one that
     * is silent. Pinned so it is not added absent-mindedly with the field list.
     */
    @Test
    void theFormPublishesNoActionWhileThereIsNowhereToPost() throws Exception {
        served(null);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactForm.action").doesNotExist());
    }

    /** And the page accepts nothing — the route is GET/HEAD, so a submission is refused. */
    @Test
    void thePageDoesNotAcceptASubmission() throws Exception {
        mockMvc.perform(post("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theLocalizedFormServesUnderItsPrefix() throws Exception {
        served("es");

        mockMvc.perform(get("/es/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageType").value("contact"))
                .andExpect(jsonPath("$.localization.language.code").value("es"))
                .andExpect(jsonPath("$.canonicalUrl").value("http://acme.localhost:8080/es/contact"));
    }

    /** The switcher offers this page in each language, not the home page. */
    @Test
    void theSwitcherOffersTheContactPageInEachLanguage() throws Exception {
        served(null);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localization.languages[0].code").value("en"))
                .andExpect(jsonPath("$.localization.languages[0].url").value("/contact"))
                .andExpect(jsonPath("$.localization.languages[1].code").value("es"))
                .andExpect(jsonPath("$.localization.languages[1].url").value("/es/contact"))
                .andExpect(jsonPath("$.localization.language.url").value("/contact"));
    }

    /**
     * <b>No entity, so no title of its own.</b> The page falls all the way through
     * to the operator's SEO — asserted rather than left implicit, because it is the
     * second page type in that position and the thing someone would "fix" with a
     * hardcoded English "Contact".
     */
    @Test
    void seoFallsThroughToTheOperator() throws Exception {
        served(null);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageTitle").value("Acme Tours"))
                .andExpect(jsonPath("$.pageDescription").value("The best sailing in Mallorca"));
    }

    /** A locale the operator does not publish is the same 404 as an unknown host. */
    @Test
    void anUnpublishedLocaleIs404() throws Exception {
        when(getStorefrontContactUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    @Test
    void aHostNoOperatorOwnsIs404() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/contact").header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound());
    }

    /** A page route is a public page, and crawlers HEAD it — both forms. */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        served(null);
        served("es");

        mockMvc.perform(head("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/es/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    /** A locked store shows the gate here too — the gate runs before locale resolution. */
    @Test
    void aLockedStoreRedirectsToTheGate() throws Exception {
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/contact").header("Host", "acme.localhost:8080"))
                .andExpect(status().is3xxRedirection());
    }
}
