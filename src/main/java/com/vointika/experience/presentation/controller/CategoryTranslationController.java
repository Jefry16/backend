package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.usecase.DeleteCategoryTranslationUseCase;
import com.vointika.experience.application.usecase.GetCategoryTranslationUseCase;
import com.vointika.experience.application.usecase.ListCategoryTranslationsUseCase;
import com.vointika.experience.application.usecase.UpsertCategoryTranslationUseCase;
import com.vointika.experience.presentation.request.UpsertCategoryTranslationRequest;
import com.vointika.experience.presentation.response.CategoryTranslationResponse;
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
 * Per-locale name translations for a category. Reads are member-visible;
 * upsert/delete are ADMIN+ (membership enforced by the
 * {@code /api/tour-operators/**} interceptor). A translation's locale must be one
 * the operator supports (else 422).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/categories/{categoryId}/translations")
public class CategoryTranslationController {

    private final UpsertCategoryTranslationUseCase upsertUseCase;
    private final GetCategoryTranslationUseCase getUseCase;
    private final ListCategoryTranslationsUseCase listUseCase;
    private final DeleteCategoryTranslationUseCase deleteUseCase;

    public CategoryTranslationController(UpsertCategoryTranslationUseCase upsertUseCase,
                                         GetCategoryTranslationUseCase getUseCase,
                                         ListCategoryTranslationsUseCase listUseCase,
                                         DeleteCategoryTranslationUseCase deleteUseCase) {
        this.upsertUseCase = upsertUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** All translated locales for the category. Any member. */
    @GetMapping
    public ResponseEntity<List<CategoryTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UUID callerUserId) {
        List<CategoryTranslationResponse> body = listUseCase
                .execute(tourOperatorId, categoryId, callerUserId).stream()
                .map(CategoryTranslationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** One locale's overlay (empty overlay if untranslated). Any member. */
    @GetMapping("/{locale}")
    public ResponseEntity<CategoryTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID callerUserId) {
        var view = getUseCase.execute(tourOperatorId, categoryId, locale, callerUserId);
        return ResponseEntity.ok(CategoryTranslationResponse.from(view));
    }

    /** Creates or replaces a locale's overlay. ADMIN+. Unsupported locale → 422. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @PathVariable String locale,
            @RequestBody UpsertCategoryTranslationRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        upsertUseCase.execute(tourOperatorId, categoryId, locale, body.name(), callerUserId);
        return ResponseEntity.noContent().build();
    }

    /** Removes a locale's overlay. ADMIN+. Idempotent. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, categoryId, locale, callerUserId);
        return ResponseEntity.noContent().build();
    }
}
