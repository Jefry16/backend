package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.output.OperatorLocalesView;
import com.vointika.touroperator.application.usecase.GetOperatorLocalesUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorLocalesUseCase;
import com.vointika.touroperator.presentation.request.UpdateOperatorLocalesRequest;
import com.vointika.touroperator.presentation.response.OperatorLocalesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The operator's content languages — its default/primary locale + supported set,
 * the master-list-validated basis experience translations and the storefront
 * build on. Reads are member-visible; changes are ADMIN+ (membership enforced by
 * the {@code /api/tour-operators/**} interceptor).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/locales")
public class TourOperatorLocalesController {

    private final GetOperatorLocalesUseCase getOperatorLocalesUseCase;
    private final UpdateOperatorLocalesUseCase updateOperatorLocalesUseCase;

    public TourOperatorLocalesController(GetOperatorLocalesUseCase getOperatorLocalesUseCase,
                                         UpdateOperatorLocalesUseCase updateOperatorLocalesUseCase) {
        this.getOperatorLocalesUseCase = getOperatorLocalesUseCase;
        this.updateOperatorLocalesUseCase = updateOperatorLocalesUseCase;
    }

    /** The operator's primary locale + supported set. Any member. */
    @GetMapping
    public ResponseEntity<OperatorLocalesResponse> get(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        OperatorLocalesView view = getOperatorLocalesUseCase.execute(tourOperatorId, UUID.fromString(userIdStr));
        return ResponseEntity.ok(OperatorLocalesResponse.from(view));
    }

    /** Replaces the operator's languages. ADMIN+. Unknown code or primary∉supported → 422. */
    @PatchMapping
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @RequestBody UpdateOperatorLocalesRequest body,
            @AuthenticationPrincipal String userIdStr) {
        updateOperatorLocalesUseCase.execute(
                tourOperatorId,
                body.primaryLocale(),
                body.supportedLocales(),
                UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
