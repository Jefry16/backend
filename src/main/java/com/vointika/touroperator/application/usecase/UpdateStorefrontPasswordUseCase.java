package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces the operator's storefront password-protection settings (Shopify's
 * Store access). ADMIN+ only. Enabling without a password (stored or
 * provided) is a 422; a no-op save records nothing.
 *
 * <p>The audit diff carries {@code passwordEnabled} and {@code
 * passwordMessage} ONLY — the password value itself must never reach the
 * trail (a password change with nothing else touched audits as a bare
 * event with no field changes).
 */
public class UpdateStorefrontPasswordUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateStorefrontPasswordUseCase(TourOperatorRepository tourOperatorRepository,
                                           TourOperatorMembershipCheck membershipCheck,
                                           TransactionRunner transactionRunner,
                                           AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, boolean enabled, String password, String message,
                        UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));

        Map<String, Object> before = auditSnapshot(operator);
        boolean passwordChanged = password != null && !password.isBlank()
                && !password.trim().equals(operator.getStorefrontPassword());
        operator.updateStorefrontPassword(enabled, password, message);
        List<FieldChange> changes = AuditChanges.diff(before, auditSnapshot(operator));

        transactionRunner.run(() -> {
            tourOperatorRepository.save(operator);
            if (!changes.isEmpty() || passwordChanged) {
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "TOUR_OPERATOR", tourOperatorId,
                        "tour_operator.storefront_password_updated",
                        null,
                        changes));
            }
        });
    }

    /** The auditable fields — deliberately EXCLUDES the password value. */
    private static Map<String, Object> auditSnapshot(TourOperator operator) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("passwordEnabled", operator.isPasswordEnabled());
        snapshot.put("passwordMessage", operator.getPasswordMessage());
        return snapshot;
    }
}
