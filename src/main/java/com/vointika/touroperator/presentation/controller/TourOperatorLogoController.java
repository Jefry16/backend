package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.usecase.ClearOperatorLogoUseCase;
import com.vointika.touroperator.application.usecase.SetOperatorLogoUseCase;
import com.vointika.touroperator.presentation.request.SetOperatorLogoRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The operator's logo — a reference to one of its media records. ADMIN+ only
 * (membership enforced by the {@code /api/tour-operators/**} interceptor). The
 * resolved logo URL surfaces on the operator (the profile's operator switcher).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/logo")
public class TourOperatorLogoController {

    private final SetOperatorLogoUseCase setOperatorLogoUseCase;
    private final ClearOperatorLogoUseCase clearOperatorLogoUseCase;

    public TourOperatorLogoController(SetOperatorLogoUseCase setOperatorLogoUseCase,
                                      ClearOperatorLogoUseCase clearOperatorLogoUseCase) {
        this.setOperatorLogoUseCase = setOperatorLogoUseCase;
        this.clearOperatorLogoUseCase = clearOperatorLogoUseCase;
    }

    /** Sets the logo to a media id from this operator's library. Unknown media → 422. */
    @PutMapping
    public ResponseEntity<Void> set(
            @PathVariable UUID tourOperatorId,
            @RequestBody SetOperatorLogoRequest body,
            @AuthenticationPrincipal String userIdStr) {
        setOperatorLogoUseCase.execute(
                tourOperatorId, body == null ? null : body.mediaId(), UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    /** Removes the logo. Idempotent. */
    @DeleteMapping
    public ResponseEntity<Void> clear(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        clearOperatorLogoUseCase.execute(tourOperatorId, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
