package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.presentation.view.HomeView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The storefront home page: one tenant, one template, rendered in-process.
 *
 * <p>It renders and writes the string itself rather than returning a view name,
 * because Spring's {@code MustacheView} recompiles the template on every
 * request — and owning the compiled template is what the real theme cache will
 * need anyway.
 *
 * <p><b>It must not throw.</b> No handle and no such operator are both 404 with
 * the not-found template; an exception would reach
 * {@code GlobalExceptionHandler}, a global {@code @ControllerAdvice} that would
 * answer an HTML request with a JSON body.
 *
 * <p>The tenant comes from {@code getServerName()} rather than the raw
 * {@code Host} header, and the difference only shows up behind a proxy:
 * {@code ForwardedHeaderFilter} (enabled by {@code server.forward-headers-strategy},
 * which {@code EndpointRateLimitFilter} already wants for the client IP) makes
 * the servlet API honour {@code X-Forwarded-Host} and strips the forwarded
 * headers. A {@code @RequestHeader("Host")} would keep reading the literal header
 * and quietly resolve a different tenant from the rest of the app.
 */
@Controller
public class StorefrontHomeController {

    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final TenantHandleResolver tenantHandleResolver;
    private final GetHomePageUseCase getHomePageUseCase;
    private final MediaUrlResolver mediaUrlResolver;
    private final Template homeTemplate;
    private final Template notFoundTemplate;

    public StorefrontHomeController(TenantHandleResolver tenantHandleResolver,
                                    GetHomePageUseCase getHomePageUseCase,
                                    MediaUrlResolver mediaUrlResolver,
                                    @Qualifier("storefrontHomeTemplate") Template homeTemplate,
                                    @Qualifier("storefrontNotFoundTemplate") Template notFoundTemplate) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getHomePageUseCase = getHomePageUseCase;
        this.mediaUrlResolver = mediaUrlResolver;
        this.homeTemplate = homeTemplate;
        this.notFoundTemplate = notFoundTemplate;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home(HttpServletRequest request) {
        return tenantHandleResolver.resolve(request.getServerName())
                .flatMap(getHomePageUseCase::execute)
                .map(page -> ResponseEntity.ok()
                        .contentType(HTML_UTF8)
                        .body(homeTemplate.execute(HomeView.from(page, mediaUrlResolver))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(HTML_UTF8)
                        .body(notFoundTemplate.execute(Map.of())));
    }
}
