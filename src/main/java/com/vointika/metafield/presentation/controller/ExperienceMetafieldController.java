package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.UpsertMetafieldValueInput;
import com.vointika.metafield.application.usecase.DeleteMetafieldValueUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldValuesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldValueUseCase;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.presentation.request.UpsertMetafieldValueRequest;
import com.vointika.metafield.presentation.response.MetafieldValueResponse;
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
 * An experience's metafield values — a thin mount of the owner-generic value
 * use cases with the EXPERIENCE owner type (its PAGE twin serves pages).
 * Reads member; writes ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/metafields")
public class ExperienceMetafieldController {

    private static final MetafieldOwnerType OWNER = MetafieldOwnerType.EXPERIENCE;

    private final ListMetafieldValuesUseCase listUseCase;
    private final UpsertMetafieldValueUseCase upsertUseCase;
    private final DeleteMetafieldValueUseCase deleteUseCase;

    public ExperienceMetafieldController(ListMetafieldValuesUseCase listUseCase,
                                         UpsertMetafieldValueUseCase upsertUseCase,
                                         DeleteMetafieldValueUseCase deleteUseCase) {
        this.listUseCase = listUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<MetafieldValueResponse>> list(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(listUseCase
                .execute(tourOperatorId, OWNER, experienceId, callerUserId).stream()
                .map(MetafieldValueResponse::from)
                .toList());
    }

    @PutMapping("/{namespace}/{key}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String namespace,
            @PathVariable String key,
            @RequestBody UpsertMetafieldValueRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        upsertUseCase.execute(new UpsertMetafieldValueInput(
                callerUserId, tourOperatorId, OWNER, experienceId,
                namespace, key, body.value()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{namespace}/{key}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @PathVariable String namespace,
            @PathVariable String key,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, OWNER, experienceId, namespace, key,
                callerUserId);
        return ResponseEntity.noContent().build();
    }
}
