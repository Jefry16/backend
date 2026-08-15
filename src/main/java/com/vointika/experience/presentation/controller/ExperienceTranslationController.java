package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.dto.input.UpsertExperienceTranslationInput;
import com.vointika.experience.application.usecase.DeleteExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.GetExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.ListExperienceTranslationsUseCase;
import com.vointika.experience.application.usecase.UpsertExperienceTranslationUseCase;
import com.vointika.experience.presentation.response.ExperienceTranslationResponse;
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
 * Per-locale translation overlays for an experience. Reads are member-visible;
 * upsert/delete are ADMIN+ (membership enforced by the {@code /api/tour-operators/**}
 * interceptor). A translation's locale must be one the operator supports (else 422).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/translations")
public class ExperienceTranslationController {

    private final UpsertExperienceTranslationUseCase upsertUseCase;
    private final GetExperienceTranslationUseCase getUseCase;
    private final ListExperienceTranslationsUseCase listUseCase;
    private final DeleteExperienceTranslationUseCase deleteUseCase;

    public ExperienceTranslationController(UpsertExperienceTranslationUseCase upsertUseCase,
                                           GetExperienceTranslationUseCase getUseCase,
                                           ListExperienceTranslationsUseCase listUseCase,
                                           DeleteExperienceTranslationUseCase deleteUseCase) {
        this.upsertUseCase = upsertUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** All translated locales for the experience. Any member. */
    @GetMapping
    public ResponseEntity<List<ExperienceTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal UUID userId) {
        List<ExperienceTranslationResponse> body = listUseCase
                .execute(tourOperatorId, experienceId, userId).stream()
                .map(ExperienceTranslationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** One locale's overlay (empty overlay if untranslated). Any member. */
    @GetMapping("/{locale}")
    public ResponseEntity<ExperienceTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID userId) {
        var view = getUseCase.execute(tourOperatorId, experienceId, locale, userId);
        return ResponseEntity.ok(ExperienceTranslationResponse.from(view));
    }

    /**
     * Creates or replaces a locale's overlay. ADMIN+. Unsupported locale → 422; a handle
     * already used by another experience — localized in this locale, or canonical → 409.
     */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @RequestBody UpsertExperienceTranslationInput body,
            @AuthenticationPrincipal UUID userId) {
        upsertUseCase.execute(tourOperatorId, experienceId, locale, body, userId);
        return ResponseEntity.noContent().build();
    }

    /** Removes a locale's overlay. ADMIN+. Idempotent. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID userId) {
        deleteUseCase.execute(tourOperatorId, experienceId, locale, userId);
        return ResponseEntity.noContent().build();
    }
}
