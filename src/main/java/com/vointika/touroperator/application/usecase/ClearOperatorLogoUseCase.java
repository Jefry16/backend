package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.List;
import java.util.UUID;

/**
 * Removes the operator's logo. ADMIN+ only; membership enforced by the
 * interceptor. Idempotent — clearing an already-empty logo is a no-op success.
 * Guards: caller not ADMIN+ → 403; operator missing → 404 (defensive).
 */
public class ClearOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public ClearOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                    TourOperatorMembershipCheck membershipCheck,
                                    TransactionRunner transactionRunner,
                                    AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        UUID logoBefore = operator.getLogoMediaId();
        operator.clearLogo();
        transactionRunner.run(() -> {
            tourOperatorRepository.save(operator);
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
