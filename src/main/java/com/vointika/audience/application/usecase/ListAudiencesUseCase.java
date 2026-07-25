package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists an operator's audiences — cursor-paginated, tenant-scoped. Any member may
 * view; non-member → 404. Filter by {@code name} (text) or {@code paxPerUnit}
 * (number); sort by {@code createdAt} (default, newest first), {@code name},
 * {@code paxPerUnit}, or {@code id}.
 */
public class ListAudiencesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("name")
            .number("paxPerUnit", Integer.class)
            .instant("createdAt")
            .sortable("name")
            .sortable("paxPerUnit")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final AudienceRepository audienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListAudiencesUseCase(AudienceRepository audienceRepository,
                                TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<Audience> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return audienceRepository.list(query);
    }
}
