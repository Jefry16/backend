package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository.TranslatedValue;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One owner's metafield translations for one locale, keyed {@code namespace.key}
 * — the shape the PUT accepts, so the editor round-trips what it loaded.
 *
 * <p>Any member may read: a translation is the operator's own published content,
 * and the write is the part that needs ADMIN+.
 *
 * <p><b>A locale with nothing translated is an empty map, not a 404.</b> "This
 * locale has no translations yet" is the normal state of an editor someone is
 * about to fill in, not a missing resource.
 */
public class GetMetafieldTranslationsUseCase {

    private final MetafieldValueTranslationRepository translationRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMetafieldTranslationsUseCase(MetafieldValueTranslationRepository translationRepository,
                                           MetafieldOwnerAccess ownerAccess,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.translationRepository = translationRepository;
        this.ownerAccess = ownerAccess;
        this.membershipCheck = membershipCheck;
    }

    public Map<String, String> execute(UUID callerUserId, UUID tourOperatorId,
                                       MetafieldOwnerType ownerType, UUID ownerId, String rawLocale) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        ownerAccess.ensureOwned(ownerType, ownerId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        Map<String, String> byQualifiedKey = new LinkedHashMap<>();
        for (TranslatedValue row : translationRepository
                .findForOwner(tourOperatorId, ownerType, ownerId, locale.value())) {
            byQualifiedKey.put(row.namespace() + "." + row.key(), row.value());
        }
        return byQualifiedKey;
    }
}
