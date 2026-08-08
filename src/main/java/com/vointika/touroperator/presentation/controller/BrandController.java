package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.UpdateBrandInput;
import com.vointika.touroperator.application.usecase.GetBrandUseCase;
import com.vointika.touroperator.application.usecase.UpdateBrandUseCase;
import com.vointika.touroperator.presentation.response.BrandResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The operator's brand — Shopify's brand settings area, and the substrate the
 * storefront renders on every page. Read member; write ADMIN+.
 *
 * <p><b>A settings sub-resource, not a collection</b>, so there is one per
 * operator and no id in the path: the shape {@code /seo}, {@code /locales} and
 * {@code /storefront-password} use. {@code GET} on an operator who has filled in
 * nothing returns an <b>empty brand</b>, not a 404 — that is the state most
 * operators are in and what the form should render.
 *
 * <p>{@code PUT} is a <b>full replace</b>, including the palette and the social
 * links, which are stored wholesale rather than diffed. An absent field clears
 * it; absent collections empty them. An editor sends the whole object.
 *
 * <p>This replaced {@code PUT}/{@code DELETE .../logo}, which was the brand's
 * only writer while the rest of the row was read-only. One column, one write
 * path.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/brand")
public class BrandController {

    private final GetBrandUseCase getUseCase;
    private final UpdateBrandUseCase updateUseCase;

    public BrandController(GetBrandUseCase getUseCase, UpdateBrandUseCase updateUseCase) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
    }

    @GetMapping
    public ResponseEntity<BrandResponse> get(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(BrandResponse.from(
                getUseCase.execute(tourOperatorId, UUID.fromString(userIdStr))));
    }

    @PutMapping
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @RequestBody UpdateBrandInput body,
            @AuthenticationPrincipal String userIdStr) {
        updateUseCase.execute(tourOperatorId, body, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
