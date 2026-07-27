package com.vointika.page.application.usecase;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists an operator's pages — cursor-paginated, tenant-scoped, WITHOUT bodies
 * (heavy HTML stays on the detail read). Any member. Filter by {@code title}/
 * {@code handle} (text) or {@code status} (set); sort by title/handle/status/
 * createdAt/id, default {@code -id} (newest first).
 */
public class ListPagesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("title")
            .text("handle")
            .set("status", PageStatus.class)
            .instant("createdAt")
            .sortable("id")
            .sortable("title")
            .sortable("handle")
            .sortable("status")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPagesUseCase(PageRepository pageRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.pageRepository = pageRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<PageListItem> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return pageRepository.list(query);
    }
}
