package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.UpsertOperatorTranslationInput;
import com.vointika.touroperator.application.usecase.DeleteOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListOperatorTranslationsUseCase;
import com.vointika.touroperator.application.usecase.UpsertOperatorTranslationUseCase;
import com.vointika.touroperator.presentation.response.OperatorTranslationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Per-locale translation overlays for the operator itself — the operator text every
 * page type falls back to. Reads are member-visible; upsert/delete are ADMIN+
 * (membership enforced by the {@code /api/tour-operators/**} interceptor). A
 * translation's locale must be one the operator supports (else 422).
 *
 * <p>Mirrors {@code ExperienceTranslationController} and
 * {@code PageTranslationController}, minus the nested resource id: the operator
 * <em>is</em> the entity here, so there is no segment between the tenant and
 * {@code /translations}.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/translations")
public class OperatorTranslationController {

    private final UpsertOperatorTranslationUseCase upsertUseCase;
    private final GetOperatorTranslationUseCase getUseCase;
    private final ListOperatorTranslationsUseCase listUseCase;
    private final DeleteOperatorTranslationUseCase deleteUseCase;

    public OperatorTranslationController(UpsertOperatorTranslationUseCase upsertUseCase,
                                         GetOperatorTranslationUseCase getUseCase,
                                         ListOperatorTranslationsUseCase listUseCase,
                                         DeleteOperatorTranslationUseCase deleteUseCase) {
        this.upsertUseCase = upsertUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** All translated locales for the operator. Any member. */
    @GetMapping
    public ResponseEntity<List<OperatorTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        List<OperatorTranslationResponse> body = listUseCase
                .execute(tourOperatorId, UUID.fromString(userIdStr)).stream()
                .map(OperatorTranslationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** One locale's overlay (empty overlay if untranslated). Any member. */
    @GetMapping("/{locale}")
    public ResponseEntity<OperatorTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(OperatorTranslationResponse.from(
                getUseCase.execute(tourOperatorId, locale, UUID.fromString(userIdStr))));
    }

    /** Creates or replaces a locale's overlay. ADMIN+. Unsupported locale → 422. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @RequestBody UpsertOperatorTranslationInput body,
            @AuthenticationPrincipal String userIdStr) {
        upsertUseCase.execute(tourOperatorId, locale, body, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    /** Removes a locale's overlay. ADMIN+. Idempotent. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal String userIdStr) {
        deleteUseCase.execute(tourOperatorId, locale, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
