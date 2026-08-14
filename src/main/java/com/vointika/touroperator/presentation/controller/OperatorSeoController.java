package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.usecase.GetOperatorSeoUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorSeoUseCase;
import com.vointika.touroperator.presentation.request.UpdateOperatorSeoRequest;
import com.vointika.touroperator.presentation.response.OperatorSeoResponse;
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
 * The operator's canonical SEO defaults — the last fallback for every storefront
 * page, and the home page's only title source besides the operator name. Reads
 * member-visible; changes ADMIN+.
 *
 * <p>A settings sub-resource like {@code /locales} and
 * {@code /storefront-password}: the operator has no general update endpoint, and
 * these fields are configuration rather than the tenant's identity.
 * Per-locale overrides live under {@code /translations}.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/seo")
public class OperatorSeoController {

    private final GetOperatorSeoUseCase getUseCase;
    private final UpdateOperatorSeoUseCase updateUseCase;

    public OperatorSeoController(GetOperatorSeoUseCase getUseCase,
                                 UpdateOperatorSeoUseCase updateUseCase) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
    }

    /** The current canonical defaults. Any member. */
    @GetMapping
    public ResponseEntity<OperatorSeoResponse> get(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(OperatorSeoResponse.from(
                getUseCase.execute(tourOperatorId, UUID.fromString(userIdStr))));
    }

    /**
     * Replaces them wholesale. ADMIN+. A null field clears that override; an
     * {@code ogImageMediaId} outside this operator's library → 422.
     */
    @PutMapping
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @RequestBody UpdateOperatorSeoRequest body,
            @AuthenticationPrincipal String userIdStr) {
        updateUseCase.execute(tourOperatorId, body.seoTitle(), body.seoDescription(),
                body.ogImageMediaId(), UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
