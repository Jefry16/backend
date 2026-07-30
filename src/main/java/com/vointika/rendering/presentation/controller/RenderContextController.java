package com.vointika.rendering.presentation.controller;

import com.vointika.rendering.application.usecase.GetExperienceListRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetExperienceRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetShopRenderContextUseCase;
import com.vointika.rendering.presentation.response.ExperienceListRenderContextResponse;
import com.vointika.rendering.presentation.response.ExperienceRenderContextResponse;
import com.vointika.rendering.presentation.response.ShopRenderContextResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Render contexts for the public storefront — everything a page needs, in one
 * response per page render.
 *
 * <p>Server-to-server: the only caller is the storefront BFF, authenticated by
 * {@code X-Internal-Secret} rather than a JWT. The one-call-per-page shape is
 * deliberate — composition belongs here, where the domain lives, not fanned out
 * across the edge.
 *
 * <p><strong>Trust boundary:</strong> when an operator's password gate is on,
 * enforcing it is the BFF's job — it holds the visitor's unlock cookie, which
 * this stateless API cannot see. That is safe only because the BFF is the sole
 * client of this surface and the shared secret is what keeps it that way. The
 * gate state travels on every {@code shop} block so the BFF can never render a
 * page without knowing whether it should have gated it.
 */
@RestController
@RequestMapping("/api/internal/render-context/{tenantSlug}")
public class RenderContextController {

    private final GetShopRenderContextUseCase getShopUseCase;
    private final GetExperienceListRenderContextUseCase getExperienceListUseCase;
    private final GetExperienceRenderContextUseCase getExperienceUseCase;

    public RenderContextController(GetShopRenderContextUseCase getShopUseCase,
                                   GetExperienceListRenderContextUseCase getExperienceListUseCase,
                                   GetExperienceRenderContextUseCase getExperienceUseCase) {
        this.getShopUseCase = getShopUseCase;
        this.getExperienceListUseCase = getExperienceListUseCase;
        this.getExperienceUseCase = getExperienceUseCase;
    }

    /**
     * The tenant's chrome and resolved locale, with no page content — what the
     * password gate renders from, and what resolves the tenant before anything
     * else is asked for.
     *
     * @param locale the locale from the URL prefix, omitted for the bare path
     */
    @GetMapping("/shop")
    public ResponseEntity<ShopRenderContextResponse> shop(
            @PathVariable String tenantSlug,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(
                ShopRenderContextResponse.from(getShopUseCase.execute(tenantSlug, locale)));
    }

    /** Everything the experience-list page renders. Published experiences only. */
    @GetMapping("/experience-list")
    public ResponseEntity<ExperienceListRenderContextResponse> experienceList(
            @PathVariable String tenantSlug,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(ExperienceListRenderContextResponse.from(
                getExperienceListUseCase.execute(tenantSlug, locale)));
    }

    /**
     * One experience's page. {@code experienceSlug} may be the canonical handle
     * or the localized one for this locale; an unpublished or unknown handle is
     * a 404 either way.
     */
    @GetMapping("/experience/{experienceSlug}")
    public ResponseEntity<ExperienceRenderContextResponse> experience(
            @PathVariable String tenantSlug,
            @PathVariable String experienceSlug,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(ExperienceRenderContextResponse.from(
                getExperienceUseCase.execute(tenantSlug, experienceSlug, locale)));
    }
}
