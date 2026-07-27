package com.vointika.page.presentation.controller;

import com.vointika.page.application.dto.input.UpsertPageTranslationInput;
import com.vointika.page.application.usecase.DeletePageTranslationUseCase;
import com.vointika.page.application.usecase.GetPageTranslationUseCase;
import com.vointika.page.application.usecase.ListPageTranslationsUseCase;
import com.vointika.page.application.usecase.UpsertPageTranslationUseCase;
import com.vointika.page.presentation.request.UpsertPageTranslationRequest;
import com.vointika.page.presentation.response.PageTranslationResponse;
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
 * Per-locale translation overlays for a page — the experience-translations
 * surface: list, get one (empty overlay when untranslated), PUT upsert,
 * DELETE (idempotent). Reads member; writes ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/pages/{pageId}/translations")
public class PageTranslationController {

    private final ListPageTranslationsUseCase listUseCase;
    private final GetPageTranslationUseCase getUseCase;
    private final UpsertPageTranslationUseCase upsertUseCase;
    private final DeletePageTranslationUseCase deleteUseCase;

    public PageTranslationController(ListPageTranslationsUseCase listUseCase,
                                     GetPageTranslationUseCase getUseCase,
                                     UpsertPageTranslationUseCase upsertUseCase,
                                     DeletePageTranslationUseCase deleteUseCase) {
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<PageTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(listUseCase
                .execute(tourOperatorId, pageId, UUID.fromString(callerUserId)).stream()
                .map(PageTranslationResponse::from)
                .toList());
    }

    @GetMapping("/{locale}")
    public ResponseEntity<PageTranslationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(PageTranslationResponse.from(
                getUseCase.execute(tourOperatorId, pageId, locale, UUID.fromString(callerUserId))));
    }

    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @PathVariable String locale,
            @RequestBody UpsertPageTranslationRequest body,
            @AuthenticationPrincipal String callerUserId) {
        upsertUseCase.execute(new UpsertPageTranslationInput(
                UUID.fromString(callerUserId), tourOperatorId, pageId, locale,
                body.title(), body.body(), body.seoTitle(), body.seoDescription(), body.slug()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        deleteUseCase.execute(tourOperatorId, pageId, locale, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }
}
