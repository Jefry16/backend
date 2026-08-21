package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists an operator's categories — cursor-paginated, tenant-scoped. Any member
 * may view; non-member → 404. Filter by {@code name} (text) or {@code createdAt}
 * (instant); sort by {@code createdAt} (default, newest first), {@code name} or
 * {@code id}.
 *
 * <p>Cursor-paginated rather than a bare array because the ceiling is whatever
 * the operator builds — the same test §4b applies to {@code /metafields}. Both
 * sortable columns are {@code NOT NULL}, which the keyset cursor requires.
 */
public class ListCategoriesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("name")
            .instant("createdAt")
            .sortable("name")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final CategoryRepository categoryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListCategoriesUseCase(CategoryRepository categoryRepository,
                                 TourOperatorMembershipCheck membershipCheck) {
        this.categoryRepository = categoryRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<Category> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return categoryRepository.list(query);
    }
}
