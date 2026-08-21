package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Reads one category. Any member may view; non-member → 404; an id not under this
 * operator → 404.
 */
public class GetCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetCategoryUseCase(CategoryRepository categoryRepository,
                              TourOperatorMembershipCheck membershipCheck) {
        this.categoryRepository = categoryRepository;
        this.membershipCheck = membershipCheck;
    }

    public Category execute(UUID tourOperatorId, UUID categoryId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return categoryRepository.requireByIdAndTourOperatorId(categoryId, tourOperatorId);
    }
}
