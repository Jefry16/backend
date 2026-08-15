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
 * The operator's own metafield values — Shopify's {@code shop.metafields}, and
 * the same thin mount of the owner-generic use cases its experience and page
 * twins are. Reads member; writes ADMIN+.
 *
 * <p><b>There is no owner id in the path</b>, and that is the whole difference:
 * the owner <em>is</em> the tenant, so the operator id plays both parts. That
 * makes this a settings sub-resource in URL shape — one per operator, no id,
 * like {@code /brand}, {@code /seo} and {@code /locales} — rather than a
 * collection under a resource.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metafields")
public class TourOperatorMetafieldController {

    private static final MetafieldOwnerType OWNER = MetafieldOwnerType.TOUR_OPERATOR;

    private final ListMetafieldValuesUseCase listUseCase;
    private final UpsertMetafieldValueUseCase upsertUseCase;
    private final DeleteMetafieldValueUseCase deleteUseCase;

    public TourOperatorMetafieldController(ListMetafieldValuesUseCase listUseCase,
                                           UpsertMetafieldValueUseCase upsertUseCase,
                                           DeleteMetafieldValueUseCase deleteUseCase) {
        this.listUseCase = listUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<MetafieldValueResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(listUseCase
                .execute(tourOperatorId, OWNER, tourOperatorId, callerUserId).stream()
                .map(MetafieldValueResponse::from)
                .toList());
    }

    @PutMapping("/{namespace}/{key}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String namespace,
            @PathVariable String key,
            @RequestBody UpsertMetafieldValueRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        upsertUseCase.execute(new UpsertMetafieldValueInput(
                callerUserId, tourOperatorId, OWNER, tourOperatorId,
                namespace, key, body.value()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{namespace}/{key}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String namespace,
            @PathVariable String key,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, OWNER, tourOperatorId, namespace, key,
                callerUserId);
        return ResponseEntity.noContent().build();
    }
}
