package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.usecase.GetStorefrontPasswordUseCase;
import com.vointika.touroperator.application.usecase.UpdateStorefrontPasswordUseCase;
import com.vointika.touroperator.presentation.request.UpdateStorefrontPasswordRequest;
import com.vointika.touroperator.presentation.response.StorefrontPasswordResponse;
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
 * The operator's storefront password protection — Shopify's Preferences →
 * Store access, on the operator itself (the operator IS the storefront).
 * Reads member-visible (the password is a shared gate, not a credential);
 * changes ADMIN+.
 *
 * <p><b>Nothing enforces this today.</b> The SSR gate shipped in #91 and was
 * deleted when the storefront was cut back to a placeholder — a page that
 * renders no operator data has nothing to protect. So an operator can set a
 * password and see it stored, and the storefront will not ask for it until the
 * real pages return. Recover the gate from git alongside them; the ordering rule
 * it enforced (gate before locale resolution, or a locked store leaks which
 * locales it publishes) is the part that is expensive to rediscover.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/storefront-password")
public class StorefrontPasswordController {

    private final GetStorefrontPasswordUseCase getUseCase;
    private final UpdateStorefrontPasswordUseCase updateUseCase;

    public StorefrontPasswordController(GetStorefrontPasswordUseCase getUseCase,
                                        UpdateStorefrontPasswordUseCase updateUseCase) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
    }

    /** The current settings (enabled + password + visitor message). Any member. */
    @GetMapping
    public ResponseEntity<StorefrontPasswordResponse> get(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(StorefrontPasswordResponse.from(
                getUseCase.execute(tourOperatorId, UUID.fromString(callerUserId))));
    }

    /**
     * Replaces the settings. ADMIN+. A null/blank password keeps the stored
     * one; enabling with no password at all → 422. A null/blank message
     * clears it.
     */
    @PutMapping
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @RequestBody UpdateStorefrontPasswordRequest body,
            @AuthenticationPrincipal String callerUserId) {
        updateUseCase.execute(tourOperatorId, body.enabled(), body.password(), body.message(),
                UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }
}
