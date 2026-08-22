package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.CreatePolicyInput;
import com.vointika.touroperator.application.dto.input.UpdatePolicyInput;
import com.vointika.touroperator.application.usecase.DeletePolicyUseCase;
import com.vointika.touroperator.application.usecase.GetPolicyUseCase;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.application.usecase.CreatePolicyUseCase;
import com.vointika.touroperator.application.usecase.UpdatePolicyUseCase;
import com.vointika.touroperator.presentation.response.PolicyResponse;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The operator's four store policies. Reads member; writes ADMIN+ (membership
 * enforced by the {@code /api/tour-operators/**} interceptor).
 *
 * <p><b>Addressed by id</b>, like every other resource in this API: {@code POST}
 * creates, {@code GET}/{@code PUT}/{@code DELETE} take the policy's id. The
 * {@code type} is chosen once, in the create body, and is immutable afterwards —
 * it is the document's address <em>on the storefront</em>, so moving it is a
 * delete and a create. One per type is a UNIQUE constraint, so a repeat is a
 * <b>409</b>.
 *
 * <p>Every id lookup is <b>tenant-scoped</b>: the membership interceptor proves
 * the caller belongs to the operator in the path, not that the id does, so the
 * query binds the two. A policy id from another operator is a 404.
 *
 * <p>The enum name ({@code LEGAL_NOTICE}) is what the body carries, matching how
 * this context writes {@code linkType} and {@code role}. The hyphenated slug
 * ({@code legal-notice}) is a public-URL concern and lives in the storefront's
 * {@code PolicySlug}, which owns the transform both ways. <b>The split is the
 * point</b>: this API never speaks the slug, and the storefront never speaks the
 * enum — it cannot, since it may not import {@code PolicyType}, which is why an
 * unknown slug becomes a name no constant has rather than a validation error.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/policies")
public class PolicyController {

    private final ListPoliciesUseCase listUseCase;
    private final ListQueryParser listQueryParser;
    private final GetPolicyUseCase getUseCase;
    private final CreatePolicyUseCase createUseCase;
    private final UpdatePolicyUseCase updateUseCase;
    private final DeletePolicyUseCase deleteUseCase;

    public PolicyController(ListPoliciesUseCase listUseCase,
                            GetPolicyUseCase getUseCase,
                            CreatePolicyUseCase createUseCase,
                            UpdatePolicyUseCase updateUseCase,
                            DeletePolicyUseCase deleteUseCase,
                            ListQueryParser listQueryParser) {
        this.listUseCase = listUseCase;
        this.listQueryParser = listQueryParser;
        this.getUseCase = getUseCase;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    /**
     * Every policy the operator has written, through the shared cursor framework
     * — {@code ?filter[type][in]=TERMS&sort=-updatedAt}. Four rows will never
     * paginate, but this is tenant data and speaks the same grammar as every
     * other tenant list (PATTERNS §4b).
     */
    @GetMapping
    public ResponseEntity<CursorPageResponse<PolicyResponse>> list(
            @PathVariable UUID tourOperatorId,
            HttpServletRequest request,
            @AuthenticationPrincipal UUID userId) {
        ListQuery query = listQueryParser.parse(
                request, ListPoliciesUseCase.SCHEMA, tourOperatorId);
        return ResponseEntity.ok(CursorPageResponse.of(
                listUseCase.execute(query, userId), PolicyResponse::from));
    }

    /** Writes a policy the operator has not written yet. ADMIN+. 409 on a repeat type. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreatePolicyInput body,
            @AuthenticationPrincipal UUID userId) {
        UUID id = createUseCase.execute(tourOperatorId, body, userId);
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/policies/" + id))
                .build();
    }

    /** One policy. An id from another operator is a 404, not a document. */
    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(PolicyResponse.from(
                getUseCase.execute(tourOperatorId, policyId, userId)));
    }

    /** Replaces the policy's text. The type is not settable — it is the address. */
    @PutMapping("/{policyId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID policyId,
            @RequestBody UpdatePolicyInput body,
            @AuthenticationPrincipal UUID userId) {
        updateUseCase.execute(tourOperatorId, policyId, body, userId);
        return ResponseEntity.noContent().build();
    }

    /** Unpublishes the policy by removing it. ADMIN+. Idempotent. */
    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UUID userId) {
        deleteUseCase.execute(tourOperatorId, policyId, userId);
        return ResponseEntity.noContent().build();
    }
}
