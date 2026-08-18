package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces the operator's canonical SEO defaults. ADMIN+, audited.
 *
 * <p>A whole-value replace, not a partial patch: a null field clears the
 * override, which is what the storefront reads as "fall back to the next step".
 * A partial patch could not express "remove this override" without a sentinel.
 *
 * <p>{@code ogImageMediaId} is validated against the operator's own media
 * library (422 on a foreign or unknown id), the same guard
 * {@code UpdateBrandUseCase} applies — a bare media id with no FK is only as
 * trustworthy as the check that admits it.
 */
public class UpdateOperatorSeoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateOperatorSeoUseCase(TourOperatorRepository tourOperatorRepository,
                                    MediaAssetBatchQuery mediaAssetBatchQuery,
                                    TourOperatorMembershipCheck membershipCheck,
                                    TransactionRunner transactionRunner,
                                    AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, String rawSeoTitle, String rawSeoDescription,
                        UUID ogImageMediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.requireById(tourOperatorId);

        OperatorSeoTitle seoTitle = blank(rawSeoTitle) ? null : new OperatorSeoTitle(rawSeoTitle);
        OperatorSeoDescription seoDescription =
                blank(rawSeoDescription) ? null : new OperatorSeoDescription(rawSeoDescription);

        if (ogImageMediaId != null
                && !mediaAssetBatchQuery.findAssetsByIds(tourOperatorId, Set.of(ogImageMediaId))
                        .containsKey(ogImageMediaId)) {
            throw new InvalidFieldException("Media not found in this operator's library");
        }

        Map<String, Object> before = auditSnapshot(operator);
        operator.updateSeo(seoTitle, seoDescription, ogImageMediaId);
        List<FieldChange> changes = AuditChanges.diff(before, auditSnapshot(operator));
        if (changes.isEmpty()) {
            return;
        }

        transactionRunner.run(() -> {
            tourOperatorRepository.save(operator);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.seo_updated", null, changes));
        });
    }

    private static Map<String, Object> auditSnapshot(TourOperator operator) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("seoTitle", operator.getSeoTitle() == null ? null : operator.getSeoTitle().value());
        snapshot.put("seoDescription",
                operator.getSeoDescription() == null ? null : operator.getSeoDescription().value());
        snapshot.put("ogImageMediaId",
                operator.getOgImageMediaId() == null ? null : operator.getOgImageMediaId().toString());
        return snapshot;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
