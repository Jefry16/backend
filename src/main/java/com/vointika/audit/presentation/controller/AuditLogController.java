package com.vointika.audit.presentation.controller;

import com.vointika.audit.application.usecase.GetAuditLogEntryUseCase;
import com.vointika.audit.application.usecase.ListAuditLogUseCase;
import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.audit.presentation.response.AuditLogEntryResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-facing audit trail — the operator's action history across entities.
 * Read-only: entries are written by the mutating use cases through
 * {@code AuditTrailPort}, never through this API. Membership enforced by the
 * {@code /api/tour-operators/**} interceptor plus the use cases' ensureMember
 * (non-member → 404); any member may read.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/audit-log")
public class AuditLogController {

    private final ListAuditLogUseCase listAuditLogUseCase;
    private final GetAuditLogEntryUseCase getAuditLogEntryUseCase;
    private final ListQueryParser listQueryParser;

    public AuditLogController(ListAuditLogUseCase listAuditLogUseCase,
                              GetAuditLogEntryUseCase getAuditLogEntryUseCase,
                              ListQueryParser listQueryParser) {
        this.listAuditLogUseCase = listAuditLogUseCase;
        this.getAuditLogEntryUseCase = getAuditLogEntryUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's trail — cursor-paginated, newest first. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<AuditLogEntryResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListAuditLogUseCase.SCHEMA, tourOperatorId);
        CursorPage<AuditLogListItem> page =
                listAuditLogUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, AuditLogEntryResponse::from));
    }

    /** A single entry by id (a list row's shape). Any member; cross-tenant → 404. */
    @GetMapping("/{entryId}")
    public ResponseEntity<AuditLogEntryResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(AuditLogEntryResponse.from(
                getAuditLogEntryUseCase.execute(tourOperatorId, entryId, callerUserId)));
    }
}
