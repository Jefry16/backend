package com.vointika.touroperator.application.usecase;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists a tour operator's team members — cursor-paginated via the shared list
 * framework, tenant-scoped to the operator. **Any member** of the operator may
 * view the roster; a non-member is a 404 (the membership interceptor already
 * turns a non-member into "operator not found", and {@code ensureMember} here is
 * the defense-in-depth gate). Filterable by {@code role}, sorted {@code joinedAt}
 * ascending by default (owner first).
 *
 * <p>Each page's rows are enriched with the member's name + email via a SINGLE
 * batched {@link UserAccountQuery#findAccounts} over the page's user ids (no N+1).
 * name/email are best-effort: an unresolvable account carries null rather than
 * dropping the member row.
 */
public class ListMembersUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("role", MemberRole.class)
            .instant("joinedAt")
            .sortable("joinedAt")
            .sortable("id")
            .defaultSort("joinedAt")
            .build();

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

    public CursorPage<MemberListView> execute(ListQuery query, UUID callerUserId) {
        // Member-only: a non-member gets 404 (ensureMember throws ResourceNotFound).
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<TourOperatorMember> page = memberRepository.list(query);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        Set<UUID> userIds = page.data().stream()
                .map(TourOperatorMember::getUserId)
                .collect(Collectors.toSet());
        // HashMap, not Collectors.toMap: an account's name may be null, and toMap
        // rejects null values.
        Map<UUID, UserAccountView> accountsById = new HashMap<>();
        for (UserAccountView account : userAccountQuery.findAccounts(userIds)) {
            accountsById.put(account.userId(), account);
        }

        return new CursorPage<>(
                page.data().stream()
                        .map(member -> {
                            UserAccountView account = accountsById.get(member.getUserId());
                            return new MemberListView(
                                    member.getUserId(),
                                    member.getRole(),
                                    member.getJoinedAt(),
                                    account == null ? null : account.name(),
                                    account == null ? null : account.email());
                        })
                        .toList(),
                page.nextCursor());
    }
}
