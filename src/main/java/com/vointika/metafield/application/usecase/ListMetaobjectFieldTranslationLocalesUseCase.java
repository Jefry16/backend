package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/** Which locales this entry has any field translation in. Any member. */
public class ListMetaobjectFieldTranslationLocalesUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final MetaobjectEntryValueTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetaobjectFieldTranslationLocalesUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        this.entryRepository = entryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<String> execute(UUID callerUserId, UUID tourOperatorId, UUID metaobjectId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        entryRepository.requireByIdAndTourOperatorId(metaobjectId, tourOperatorId);
        return translationRepository.findLocalesForEntry(metaobjectId);
    }
}
