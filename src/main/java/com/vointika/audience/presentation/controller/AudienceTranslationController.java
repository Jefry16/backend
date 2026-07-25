package com.vointika.audience.presentation.controller;

import com.vointika.audience.application.usecase.DeleteAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.GetAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.ListAudienceTranslationsUseCase;
import com.vointika.audience.application.usecase.UpsertAudienceTranslationUseCase;
import com.vointika.audience.presentation.request.UpsertAudienceTranslationRequest;
import com.vointika.audience.presentation.response.AudienceTranslationResponse;
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
 * Per-locale name translations for an audience. Reads are member-visible;
 * upsert/delete are ADMIN+ (membership enforced by the
 * {@code /api/tour-operators/**} interceptor). A translation's locale must be
 * one the operator supports (else 422).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/audiences/{audienceId}/translations")
public class AudienceTranslationController {

    private final UpsertAudienceTranslationUseCase upsertUseCase;
    private final GetAudienceTranslationUseCase getUseCase;
    private final ListAudienceTranslationsUseCase listUseCase;
    private final DeleteAudienceTranslationUseCase deleteUseCase;

    public AudienceTranslationController(UpsertAudienceTranslationUseCase upsertUseCase,
                                         GetAudienceTranslationUseCase getUseCase,
                                         ListAudienceTranslationsUseCase listUseCase,
                                         DeleteAudienceTranslationUseCase deleteUseCase) {
        this.upsertUseCase = upsertUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** All translated locales for the audience. Any member. */
    @GetMapping
    public ResponseEntity<List<AudienceTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @AuthenticationPrincipal String callerUserId) {
        List<AudienceTranslationResponse> body = listUseCase
                .execute(tourOperatorId, audienceId, UUID.fromString(callerUserId)).stream()
                .map(AudienceTranslationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** One locale's overlay (empty overlay if untranslated). Any member. */
    @GetMapping("/{locale}")
    public ResponseEntity<AudienceTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        var view = getUseCase.execute(tourOperatorId, audienceId, locale, UUID.fromString(callerUserId));
        return ResponseEntity.ok(AudienceTranslationResponse.from(view));
    }

    /** Creates or replaces a locale's overlay. ADMIN+. Unsupported locale → 422. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @PathVariable String locale,
            @RequestBody UpsertAudienceTranslationRequest body,
            @AuthenticationPrincipal String callerUserId) {
        upsertUseCase.execute(tourOperatorId, audienceId, locale,
                body == null ? null : body.name(), UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }

    /** Removes a locale's overlay. ADMIN+. Idempotent. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        deleteUseCase.execute(tourOperatorId, audienceId, locale, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }
}
