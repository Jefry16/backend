package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.UpsertMetaobjectFieldTranslationsInput;
import com.vointika.metafield.application.usecase.DeleteMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectFieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetaobjectFieldTranslationsUseCase;
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
 * Per-locale overlays for one metaobject entry's field values — what makes the
 * boat's {@code notes} read English on {@code /en} once a metafield resolves it.
 *
 * <p><b>Keys are bare field keys</b>, not {@code namespace.key}: a metaobject
 * field has no namespace. The request record is shared with the metafield mounts
 * because the envelope is identical — a map of key to text — and the difference
 * is only what a key means, which the path already says.
 *
 * <p>{@code field-translations} rather than {@code translations} keeps the door
 * open for translating an entry's own {@code name} and {@code handle} later
 * without a second meaning for the same segment.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metaobjects/{metaobjectId}/field-translations")
public class MetaobjectFieldTranslationController {

    private final ListMetaobjectFieldTranslationLocalesUseCase listLocalesUseCase;
    private final GetMetaobjectFieldTranslationsUseCase getUseCase;
    private final UpsertMetaobjectFieldTranslationsUseCase upsertUseCase;
    private final DeleteMetaobjectFieldTranslationsUseCase deleteUseCase;

    public MetaobjectFieldTranslationController(
            ListMetaobjectFieldTranslationLocalesUseCase listLocalesUseCase,
            GetMetaobjectFieldTranslationsUseCase getUseCase,
            UpsertMetaobjectFieldTranslationsUseCase upsertUseCase,
            DeleteMetaobjectFieldTranslationsUseCase deleteUseCase) {
        this.listLocalesUseCase = listLocalesUseCase;
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** The locales this entry has any field translation in. Any member. */
    @GetMapping
    public ResponseEntity<List<String>> listLocales(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(listLocalesUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, metaobjectId));
    }

    /** One locale's overlay, keyed by field key. Empty object when untranslated. */
    @GetMapping("/{locale}")
    public ResponseEntity<Map<String, String>> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(getUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, metaobjectId, locale));
    }

    /** Replaces the whole locale in one write. ADMIN+. 204. */
    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @PathVariable String locale,
            @RequestBody UpsertMetafieldTranslationsRequest body,
            @AuthenticationPrincipal String callerUserId) {
        upsertUseCase.execute(new UpsertMetaobjectFieldTranslationsInput(
                UUID.fromString(callerUserId), tourOperatorId, metaobjectId,
                locale, body.values()));
        return ResponseEntity.noContent().build();
    }

    /** Drops the whole locale. ADMIN+. 204 whether or not anything was there. */
    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @PathVariable String locale,
            @AuthenticationPrincipal String callerUserId) {
        deleteUseCase.execute(
                UUID.fromString(callerUserId), tourOperatorId, metaobjectId, locale);
        return ResponseEntity.noContent().build();
    }
}
