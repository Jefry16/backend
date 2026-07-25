package com.vointika.audience.presentation.controller;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.application.usecase.CreateAudienceUseCase;
import com.vointika.audience.application.usecase.GetAudienceUseCase;
import com.vointika.audience.application.usecase.ListAudiencesUseCase;
import com.vointika.audience.application.usecase.UpdateAudienceUseCase;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.presentation.request.AudienceRequest;
import com.vointika.audience.presentation.response.AudienceResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * An operator's audiences (its pax pricing tiers). Membership on the operator is
 * enforced by the {@code /api/tour-operators/**} interceptor (non-member → 404);
 * reads are member-visible, writes (create/update/delete) are ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/audiences")
public class AudienceController {

    private final CreateAudienceUseCase createAudienceUseCase;
    private final UpdateAudienceUseCase updateAudienceUseCase;
    private final ListAudiencesUseCase listAudiencesUseCase;
    private final GetAudienceUseCase getAudienceUseCase;
    private final ListQueryParser listQueryParser;

    public AudienceController(CreateAudienceUseCase createAudienceUseCase,
                             UpdateAudienceUseCase updateAudienceUseCase,
                             ListAudiencesUseCase listAudiencesUseCase,
                             GetAudienceUseCase getAudienceUseCase,
                             ListQueryParser listQueryParser) {
        this.createAudienceUseCase = createAudienceUseCase;
        this.updateAudienceUseCase = updateAudienceUseCase;
        this.listAudiencesUseCase = listAudiencesUseCase;
        this.getAudienceUseCase = getAudienceUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's audiences — cursor-paginated. Any member; filter name/paxPerUnit, sort createdAt/name/paxPerUnit/id. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<AudienceResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListAudiencesUseCase.SCHEMA, tourOperatorId);
        CursorPage<Audience> page = listAudiencesUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(CursorPageResponse.of(page, AudienceResponse::from));
    }

    /** A single audience. Any member; 404 if not under this operator. */
    @GetMapping("/{audienceId}")
    public ResponseEntity<AudienceResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @AuthenticationPrincipal String callerUserId) {
        Audience audience = getAudienceUseCase.execute(tourOperatorId, audienceId, UUID.fromString(callerUserId));
        return ResponseEntity.ok(AudienceResponse.from(audience));
    }

    /** Creates an audience. ADMIN+. 201 + Location. Duplicate name → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody AudienceRequest body,
            @AuthenticationPrincipal String callerUserId) {
        UUID id = createAudienceUseCase.execute(
                tourOperatorId, UUID.fromString(callerUserId), toInput(body));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/audiences/" + id))
                .build();
    }

    /** Updates an audience's name + pax-per-unit. ADMIN+. 204. Duplicate name → 409. */
    @PatchMapping("/{audienceId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID audienceId,
            @RequestBody AudienceRequest body,
            @AuthenticationPrincipal String callerUserId) {
        updateAudienceUseCase.execute(
                tourOperatorId, audienceId, UUID.fromString(callerUserId), toInput(body));
        return ResponseEntity.noContent().build();
    }

    private static AudienceInput toInput(AudienceRequest b) {
        return new AudienceInput(b.name(), b.paxPerUnit());
    }
}
