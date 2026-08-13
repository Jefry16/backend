package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Drops every metafield translation this owner has in one locale — the editor's
 * "this locale is not translated after all". ADMIN+.
 *
 * <p><b>204 whether or not anything was there</b>, but nothing is audited when
 * nothing went: a delete that removed no rows is not an event, and logging it
 * would fill the owner's timeline with the editor's navigation. The same rule
 * {@code UpsertOperatorTranslationUseCase} follows when it clears a blank form.
 */
public class DeleteMetafieldTranslationsUseCase {

    private final MetafieldValueTranslationRepository translationRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetafieldTranslationsUseCase(MetafieldValueTranslationRepository translationRepository,
                                              MetafieldOwnerAccess ownerAccess,
                                              TourOperatorMembershipCheck membershipCheck,
                                              TransactionRunner transactionRunner,
                                              AuditTrailPort auditTrailPort) {
        this.translationRepository = translationRepository;
        this.ownerAccess = ownerAccess;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID callerUserId, UUID tourOperatorId,
                        MetafieldOwnerType ownerType, UUID ownerId, String rawLocale) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        ownerAccess.ensureOwned(ownerType, ownerId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        transactionRunner.run(() -> {
            if (translationRepository.deleteForOwner(
                    tourOperatorId, ownerType, ownerId, locale.value()) == 0) {
                return;
            }
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    ownerType.auditEntityType(), ownerId,
                    ownerType.action("translation_cleared"),
                    Map.of("locale", locale.value())));
        });
    }
}
