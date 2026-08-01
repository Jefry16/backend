package com.vointika.rendering.presentation.controller;

import com.vointika.rendering.application.usecase.VerifyStorefrontPasswordUseCase;
import com.vointika.rendering.presentation.request.VerifyPasswordRequest;
import com.vointika.rendering.presentation.response.VerifyPasswordResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Visitor actions on a storefront that are not page renders. Separate from
 * {@code render-context} because these are relayed shopper intent, not reads of
 * a page — the cart and contact-form relays will join this family.
 *
 * <p>Same shared-secret gate: the visitor's browser posts to the BFF, and the
 * BFF posts here.
 */
@RestController
@RequestMapping("/api/storefront/{tenantSlug}")
public class StorefrontController {

    private final VerifyStorefrontPasswordUseCase verifyPasswordUseCase;

    public StorefrontController(VerifyStorefrontPasswordUseCase verifyPasswordUseCase) {
        this.verifyPasswordUseCase = verifyPasswordUseCase;
    }

    /**
     * Checks a storefront password. Always 200 — {@code verified:false} covers a
     * wrong password, a gate that is not enabled, and a tenant that does not
     * exist alike, so the endpoint reveals nothing but the answer to the guess.
     */
    @PostMapping("/verify-password")
    public ResponseEntity<VerifyPasswordResponse> verifyPassword(
            @PathVariable String tenantSlug,
            @RequestBody VerifyPasswordRequest request) {
        return ResponseEntity.ok(new VerifyPasswordResponse(
                verifyPasswordUseCase.execute(tenantSlug, request.password())));
    }
}
