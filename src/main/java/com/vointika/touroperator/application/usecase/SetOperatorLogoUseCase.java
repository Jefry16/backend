package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sets the operator's logo to one of its own media records. ADMIN+ only;
 * membership enforced by the interceptor.
 *
 * <p>The media id is validated against the operator's library through the
 * {@link MediaKeyBatchQuery} seam — an id that isn't owned by this operator
 * (foreign, or nonexistent) is rejected (422), which is what keeps a logo from
 * pointing at another tenant's media without a DB foreign key. Guards: caller
 * not ADMIN+ → 403; unknown media → 422; operator missing → 404 (defensive).
 */
public class SetOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final MediaKeyBatchQuery mediaKeyBatchQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public SetOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                  MediaKeyBatchQuery mediaKeyBatchQuery,
                                  TourOperatorMembershipCheck membershipCheck,
                                  TransactionRunner transactionRunner,
                                  AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (mediaId == null) {
            throw new InvalidFieldException("mediaId is required");
        }
        boolean ownedByOperator = mediaKeyBatchQuery
                .findKeysByIds(tourOperatorId, Set.of(mediaId))
                .containsKey(mediaId);
        if (!ownedByOperator) {
            throw new InvalidFieldException("Media not found in this operator's library");
        }

        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        UUID logoBefore = operator.getLogoMediaId();
        operator.setLogo(mediaId);
        transactionRunner.run(() -> {
            tourOperatorRepository.save(operator);
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
