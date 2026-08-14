package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Which locales this owner has any metafield translation in — what the editor's
 * locale switcher marks as "started".
 *
 * <p>It answers from the translation rows rather than from the operator's
 * supported set: the question is "where has someone done work", not "where could
 * they". The supported set is {@code GET /translations}' job on the operator.
 */
public class ListMetafieldTranslationLocalesUseCase {

    private final MetafieldValueTranslationRepository translationRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetafieldTranslationLocalesUseCase(MetafieldValueTranslationRepository translationRepository,
                                                  MetafieldOwnerAccess ownerAccess,
                                                  TourOperatorMembershipCheck membershipCheck) {
        this.translationRepository = translationRepository;
        this.ownerAccess = ownerAccess;
        this.membershipCheck = membershipCheck;
    }

    public List<String> execute(UUID callerUserId, UUID tourOperatorId,
                                MetafieldOwnerType ownerType, UUID ownerId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        ownerAccess.ensureOwned(ownerType, ownerId, tourOperatorId);
        return translationRepository.findLocalesForOwner(tourOperatorId, ownerType, ownerId);
    }
}
