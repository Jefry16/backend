package com.vointika.touroperator.application.usecase;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Lists a tour operator's invitations — ALL statuses (PENDING / ACCEPTED /
 * REVOKED), cursor-paginated via the shared list framework and tenant-scoped to
 * the operator. **Any member** may view (read-only, like the roster; the
 * mutating actions invite/resend/revoke stay ADMIN+); a non-member is a 404
 * ({@code ensureMember} is the defense-in-depth gate behind the interceptor).
 * Filter by {@code status} and/or {@code role}; sort by {@code createdAt}
 * (default, newest first) or {@code id}; page with {@code cursor}.
 *
 * <p>No identity enrichment — an invitation carries its own email. Each row's
 * {@code expired} flag is computed against a single {@code now} for the page.
 */
public class ListInvitationsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("status", InvitationStatus.class)
            .set("role", MemberRole.class)
            .instant("createdAt")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListInvitationsUseCase(TourOperatorInvitationRepository invitationRepository,
                                  TourOperatorMembershipCheck membershipCheck) {
        this.invitationRepository = invitationRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<InvitationView> execute(ListQuery query, UUID callerUserId) {
        // Member-only: a non-member gets 404 (ensureMember throws ResourceNotFound).
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<TourOperatorInvitation> page = invitationRepository.list(query);
        Instant now = Instant.now();
        return new CursorPage<>(
                page.data().stream().map(inv -> InvitationView.from(inv, now)).toList(),
                page.nextCursor());
    }
}
