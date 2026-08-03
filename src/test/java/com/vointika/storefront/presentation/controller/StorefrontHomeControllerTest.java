package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.HomePageOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.infrastructure.config.StorefrontMustacheConfig;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No test here sends an {@code Authorization} header, and every one of them
 * expects a page — the storefront is public, and that is half of what this class
 * proves. The other half is that the compiler configured in
 * {@link StorefrontMustacheConfig} renders operator-authored text safely.
 */
@WebMvcTest(StorefrontHomeController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class})
class StorefrontHomeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetHomePageUseCase getHomePageUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

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
        when(getHomePageUseCase.execute("acme")).thenReturn(Optional.of(new HomePageOutput(
                "Acme Tours - day trips", "Acme Tours", "Boat tours and day trips",
                "tour-operators/1/logo.png", "tour-operators/1/og.png")));
        when(mediaUrlResolver.toUrl("tour-operators/1/logo.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/logo.png");
        when(mediaUrlResolver.toUrl("tour-operators/1/og.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/og.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<title>Acme Tours - day trips</title>"),
                        containsString("<h1>Acme Tours</h1>"),
                        containsString("Boat tours and day trips"),
                        containsString("<img src=\"http://localhost:9000/avatars/tour-operators/1/logo.png\""),
                        containsString("<meta property=\"og:type\" content=\"website\">"),
                        containsString("<meta property=\"og:image\" "
                                + "content=\"http://localhost:9000/avatars/tour-operators/1/og.png\">"))));
    }

    /**
     * An operator who has filled in nothing optional still gets a page. Without
     * the compiler's {@code defaultValue("")} this is a 500, not a bare page.
     */
    @Test
    void omitsTheOptionalTagsWhenNothingIsSet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme")).thenReturn(Optional.of(
                new HomePageOutput("Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        not(containsString("og:image")),
                        not(containsString("<img")),
                        not(containsString("name=\"description\"")))));
    }

    /**
     * HEAD has to be registered alongside GET in {@link StorefrontPublicRoutes}:
     * Spring MVC serves it from the {@code @GetMapping} for free, but Spring
     * Security matches on the exact method and would reject it at the filter
     * chain — a 401 in a JSON shape, on a public page. Drop the HEAD entry and
     * this fails on the status. Crawlers, link checkers and uptime monitors all
     * send HEAD, and the whole slice shipped without noticing because every other
     * test and curl used GET.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme")).thenReturn(Optional.of(
                new HomePageOutput("Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(head("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void answersNotFoundForAnUnknownHandle() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.of("nope"));
        when(getHomePageUseCase.execute("nope")).thenReturn(Optional.empty());

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

    @Test
    void escapesOperatorAuthoredText() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme")).thenReturn(Optional.of(new HomePageOutput(
                "<script>alert(1)</script>", "<script>alert(1)</script>", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("<script>")),
                        containsString("&lt;script&gt;"))));
    }
}
