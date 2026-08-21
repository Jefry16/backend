package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/** Lists a category's translation overlays — one row per translated locale. Any member. */
public class ListCategoryTranslationsUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListCategoryTranslationsUseCase(CategoryRepository categoryRepository,
                                           CategoryTranslationRepository translationRepository,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.categoryRepository = categoryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<CategoryTranslation> execute(UUID tourOperatorId, UUID categoryId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        categoryRepository.requireExists(categoryId, tourOperatorId);
        return translationRepository.findAllByCategoryId(categoryId);
    }
}
