package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository.TranslatedField;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One entry's field translations for one locale, keyed by field key — the shape
 * the PUT accepts, so the editor round-trips what it loaded. Any member.
 *
 * <p>An untranslated locale is an empty map rather than a 404: it is the normal
 * state of an editor about to be filled in.
 */
public class GetMetaobjectFieldTranslationsUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final MetaobjectEntryValueTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        this.entryRepository = entryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public Map<String, String> execute(UUID callerUserId, UUID tourOperatorId,
                                       UUID metaobjectId, String rawLocale) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        ensureOwned(metaobjectId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        Map<String, String> byField = new LinkedHashMap<>();
        for (TranslatedField row : translationRepository.findForEntry(metaobjectId, locale.value())) {
            byField.put(row.fieldKey(), row.value());
        }
        return byField;
    }

    private void ensureOwned(UUID metaobjectId, UUID tourOperatorId) {
        entryRepository.requireByIdAndTourOperatorId(metaobjectId, tourOperatorId);
    }
}
