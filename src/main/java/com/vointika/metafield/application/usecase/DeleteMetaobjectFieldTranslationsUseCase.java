package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Drops every field translation this entry has in one locale. ADMIN+. 204
 * whether or not anything was there, and nothing is audited when nothing went.
 */
public class DeleteMetaobjectFieldTranslationsUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final MetaobjectEntryValueTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        this.entryRepository = entryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID callerUserId, UUID tourOperatorId, UUID metaobjectId, String rawLocale) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        entryRepository.requireByIdAndTourOperatorId(metaobjectId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        transactionRunner.run(() -> {
            if (translationRepository.deleteForEntry(metaobjectId, locale.value()) == 0) {
                return;
            }
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAOBJECT", metaobjectId, "metaobject.translation_cleared",
                    Map.of("locale", locale.value())));
        });
    }
}
