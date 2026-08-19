package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/** All translated locales for a page (one row each). Any member. */
public class ListPageTranslationsUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPageTranslationsUseCase(PageRepository pageRepository,
                                       PageTranslationRepository translationRepository,
                                       TourOperatorMembershipCheck membershipCheck) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<PageTranslation> execute(UUID tourOperatorId, UUID pageId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        pageRepository.requireExists(pageId, tourOperatorId);
        return translationRepository.findAllByPageId(pageId);
    }
}
