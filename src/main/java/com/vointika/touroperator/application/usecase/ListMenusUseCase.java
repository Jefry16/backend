package com.vointika.touroperator.application.usecase;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.repository.MenuRepository;

import java.util.UUID;

/**
 * The operator's menus — cursor-paginated, tenant-scoped. Any member.
 * Filter by handle/title (text); sort by id (default, newest first),
 * handle, title or createdAt.
 */
public class ListMenusUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("handle")
            .text("title")
            .instant("createdAt")
            .sortable("id")
            .sortable("handle")
            .sortable("title")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final MenuRepository menuRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMenusUseCase(MenuRepository menuRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.menuRepository = menuRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<Menu> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return menuRepository.list(query);
    }
}
