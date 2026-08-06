package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.BrandData;
import com.vointika.storefront.application.dto.output.BrandData.ColorsData;
import com.vointika.storefront.application.dto.output.LocalizationData;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
import com.vointika.storefront.application.dto.output.PageData;
import com.vointika.storefront.application.dto.output.PolicyData;
import com.vointika.storefront.application.dto.output.PolicyPageOutput;
import com.vointika.storefront.application.dto.output.PolicyPageOutput.PolicyDocument;
import com.vointika.storefront.application.dto.output.ShopData;
import com.vointika.storefront.application.dto.output.ShopData.CurrencyData;
import com.vointika.storefront.application.dto.output.ShopData.TimezoneData;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetPolicyPageUseCase;
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
 * The storefront's third page type, and the first that renders HTML the operator
 * wrote rather than text they typed — so the assertion that matters most in this
 * class is the one showing the body arriving as markup rather than as escaped
 * text.
 *
 * <p>As on the other two pages, no test here sends an {@code Authorization}
 * header: both routes are public, and each has to be registered twice over, once
 * per method, in {@code StorefrontPublicRoutes} and once in the gate's patterns.
 */
@WebMvcTest(PolicyPageController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class,
        StorefrontWebConfig.class, ThemeContextDump.class})
class PolicyPageControllerTest {

    private static final UUID SHOP_ID = UUID.fromString("01900000-0000-7000-8000-000000000002");

