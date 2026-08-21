package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * Reads one locale's translation overlay. Any member. An untranslated locale
 * returns an empty overlay (name null) so the admin editor always has a form.
 */
public class GetCategoryTranslationUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetCategoryTranslationUseCase(CategoryRepository categoryRepository,
                                         CategoryTranslationRepository translationRepository,
                                         TourOperatorMembershipCheck membershipCheck) {
        this.categoryRepository = categoryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public CategoryTranslation execute(UUID tourOperatorId, UUID categoryId,
                                       String rawLocale, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        categoryRepository.requireExists(categoryId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);
        return translationRepository.findByCategoryIdAndLocale(categoryId, locale.value())
                .orElseGet(() -> CategoryTranslation.empty(categoryId, tourOperatorId, locale));
    }
}
