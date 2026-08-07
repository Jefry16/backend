package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.UpsertPolicyInput;
import com.vointika.touroperator.application.usecase.DeletePolicyUseCase;
import com.vointika.touroperator.application.usecase.GetPolicyUseCase;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.application.usecase.UpsertPolicyUseCase;
import com.vointika.touroperator.presentation.response.PolicyResponse;
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
 * The operator's four store policies. Reads member; writes ADMIN+ (membership
 * enforced by the {@code /api/tour-operators/**} interceptor).
 *
 * <p><b>The type is the address</b>, so there is no create/rename pair and no id
 * in the path: {@code PUT} writes the policy of that type, creating it if it did
 * not exist, and {@code DELETE} takes it off the storefront. {@code type} is the
 * enum name ({@code LEGAL_NOTICE}), matching how this context writes
 * {@code linkType} and {@code role}; the storefront's hyphenated slug is a
 * public-URL concern that {@code PolicySlug} owns and does not belong here.
 *
 * <p>A name no type is called is a <b>404</b>, never {@code valueOf}'s
 * {@code IllegalArgumentException}.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/policies")
public class PolicyController {

    private final ListPoliciesUseCase listUseCase;
    private final GetPolicyUseCase getUseCase;
    private final UpsertPolicyUseCase upsertUseCase;
    private final DeletePolicyUseCase deleteUseCase;

    public PolicyController(ListPoliciesUseCase listUseCase,
                            GetPolicyUseCase getUseCase,
                            UpsertPolicyUseCase upsertUseCase,
                            DeletePolicyUseCase deleteUseCase) {
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /** Every policy the operator has written. Bounded at four, so a plain array. */
    @GetMapping
    public ResponseEntity<List<PolicyResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(listUseCase.execute(tourOperatorId, UUID.fromString(userIdStr))
                .stream().map(PolicyResponse::from).toList());
    }

    /** One policy; an unwritten one is a 404, the way the storefront serves it. */
    @GetMapping("/{type}")
    public ResponseEntity<PolicyResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(PolicyResponse.from(
                getUseCase.execute(tourOperatorId, type, UUID.fromString(userIdStr))));
    }

    /** Writes the policy of that type, creating it if absent. ADMIN+. */
    @PutMapping("/{type}")
    public ResponseEntity<Void> upsert(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @RequestBody UpsertPolicyInput body,
            @AuthenticationPrincipal String userIdStr) {
        upsertUseCase.execute(tourOperatorId, type, body, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    /** Unpublishes the policy by removing it. ADMIN+. Idempotent. */
    @DeleteMapping("/{type}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable String type,
            @AuthenticationPrincipal String userIdStr) {
        deleteUseCase.execute(tourOperatorId, type, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
