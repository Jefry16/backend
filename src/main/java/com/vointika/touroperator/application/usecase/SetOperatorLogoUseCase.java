package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sets the operator's logo to one of its own media records. ADMIN+ only;
 * membership enforced by the interceptor.
 *
 * <p>The media id is validated against the operator's library through the
 * {@link MediaAssetBatchQuery} seam — an id that isn't owned by this operator
 * (foreign, or nonexistent) is rejected (422), which is what keeps a logo from
 * pointing at another tenant's media without a DB foreign key. Guards: caller
 * not ADMIN+ → 403; unknown media → 422; operator missing → 404 (defensive).
 *
 * <p><b>It writes the brand row, not the operator row.</b> V10 moved the column
 * there — Shopify has no {@code shop.logo}, only {@code shop.brand.logo} — and
 * the endpoint followed it rather than being deleted with its old home. The
 * operator is still loaded, because "no such operator" is still a 404 and a
 * missing brand row is not.
 */
public class SetOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorBrandRepository tourOperatorBrandRepository;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public SetOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                  TourOperatorBrandRepository tourOperatorBrandRepository,
                                  MediaAssetBatchQuery mediaAssetBatchQuery,
                                  TourOperatorMembershipCheck membershipCheck,
                                  TransactionRunner transactionRunner,
                                  AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.tourOperatorBrandRepository = tourOperatorBrandRepository;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (mediaId == null) {
            throw new InvalidFieldException("mediaId is required");
        }
        boolean ownedByOperator = mediaAssetBatchQuery
                .findAssetsByIds(tourOperatorId, Set.of(mediaId))
                .containsKey(mediaId);
        if (!ownedByOperator) {
            throw new InvalidFieldException("Media not found in this operator's library");
        }

        // Loaded only to answer 404 for an operator that does not exist; a brand
        // row that does not exist yet is ordinary and is created on write.
        tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        UUID logoBefore = tourOperatorBrandRepository.findLogoMediaId(tourOperatorId).orElse(null);
        transactionRunner.run(() -> {
            tourOperatorBrandRepository.setLogoMediaId(tourOperatorId, mediaId);
            // Re-setting the same logo mutates nothing — records nothing.
            if (!mediaId.equals(logoBefore)) {
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "TOUR_OPERATOR", tourOperatorId, "tour_operator.logo_updated", null,
                        List.of(new FieldChange("logoMediaId",
                                logoBefore == null ? null : logoBefore.toString(), mediaId.toString()))));
            }
        });
    }
}
