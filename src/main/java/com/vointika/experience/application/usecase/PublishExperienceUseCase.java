package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
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
 * Publishes an experience (DRAFT → PUBLISHED). ADMIN+ only. 403 non-admin; 404
 * if not under this operator; 409 if already published.
 */
public class PublishExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public PublishExperienceUseCase(ExperienceRepository experienceRepository,
                                    TourOperatorMembershipCheck membershipCheck,
                                    TransactionRunner transactionRunner,
                                    AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        experience.publish();
        // Reaching here means the flip is real (an idempotent re-publish 409s
        // in the entity), so the diff is always exactly this one field.
        transactionRunner.run(() -> {
            experienceRepository.save(experience);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "EXPERIENCE", experienceId, "experience.published", null,
                    List.of(new FieldChange("published", false, true))));
        });
    }
}
