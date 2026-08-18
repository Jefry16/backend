package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.UpsertMetafieldTranslationsInput;
import com.vointika.metafield.application.usecase.DeleteMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldTranslationsUseCase;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.presentation.request.UpsertMetafieldTranslationsRequest;
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
import java.util.Map;
import java.util.UUID;

/**
 * Per-locale overlays for one experience's metafield values.
 *
 * <p>{@code metafield-translations} rather than {@code metafields/translations},
 * which would collide with {@code /metafields/{namespace}/{key}} — PATTERNS §11.
 * Reads any member, writes ADMIN+, both gated in the use case.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/metafield-translations")
public class ExperienceMetafieldTranslationController {

    private static final MetafieldOwnerType OWNER = MetafieldOwnerType.EXPERIENCE;

    private final ListMetafieldTranslationLocalesUseCase listLocalesUseCase;
    private final GetMetafieldTranslationsUseCase getUseCase;
    private final UpsertMetafieldTranslationsUseCase upsertUseCase;
    private final DeleteMetafieldTranslationsUseCase deleteUseCase;

    public ExperienceMetafieldTranslationController(ListMetafieldTranslationLocalesUseCase listLocalesUseCase,
                                           GetMetafieldTranslationsUseCase getUseCase,
                                           UpsertMetafieldTranslationsUseCase upsertUseCase,
                                           DeleteMetafieldTranslationsUseCase deleteUseCase) {
        this.listLocalesUseCase = listLocalesUseCase;
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** The locales this owner has any translation in. Any member. */
    @GetMapping
    public ResponseEntity<List<String>> listLocales(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(listLocalesUseCase.execute(
                callerUserId, tourOperatorId, OWNER, experienceId));
    }

    /** One locale's overlay, keyed {@code namespace.key}. Empty map when untranslated. */
    @GetMapping("/{locale}")
    public ResponseEntity<Map<String, String>> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(getUseCase.execute(
                callerUserId, tourOperatorId, OWNER, experienceId, locale));
    }

    /** Replaces the whole locale in one write. ADMIN+. 204. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @RequestBody UpsertMetafieldTranslationsRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        upsertUseCase.execute(new UpsertMetafieldTranslationsInput(
                callerUserId, tourOperatorId, OWNER, experienceId,
                locale, body.values()));
        return ResponseEntity.noContent().build();
    }

    /** Drops the whole locale. ADMIN+. 204 whether or not anything was there. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String locale,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(
                callerUserId, tourOperatorId, OWNER, experienceId, locale);
        return ResponseEntity.noContent().build();
    }
}
