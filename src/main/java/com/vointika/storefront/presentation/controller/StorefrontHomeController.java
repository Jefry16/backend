package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.policy.LocaleResolver;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The storefront home page: one tenant, one template, rendered in-process, in
 * one of the operator's locales.
 *
 * <p>The locale route is {@link LocaleResolver#PATH_TEMPLATE}, whose variable is
 * constrained rather than bare: the same string is registered as a
 * {@code PublicRoute}, and a bare {@code /{locale}} would open every
 * single-segment path in the application.
 *
 * <p>It would still match the literal {@code /password} if the constraint were
 * loosened; Spring's {@code PathPattern} prefers the literal, so the password
 * page would win anyway. That is a property of the matcher rather than of this
 * class, and every future top-level literal route inherits the same collision —
 * {@code PasswordPageControllerTest} asserts it so the next one breaks a test
 * rather than a page.
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
        return render(request, null);
    }

    /**
     * A secondary locale the operator publishes. The primary is <b>not</b> served
     * here — it lives at {@code /}, and two URLs for one page is duplicate
     * content — so {@code /{primary}} is a 404 like any unsupported locale.
     */
    @GetMapping(path = LocaleResolver.PATH_TEMPLATE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> localizedHome(@PathVariable String locale, HttpServletRequest request) {
        return render(request, locale);
    }

    private ResponseEntity<String> render(HttpServletRequest request, String pathLocale) {
        return tenantHandleResolver.resolve(request.getServerName())
                .flatMap(handle -> getHomePageUseCase.execute(handle, pathLocale))
                .map(page -> ResponseEntity.ok()
                        .contentType(HTML_UTF8)
                        .body(homeTemplate.execute(HomeView.from(page, pathLocale, mediaUrlResolver,
                                RequestOrigin.of(request), request.getRequestURI()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(HTML_UTF8)
                        .body(notFoundTemplate.execute(Map.of())));
    }
}
