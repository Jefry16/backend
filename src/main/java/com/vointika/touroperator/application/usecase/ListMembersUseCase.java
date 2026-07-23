package com.vointika.touroperator.application.usecase;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.UUID;

/**
 * Lists a tour operator's team members — cursor-paginated via the shared list
 * framework, tenant-scoped to the operator. **Any member** of the operator may
 * view the roster; a non-member is a 404 (the membership interceptor already
 * turns a non-member into "operator not found", and {@code ensureMember} here is
 * the defense-in-depth gate). Filterable by {@code role}, {@code name} and
 * {@code email} — all as exact-match {@code in} sets ({@code filter[name][in]=…},
 * for a fetched multi-select picker); sortable by {@code joinedAt} (default —
 * owner first), {@code name}, {@code email} and {@code role} (role sorts
 * alphabetically on the stored enum — ADMIN, OWNER, STAFF — not by hierarchy).
 *
 * <p>name/email are denormalized onto the member row (see the V1 members-table
 * comment + {@link TourOperatorMember}), so the list reads them straight off the
 * row — which is exactly what makes them sortable/filterable, since identity's
 * tables can't be joined (§3.5). No post-pagination enrichment.
 */
public class ListMembersUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("role", MemberRole.class)
            .sortable("role")
            .instant("joinedAt")
            .sortable("joinedAt")
            .sortable("id")
            .set("name", String.class)
            .sortable("name")
            .set("email", String.class)
            .sortable("email")
            .defaultSort("joinedAt")
            .build();

    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMembersUseCase(TourOperatorMemberRepository memberRepository,
                              TourOperatorMembershipCheck membershipCheck) {
        this.memberRepository = memberRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MemberListView> execute(ListQuery query, UUID callerUserId) {
        // Member-only: a non-member gets 404 (ensureMember throws ResourceNotFound).
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<TourOperatorMember> page = memberRepository.list(query);
        return new CursorPage<>(
                page.data().stream()
                        .map(member -> new MemberListView(
                                member.getUserId(),
                                member.getRole(),
                                member.getJoinedAt(),
                                member.getName(),
                                member.getEmail()))
                        .toList(),
                page.nextCursor());
    }
}
