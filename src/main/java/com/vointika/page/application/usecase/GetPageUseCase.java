package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** A single page (body included). Any member; cross-tenant → byte-identical 404. */
public class GetPageUseCase {

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetPageUseCase(PageRepository pageRepository,
                          TourOperatorMembershipCheck membershipCheck) {
        this.pageRepository = pageRepository;
        this.membershipCheck = membershipCheck;
    }

    public Page execute(UUID tourOperatorId, UUID pageId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return pageRepository
.requireByIdAndTourOperatorId(pageId, tourOperatorId);
    }
}
