package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.UpsertPolicyTranslationInput;
import com.vointika.touroperator.application.usecase.DeletePolicyTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListPolicyTranslationsUseCase;
import com.vointika.touroperator.application.usecase.UpsertPolicyTranslationUseCase;
import com.vointika.touroperator.presentation.response.PolicyTranslationResponse;
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
 * Per-locale overlays on one policy. Reads member; writes ADMIN+.
 *
 * <p>Mirrors {@code ExperienceTranslationController} and
 * {@code PageTranslationController} — the nested resource here is the policy,
 * keyed by its type rather than by an id.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/policies/{type}/translations")
public class PolicyTranslationController {

    private final ListPolicyTranslationsUseCase listUseCase;
    private final UpsertPolicyTranslationUseCase upsertUseCase;
    private final DeletePolicyTranslationUseCase deleteUseCase;

    public PolicyTranslationController(ListPolicyTranslationsUseCase listUseCase,
                                       UpsertPolicyTranslationUseCase upsertUseCase,
                                       DeletePolicyTranslationUseCase deleteUseCase) {
        this.listUseCase = listUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<PolicyTranslationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(listUseCase.execute(tourOperatorId, type, UUID.fromString(userIdStr))
                .stream().map(PolicyTranslationResponse::from).toList());
    }

    @PutMapping("/{locale}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @PathVariable String locale,
            @RequestBody UpsertPolicyTranslationInput body,
            @AuthenticationPrincipal String userIdStr) {
        upsertUseCase.execute(tourOperatorId, type, locale, body, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{locale}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @PathVariable String locale,
            @AuthenticationPrincipal String userIdStr) {
        deleteUseCase.execute(tourOperatorId, type, locale, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
