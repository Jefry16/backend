package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * One (page, locale) overlay for the admin editor. Any member. An untranslated
 * locale returns the EMPTY overlay (all fields absent) rather than 404 — the
 * editor form seeds from it either way.
 */
public class GetPageTranslationUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetPageTranslationUseCase(PageRepository pageRepository,
                                     PageTranslationRepository translationRepository,
                                     TourOperatorMembershipCheck membershipCheck) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public PageTranslation execute(UUID tourOperatorId, UUID pageId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        if (pageRepository.findByIdAndTourOperatorId(pageId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Page not found");
        }
        LocaleCode locale = new LocaleCode(rawLocale);
        return translationRepository.find(pageId, locale)
                .orElseGet(() -> PageTranslation.empty(pageId, tourOperatorId, locale));
    }
}
