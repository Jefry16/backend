package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.UUID;

/**
 * Cancels a departure (terminal). ADMIN+. 404 if not under this operator; already
 * cancelled → 409. No booking cascade yet (no booking context). Returns the
 * refreshed slot.
 */
public class CancelSlotUseCase {

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CancelSlotUseCase(SlotRepository slotRepository,
                             SlotAudiencePricingRepository pricingRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public SlotView execute(UUID tourOperatorId, UUID slotId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Slot slot = slotRepository.findByIdAndTourOperatorId(slotId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        String statusBefore = slot.status().name();
        Slot cancelled = transactionRunner.call(() -> {
            Slot saved = slotRepository.save(slot.cancel());
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "SLOT", slotId, "slot.cancelled", null,
                    List.of(new FieldChange("status", statusBefore, saved.status().name()))));
            return saved;
        });
        return SlotView.from(cancelled, pricingRepository.findBySlotId(cancelled.id()));
    }
}
