package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.List;
import java.util.UUID;

/**
 * Removes the operator's logo. ADMIN+ only; membership enforced by the
 * interceptor. Idempotent — clearing an already-empty logo is a no-op success.
 * Guards: caller not ADMIN+ → 403; operator missing → 404 (defensive).
 *
 * <p>It clears the brand row's column, not the operator's — see
 * {@link SetOperatorLogoUseCase} for why the endpoint followed V10's move.
 */
public class ClearOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorBrandRepository tourOperatorBrandRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public ClearOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                    TourOperatorBrandRepository tourOperatorBrandRepository,
                                    TourOperatorMembershipCheck membershipCheck,
                                    TransactionRunner transactionRunner,
                                    AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.tourOperatorBrandRepository = tourOperatorBrandRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        // Loaded only to answer 404 for an operator that does not exist; a brand
        // row that does not exist yet is an operator with no logo.
        tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        UUID logoBefore = tourOperatorBrandRepository.findLogoMediaId(tourOperatorId).orElse(null);
        transactionRunner.run(() -> {
            tourOperatorBrandRepository.setLogoMediaId(tourOperatorId, null);
            // Idempotent clear of an already-empty logo — records nothing.
            if (logoBefore != null) {
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "TOUR_OPERATOR", tourOperatorId, "tour_operator.logo_updated", null,
                        List.of(new FieldChange("logoMediaId", logoBefore.toString(), null))));
            }
        });
    }
}
