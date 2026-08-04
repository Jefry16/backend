package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetExperienceListPageUseCase;
import com.vointika.storefront.presentation.view.ExperienceListView;
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
 * The storefront's listing of what an operator sells — the first page that
 * renders a collection, and the first that shares its chrome with another page
 * through {@code storefront/layout}.
 *
 * <p>Both routes come from {@link StorefrontRoutes}, because the same two
 * patterns are also registered as {@code PublicRoute}s and as the gate's
 * interceptor patterns; a retyped copy is a 401 or an ungated page waiting to
 * happen.
 *
 * <p><b>No pagination and no cap.</b> An operator has tens of experiences, not
 * thousands, and a silent cap is worse than none — truncation reads as
 * completeness. Revisit against a real operator, not against a number.
 *
 * <p>It must not throw, reads the tenant from {@code getServerName()} and writes
 * the rendered string itself rather than returning a view name — all three for
 * the reasons {@link StorefrontHomeController} documents.
 */
@Controller
public class ExperienceListController {

    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final TenantHandleResolver tenantHandleResolver;
    private final GetExperienceListPageUseCase getExperienceListPageUseCase;
    private final MediaUrlResolver mediaUrlResolver;
    private final Template experienceListTemplate;
    private final Template notFoundTemplate;

    public ExperienceListController(TenantHandleResolver tenantHandleResolver,
                                    GetExperienceListPageUseCase getExperienceListPageUseCase,
                                    MediaUrlResolver mediaUrlResolver,
                                    @Qualifier("storefrontExperienceListTemplate") Template experienceListTemplate,
                                    @Qualifier("storefrontNotFoundTemplate") Template notFoundTemplate) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getExperienceListPageUseCase = getExperienceListPageUseCase;
        this.mediaUrlResolver = mediaUrlResolver;
        this.experienceListTemplate = experienceListTemplate;
        this.notFoundTemplate = notFoundTemplate;
    }

    @GetMapping(path = StorefrontRoutes.EXPERIENCES, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> experiences(HttpServletRequest request) {
        return render(request, null);
    }

    /**
     * The same listing under a published secondary locale. The primary is a 404
     * here exactly as it is at {@code /{primary}} — it already lives at
     * {@link StorefrontRoutes#EXPERIENCES}.
     */
    @GetMapping(path = StorefrontRoutes.LOCALIZED_EXPERIENCES, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> localizedExperiences(@PathVariable String locale, HttpServletRequest request) {
        return render(request, locale);
    }

    private ResponseEntity<String> render(HttpServletRequest request, String pathLocale) {
        return tenantHandleResolver.resolve(request.getServerName())
                .flatMap(handle -> getExperienceListPageUseCase.execute(handle, pathLocale))
                .map(page -> ResponseEntity.ok()
                        .contentType(HTML_UTF8)
                        .body(experienceListTemplate.execute(
                                ExperienceListView.from(page, pathLocale, mediaUrlResolver,
                                RequestOrigin.of(request), request.getRequestURI()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(HTML_UTF8)
                        .body(notFoundTemplate.execute(Map.of())));
    }
}
