package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.ExperienceInputMapper;
import com.vointika.experience.application.service.CategoryReferenceValidator;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Updates an experience's editable fields (everything but handle/status). ADMIN+
 * only. Guards: caller not ADMIN+ → 403; id not under this operator → 404;
 * invalid field, media ref or category ref → 422. The handle is immutable and the
 * status is changed only via publish/unpublish.
 *
 * <p><b>It is a replace despite the PATCH verb</b>, and {@code categoryId} inherits
 * that: an omitted one files the experience back to uncategorized rather than
 * leaving it where it was. Every other optional field here behaves the same way —
 * the SEO pair did it silently until #145 — so the caller sends the whole record.
 *
 * <p>Slots snapshot the experience's name/description at creation — when either
 * changes here, the snapshot is refreshed across the experience's slots in the
 * same transaction, so existing departures never show stale copy. Slot timing is
 * untouched: a slot's startAt/endAt are explicit, and the experience carries no
 * duration of its own to derive them from.
 */
public class UpdateExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final SlotRepository slotRepository;
    private final MediaReferenceValidator mediaReferenceValidator;
    private final CategoryReferenceValidator categoryReferenceValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateExperienceUseCase(ExperienceRepository experienceRepository,
                                   SlotRepository slotRepository,
                                   MediaReferenceValidator mediaReferenceValidator,
                                   CategoryReferenceValidator categoryReferenceValidator,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.slotRepository = slotRepository;
        this.mediaReferenceValidator = mediaReferenceValidator;
        this.categoryReferenceValidator = categoryReferenceValidator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId, ExperienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository
                .requireByIdAndTourOperatorId(experienceId, tourOperatorId);

        var mediaIds = ExperienceInputMapper.mediaIds(input);
        mediaReferenceValidator.validate(tourOperatorId, mediaIds, input.thumbnailMediaId());
        categoryReferenceValidator.validate(tourOperatorId, input.categoryId());

        Map<String, Object> before = experience.auditSnapshot();
        String nameBefore = experience.getName().value();
        String descriptionBefore = experience.getDescription().value();

        experience.update(
                ExperienceInputMapper.name(input),
                ExperienceInputMapper.description(input),
                ExperienceInputMapper.longDescription(input),
                input.isFeatured(),
                mediaIds,
                input.thumbnailMediaId(),
                ExperienceInputMapper.bookingCutoffHours(input),
                ExperienceInputMapper.seoTitle(input),
                ExperienceInputMapper.seoDescription(input),
                ExperienceInputMapper.startingPrice(input),
                input.categoryId());

        boolean snapshotChanged = !experience.getName().value().equals(nameBefore)
                || !experience.getDescription().value().equals(descriptionBefore);

        // The slot-snapshot propagation is part of this edit — it rides the one
        // experience.updated entry, no entry of its own. A no-op PUT (nothing
        // actually changed) records nothing.
        List<FieldChange> changes = AuditChanges.diff(before, experience.auditSnapshot());
        transactionRunner.run(() -> {
            experienceRepository.save(experience);
            if (snapshotChanged) {
                slotRepository.propagateExperienceSnapshot(
                        experience.getId(),
                        experience.getName().value(),
                        experience.getDescription().value());
            }
            if (!changes.isEmpty()) {
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "EXPERIENCE", experienceId, "experience.updated", null, changes));
            }
        });
    }
}
