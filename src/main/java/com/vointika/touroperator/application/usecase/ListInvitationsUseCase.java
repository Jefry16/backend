package com.vointika.touroperator.application.usecase;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists a tour operator's invitations — ALL statuses (PENDING / ACCEPTED /
 * REVOKED), cursor-paginated via the shared list framework and tenant-scoped to
 * the operator. **Any member** may view (read-only, like the roster; the
 * mutating actions invite/resend/revoke stay ADMIN+); a non-member is a 404
 * ({@code ensureMember} is the defense-in-depth gate behind the interceptor).
 * Filter by {@code status} and/or {@code role}; sort by {@code createdAt}
 * (default, newest first) or {@code id}; page with {@code cursor}.
 *
 * <p>Each row's {@code expired} flag is computed against a single {@code now}
 * for the page; the inviter's display name is enriched via ONE batched
 * {@link UserAccountQuery#findAccounts} over the page's inviter ids (no N+1,
 * best-effort — an unresolvable account carries a null name, never dropping the
 * row). The invitee email needs no lookup — the invitation carries it.
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
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListInvitationsUseCase(TourOperatorInvitationRepository invitationRepository,
                                  UserAccountQuery userAccountQuery,
                                  TourOperatorMembershipCheck membershipCheck) {
        this.invitationRepository = invitationRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<InvitationView> execute(ListQuery query, UUID callerUserId) {
        // Member-only: a non-member gets 404 (ensureMember throws ResourceNotFound).
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<TourOperatorInvitation> page = invitationRepository.list(query);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        Set<UUID> inviterIds = page.data().stream()
                .map(TourOperatorInvitation::getInvitedByUserId)
                .collect(Collectors.toSet());
        // HashMap, not Collectors.toMap: a name may be null, and toMap rejects null values.
        Map<UUID, String> nameByUserId = new HashMap<>();
        for (UserAccountView account : userAccountQuery.findAccounts(inviterIds)) {
            nameByUserId.put(account.userId(), account.name());
        }

        Instant now = Instant.now();
        return new CursorPage<>(
                page.data().stream()
                        .map(inv -> InvitationView.from(inv, now, nameByUserId.get(inv.getInvitedByUserId())))
                        .toList(),
                page.nextCursor());
    }
}