    private static final String BODY = "<h2>Free cancellation</h2>"
            + "<ul><li>48 hours or more: full refund.</li></ul>"
            + "<p>Write to <a href=\"mailto:hola@acme.test\">hola@acme.test</a>.</p>";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetPolicyPageUseCase getPolicyPageUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void unlockedByDefault() {
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.UNLOCKED);
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
    }

    /**
     * <b>The body renders as HTML, and that is the slice's whole decision.</b>
     * Escaping it would put {@code &lt;h2&gt;} on the page — the operator wrote
     * markup about their own storefront, stored verbatim, and rendering it is the
     * feature. Change the template's unescaped tag to the escaping one and this
     * is the test that goes red.
     */
    @Test
    void thePolicyRendersWithItsHtmlIntact() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "cancellation"))
                .thenReturn(Optional.of(page("es", "Cancellation policy", BODY)));

        mockMvc.perform(get("/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Cancellation policy</h1>"),
                        containsString("<h2>Free cancellation</h2>"),
                        containsString("<li>48 hours or more: full refund.</li>"),
                        containsString("<a href=\"mailto:hola@acme.test\">hola@acme.test</a>"),
                        not(containsString("&lt;h2&gt;")))));
    }

    /**
     * <b>A script in a policy body runs, and that is asserted deliberately.</b>
     * The body is unescaped by design, so this documents the size of the trust
     * being placed in the operator rather than pretending it is smaller: they are
     * the author of their own storefront, and this is our template rendering
     * their content — not a template they wrote, which is the question that stays
     * closed. Reading it as a defect would lead to "fixing" the escaping and
     * shipping {@code &lt;p&gt;} to every visitor.
     */
    @Test
    void aScriptInThePolicyBodyRendersAsAScript() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "terms"))
                .thenReturn(Optional.of(page("es", "Terms", "<p>Hi</p><script>alert(1)</script>")));

        mockMvc.perform(get("/policies/terms").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<script>alert(1)</script>")));
    }

    /** The title is the policy's, not the shop's — the first page type with one of its own. */
    @Test
    void thePolicysTitleIsTheTitleTag() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "cancellation"))
                .thenReturn(Optional.of(page("es", "Política de cancelación", "<p>48 horas.</p>")));

        mockMvc.perform(get("/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(content().string(allOf(
                        containsString("<title>Política de cancelación</title>"),
                        containsString("<link rel=\"canonical\" "
                                + "href=\"http://acme.localhost/policies/cancellation\">"))));
    }

    /**
     * The enum name is not the slug, and this is the route that has to know it —
     * both directions in one request: {@code legal-notice} is what the URL says
     * and what the footer and the switcher link back to.
     */
    @Test
    void anUnderscoredTypeIsAddressedAndLinkedWithAHyphen() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "legal-notice"))
                .thenReturn(Optional.of(page("es", "Legal notice", "<p>Acme Tours S.L.</p>",
                        new PolicyData("LEGAL_NOTICE", "Legal notice", "legal-notice"))));

        mockMvc.perform(get("/policies/legal-notice").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Legal notice</h1>"),
                        containsString("<a href=\"/policies/legal-notice\">Legal notice</a>"))));
    }

    /**
     * The switcher points at <em>this policy</em> in each language, not at the
     * shop's front door. Same rule as the listing, one address deeper.
     */
    @Test
    void theSwitcherLinksToThisPolicyInEachLanguage() throws Exception {
        when(getPolicyPageUseCase.execute("acme", "en", "cancellation"))
                .thenReturn(Optional.of(page("en", "Cancellation policy", "<p>48 hours.</p>")));

        mockMvc.perform(get("/en/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"en\">"),
                        containsString("<a href=\"/policies/cancellation\" lang=\"es\">es</a>"),
                        containsString("<span lang=\"en\">en</span>"),
                        containsString("<a href=\"/fr/policies/cancellation\" lang=\"fr\">fr</a>"))));
    }

    /**
     * A type nobody wrote and a slug no type is named after are the same 404 with
     * the same page — which is also what stops {@code valueOf} from turning the
     * second into a 500 in the API's JSON error shape.
     */
    @Test
    void anUnwrittenTypeIsTheNotFoundPage() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "privacy")).thenReturn(Optional.empty());

        mockMvc.perform(get("/policies/privacy").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void aSlugNoTypeIsNamedAfterIsTheNotFoundPage() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "refunds")).thenReturn(Optional.empty());

        mockMvc.perform(get("/policies/refunds").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void answersNotFoundWhenTheHostResolvesToNoTenant() throws Exception {
        when(tenantHandleResolver.resolve("localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/policies/cancellation").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    /**
     * HEAD is registered per route <em>and</em> per method: Spring MVC serves it
     * from the {@code @GetMapping} for free, Spring Security does not. Drop either
     * new HEAD entry and this fails on a 401 — the shape crawlers, link checkers
     * and uptime monitors would have found in production.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "cancellation"))
                .thenReturn(Optional.of(page("es", "Cancellation policy", BODY)));
        when(getPolicyPageUseCase.execute("acme", "en", "cancellation"))
                .thenReturn(Optional.of(page("en", "Cancellation policy", BODY)));

        mockMvc.perform(head("/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(head("/en/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    /**
     * Both new routes are the gate's business, and nothing fails if one is left
     * out of {@code StorefrontWebConfig} — a locked store would simply serve the
     * page. Drop either pattern and one of these two answers 200.
     */
    @Test
    void aLockedStoreRedirectsFromBothRoutes() throws Exception {
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getPolicyPageUseCase.execute("acme", null, "cancellation"))
                .thenReturn(Optional.of(page("es", "Cancellation policy", BODY)));
        when(getPolicyPageUseCase.execute("acme", "en", "cancellation"))
                .thenReturn(Optional.of(page("en", "Cancellation policy", BODY)));

        mockMvc.perform(get("/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
        mockMvc.perform(get("/en/policies/cancellation").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * <b>The title is operator-authored text and is still escaped.</b> Only the
     * body is raw, and only because it is HTML by definition — the exception is
     * one field wide, not one page wide.
     */
    @Test
    void escapesThePolicyTitle() throws Exception {
        when(getPolicyPageUseCase.execute("acme", null, "terms"))
                .thenReturn(Optional.of(page("es", "<script>alert(1)</script>", "<p>Fine.</p>")));

        mockMvc.perform(get("/policies/terms").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>&lt;script&gt;alert(1)&lt;/script&gt;</h1>"),
                        containsString("<p>Fine.</p>"))));
    }

    private static PolicyPageOutput page(String locale, String title, String body) {
        return page(locale, title, body,
                new PolicyData("CANCELLATION", "Cancellation policy", "cancellation"));
    }

    private static PolicyPageOutput page(String locale, String title, String body, PolicyData... policies) {
        return new PolicyPageOutput(
                new StorefrontPageData(
                        new ShopData(SHOP_ID, "Acme Tours", "Calle Mayor 1, 28013 Madrid", null, null,
                                "A shop description", null, noBrand(), List.of(policies),
                                new CurrencyData("EUR", "€"),
                                new TimezoneData("Europe/Madrid", "Madrid")),
                        new PageData(title, null, null),
                        new LocalizationData(locale, List.of(
                                new LanguageData("es", "es".equals(locale), null),
                                new LanguageData("en", "en".equals(locale), "en"),
                                new LanguageData("fr", "fr".equals(locale), "fr")))),
                new PolicyDocument(policies[0].type(), title, body));
    }

    private static BrandData noBrand() {
        return new BrandData(null, null, new ColorsData(List.of(), List.of()),
                null, null, null, null, List.of());
    }
}
