package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's written policies, through the shared cursor framework
 * (PATTERNS §4b). Member-visible.
 *
 * <p><b>Why the framework for four rows.</b> The set is bounded by the enum, so
 * pagination will never trigger — but §4b's exemption is for curated
 * <em>platform</em> lists (timezones, currencies, UI languages), and this is
 * per-tenant data. Going through the framework is also what gives the screen the
 * same filter and sort grammar every other tenant list speaks, rather than a
 * bespoke array whose ordering is a repository detail.
 *
 * <p>The default sort is {@code type} ascending, which is alphabetical rather
 * than the enum's declaration order because the column stores the name. The
 * storefront footer does <em>not</em> read this path — it has its own ordered
 * query, since a public page has no cursor to carry.
 */
public class ListPoliciesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("type", PolicyType.class)
            .text("title")
            .instant("createdAt")
            .instant("updatedAt")
            .sortable("id")
            .sortable("type")
            .sortable("createdAt")
            .sortable("updatedAt")
            .defaultSort("type")
            .build();

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPoliciesUseCase(TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<PolicyView> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        CursorPage<com.vointika.touroperator.domain.entity.Policy> page = policyRepository.list(query);
        return new CursorPage<>(page.data().stream().map(PolicyView::from).toList(),
                page.nextCursor());
    }
}
