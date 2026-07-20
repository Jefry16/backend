package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists a tour operator's team members. ADMIN+ only — team management is an admin
 * capability, so a STAFF caller gets 403 (the membership interceptor has already
 * turned a non-member into a 404). Sorted by {@code joinedAt} ascending, so the
 * owner (who joined first) leads the roster.
 *
 * <p>Rows are enriched with the member's name + email via a SINGLE batched
 * {@link UserAccountQuery#findAccounts} over the page's user ids (no N+1). name/
 * email are best-effort: an unresolvable account carries null rather than dropping
 * the member row. Plain list (teams are small) — pagination is a future concern.
 */
public class ListMembersUseCase {

    private final TourOperatorMemberRepository memberRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMembersUseCase(TourOperatorMemberRepository memberRepository,
                              UserAccountQuery userAccountQuery,
                              TourOperatorMembershipCheck membershipCheck) {
        this.memberRepository = memberRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
    }

    public List<MemberListView> execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        List<TourOperatorMember> members = memberRepository.findByTourOperatorId(tourOperatorId);
        if (members.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = members.stream()
                .map(TourOperatorMember::getUserId)
                .collect(Collectors.toSet());
        // HashMap, not Collectors.toMap: an account's name may be null, and toMap
        // rejects null values.
        Map<UUID, UserAccountView> accountsById = new HashMap<>();
        for (UserAccountView account : userAccountQuery.findAccounts(userIds)) {
            accountsById.put(account.userId(), account);
        }

        return members.stream()
                .sorted(Comparator.comparing(TourOperatorMember::getJoinedAt))
                .map(member -> {
                    UserAccountView account = accountsById.get(member.getUserId());
                    return new MemberListView(
                            member.getUserId(),
                            member.getRole(),
                            member.getJoinedAt(),
                            account == null ? null : account.name(),
                            account == null ? null : account.email());
                })
                .toList();
    }
}
