package com.vointika.audience.application.usecase;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.SlotAudienceSnapshotPropagator;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Updates an audience's name and/or pax-per-unit (partial — only provided fields
 * apply). ADMIN+ only. Guards: caller not ADMIN+ → 403; id not under this
 * operator → 404; name clashes (case-insensitively) with another of the
 * operator's audiences → 409. A change syncs the name + pax-per-unit onto the
 * audience's snapshotted slot pricing rows (price/capacity stay frozen).
 */
public class UpdateAudienceUseCase {

    private final AudienceRepository audienceRepository;
    private final SlotAudienceSnapshotPropagator slotAudienceSnapshotPropagator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateAudienceUseCase(AudienceRepository audienceRepository,
                                 SlotAudienceSnapshotPropagator slotAudienceSnapshotPropagator,
                                 TourOperatorMembershipCheck membershipCheck,
                                 TransactionRunner transactionRunner,
                                 AuditTrailPort auditTrailPort) {
        this.audienceRepository = audienceRepository;
        this.slotAudienceSnapshotPropagator = slotAudienceSnapshotPropagator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, UUID callerUserId, AudienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        Audience audience = audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Audience not found"));

        Map<String, Object> before = audience.auditSnapshot();
        boolean changed = false;

        if (input.name() != null) {
            AudienceName newName = new AudienceName(input.name());
            // Any change (including case) is persisted + propagated; the clash
            // check is case-insensitive and excludes this audience.
            if (!newName.value().equals(audience.getName().value())) {
                if (audienceRepository.existsByTourOperatorIdAndNameExcluding(
                        tourOperatorId, newName.value(), audienceId)) {
                    throw new ResourceAlreadyExistsException("An audience with this name already exists");
                }
                audience.rename(newName);
                changed = true;
            }
        }

        if (input.paxPerUnit() != null) {
            PaxPerUnit newPaxPerUnit = new PaxPerUnit(input.paxPerUnit());
            if (newPaxPerUnit.value() != audience.getPaxPerUnit().value()) {
                audience.changePaxPerUnit(newPaxPerUnit);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        Audience toSave = audience;
        List<FieldChange> changes = AuditChanges.diff(before, audience.auditSnapshot());
        try {
            // The slot-snapshot propagation is part of this edit — it rides the
            // one audience.updated entry, no entry of its own.
            transactionRunner.run(() -> {
                audienceRepository.save(toSave);
                slotAudienceSnapshotPropagator.propagate(
                        toSave.getId(), toSave.getName().value(), toSave.getPaxPerUnit().value());
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "AUDIENCE", audienceId, "audience.updated", null, changes));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }
    }
}
