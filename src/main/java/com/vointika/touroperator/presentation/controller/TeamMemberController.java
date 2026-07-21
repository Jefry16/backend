package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.application.usecase.ChangeMemberRoleUseCase;
import com.vointika.touroperator.application.usecase.ListMembersUseCase;
import com.vointika.touroperator.application.usecase.RemoveTeamMemberUseCase;
import com.vointika.touroperator.presentation.request.ChangeMemberRoleRequest;
import com.vointika.touroperator.presentation.response.MemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Team-management endpoints. Membership on the operator is enforced by the
 * {@code /api/tour-operators/**} interceptor (non-member → 404); the role gates
 * (member / ADMIN+ / OWNER) live in the use cases. Sits beside {@code TeamInvitationController}.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/members")
public class TeamMemberController {

    private final ListMembersUseCase listMembersUseCase;
    private final ChangeMemberRoleUseCase changeMemberRoleUseCase;
    private final RemoveTeamMemberUseCase removeTeamMemberUseCase;
    private final ListQueryParser listQueryParser;

    public TeamMemberController(ListMembersUseCase listMembersUseCase,
                                ChangeMemberRoleUseCase changeMemberRoleUseCase,
                                RemoveTeamMemberUseCase removeTeamMemberUseCase,
                                ListQueryParser listQueryParser) {
        this.listMembersUseCase = listMembersUseCase;
        this.changeMemberRoleUseCase = changeMemberRoleUseCase;
        this.removeTeamMemberUseCase = removeTeamMemberUseCase;
        this.listQueryParser = listQueryParser;
    }

    /**
     * The operator's team roster — cursor-paginated, tenant-scoped, owner first.
     * Any member may view it; a non-member is a 404. Filter by {@code role},
     * sort by {@code joinedAt} (default) or {@code id}; page with {@code cursor}.
     */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MemberResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListMembersUseCase.SCHEMA, tourOperatorId);
        CursorPage<MemberListView> page = listMembersUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(CursorPageResponse.of(page, MemberResponse::from));
    }

    /**
     * Changes a member's role. ADMIN+; promoting to OWNER is an owner-only
     * ownership transfer (the acting owner is demoted to ADMIN). 204. Self-change
     * → 409; demoting the last owner → 409; a non-owner acting on the owner → 403;
     * unknown role → 422.
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID userId,
            @RequestBody(required = false) ChangeMemberRoleRequest body,
            @AuthenticationPrincipal String callerUserId) {
        changeMemberRoleUseCase.execute(
                tourOperatorId, userId,
                body == null ? null : body.role(),
                UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a member. Self ({@code userId} == caller) = leave (any but the last
     * owner → 409); other = ADMIN+ (only an owner removes an owner → 403; last
     * owner → 409). 204.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal String callerUserId) {
        removeTeamMemberUseCase.execute(
                tourOperatorId, userId, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }
}
