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
            @AuthenticationPrincipal UUID userId) {
        List<OperatorTranslationResponse> body = listUseCase
                .execute(tourOperatorId, userId).stream()
                .map(OperatorTranslationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** One locale's overlay (empty overlay if untranslated). Any member. */
    @GetMapping("/{locale}")
    public ResponseEntity<OperatorTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(OperatorTranslationResponse.from(
                getUseCase.execute(tourOperatorId, locale, userId)));
    }

    /** Creates or replaces a locale's overlay. ADMIN+. Unsupported locale → 422. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @RequestBody UpsertOperatorTranslationInput body,
            @AuthenticationPrincipal UUID userId) {
        upsertUseCase.execute(tourOperatorId, locale, body, userId);
        return ResponseEntity.noContent().build();
    }

    /** Removes a locale's overlay. ADMIN+. Idempotent. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID userId) {
        deleteUseCase.execute(tourOperatorId, locale, userId);
        return ResponseEntity.noContent().build();
    }
}
