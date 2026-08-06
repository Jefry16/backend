package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetPolicyPageUseCase;
import com.vointika.storefront.presentation.view.PolicyView;
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
 * The storefront's third page type, and the first that renders HTML the operator
 * wrote rather than text they typed.
 *
 * <p>Both routes come from {@link StorefrontRoutes}, because the same two
 * patterns are also registered as {@code PublicRoute}s and as the gate's
 * interceptor patterns; a retyped copy is a 401 or an ungated page waiting to
 * happen.
 *
 * <p><b>An unwritten policy — and a slug no type is named after — is a 404 with
 * the not-found page, never a throw.</b> {@code GlobalExceptionHandler} is a
 * global {@code @ControllerAdvice} and would answer an HTML request with a JSON
 * body; the type never reaches {@code valueOf} on the way there either.
 *
 * <p>It reads the tenant from {@code getServerName()} and writes the rendered
 * string itself rather than returning a view name, both for the reasons
 * {@link StorefrontHomeController} documents.
 */
@Controller
public class PolicyPageController {

    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final TenantHandleResolver tenantHandleResolver;
    private final GetPolicyPageUseCase getPolicyPageUseCase;
    private final MediaUrlResolver mediaUrlResolver;
    private final Template policyTemplate;
    private final Template notFoundTemplate;
    private final ThemeContextDump contextDump;

    public PolicyPageController(TenantHandleResolver tenantHandleResolver,
                                GetPolicyPageUseCase getPolicyPageUseCase,
                                MediaUrlResolver mediaUrlResolver,
                                @Qualifier("storefrontPolicyTemplate") Template policyTemplate,
                                @Qualifier("storefrontNotFoundTemplate") Template notFoundTemplate,
                                ThemeContextDump contextDump) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getPolicyPageUseCase = getPolicyPageUseCase;
        this.mediaUrlResolver = mediaUrlResolver;
        this.policyTemplate = policyTemplate;
        this.notFoundTemplate = notFoundTemplate;
        this.contextDump = contextDump;
    }

    @GetMapping(path = StorefrontRoutes.POLICY, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> policy(@PathVariable String type, HttpServletRequest request) {
        return render(request, null, type);
    }

    /**
     * The same document under a published secondary locale. The primary is a 404
     * here exactly as it is at {@code /{primary}} — it already lives at
     * {@code /policies/{type}}.
     */
    @GetMapping(path = StorefrontRoutes.LOCALIZED_POLICY, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> localizedPolicy(@PathVariable String locale,
                                                  @PathVariable String type,
                                                  HttpServletRequest request) {
        return render(request, locale, type);
    }

    private ResponseEntity<String> render(HttpServletRequest request, String pathLocale, String slug) {
        return tenantHandleResolver.resolve(request.getServerName())
                .flatMap(handle -> getPolicyPageUseCase.execute(handle, pathLocale, slug))
                .map(page -> respond(
                        PolicyView.from(page, pathLocale, mediaUrlResolver,
                                RequestOrigin.of(request), request.getRequestURI()),
                        request))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(HTML_UTF8)
                        .body(notFoundTemplate.execute(Map.of())));
    }

    /**
     * The page, or the object the page would have rendered from — see
     * {@link ThemeContextDump}. The 404 branch has no context object, so it is
     * always the not-found page.
     */
    private ResponseEntity<String> respond(Object view, HttpServletRequest request) {
        if (contextDump.requestedIn(request)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(contextDump.of(view));
        }
        return ResponseEntity.ok().contentType(HTML_UTF8).body(policyTemplate.execute(view));
    }
}
