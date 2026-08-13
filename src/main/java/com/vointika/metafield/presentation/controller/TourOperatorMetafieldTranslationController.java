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
 * Per-locale overlays for the operator's own metafield values — what {@code tourOperator.metafields} serves in a secondary locale.
 *
 * <p><b>The path is {@code metafield-translations}, not
 * {@code metafields/translations}.</b> The latter collides with
 * {@code /metafields/{namespace}/{key}} — {@code PathPattern} prefers the literal
 * so it would resolve, but it silently makes a namespace called
 * {@code translations} unreachable, and a route that works by tie-break is one
 * nobody remembers is fragile.
 *
 * <p>Reads are any member; writes are ADMIN+, enforced in the use case rather
 * than here (LAW: authorization belongs in the use case, not only the router).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metafield-translations")
public class TourOperatorMetafieldTranslationController {

    private static final MetafieldOwnerType OWNER = MetafieldOwnerType.TOUR_OPERATOR;

    private final ListMetafieldTranslationLocalesUseCase listLocalesUseCase;
    private final GetMetafieldTranslationsUseCase getUseCase;
    private final UpsertMetafieldTranslationsUseCase upsertUseCase;
    private final DeleteMetafieldTranslationsUseCase deleteUseCase;

    public TourOperatorMetafieldTranslationController(ListMetafieldTranslationLocalesUseCase listLocalesUseCase,
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
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(listLocalesUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, OWNER, tourOperatorId));
    }

    /** One locale's overlay, keyed {@code namespace.key}. Empty map when untranslated. */
    @GetMapping("/{locale}")
    public ResponseEntity<Map<String, String>> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(getUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, OWNER, tourOperatorId, locale));
    }

    /** Replaces the whole locale in one write. ADMIN+. 204. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @RequestBody UpsertMetafieldTranslationsRequest body,
            @AuthenticationPrincipal String callerUserId) {
        upsertUseCase.execute(new UpsertMetafieldTranslationsInput(
                UUID.fromString(callerUserId), tourOperatorId, OWNER, tourOperatorId,
                locale, body.values()));
        return ResponseEntity.noContent().build();
    }

    /** Drops the whole locale. ADMIN+. 204 whether or not anything was there. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        deleteUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, OWNER, tourOperatorId, locale);
        return ResponseEntity.noContent().build();
    }
}
