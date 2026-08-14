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
 * Every display column filters (as a set) and sorts: {@code name}, {@code email},
 * {@code role}, {@code status}, {@code invitedByName} and {@code createdAt} (the
 * default sort, newest first); {@code id} sorts too.
 *
 * <p>Each row's {@code expired} flag is computed against a single {@code now} for
 * the page. Invitee email/name AND the inviter's name are all carried on the
 * invitation row (the inviter name is a snapshot from invite time), so there is
 * no post-pagination identity enrichment — which is exactly what makes every
 * column sortable/filterable off the single root (PATTERNS §6 forbids the alternative
 * join).
 */
public class ListInvitationsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("name", String.class)
            .sortable("name")
            .set("email", String.class)
            .sortable("email")
            .set("role", MemberRole.class)
            .sortable("role")
            .set("status", InvitationStatus.class)
            .sortable("status")
            .instant("createdAt")
            .sortable("createdAt")
            .set("invitedByName", String.class)
            .sortable("invitedByName")
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
                page.data().stream()
                        .map(inv -> InvitationView.from(inv, now))
                        .toList(),
                page.nextCursor());
    }
}
