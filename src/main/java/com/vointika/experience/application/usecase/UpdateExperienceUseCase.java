package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.ExperienceInputMapper;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TransactionRunner;

import java.util.UUID;

/**
 * Updates an experience's editable fields (everything but slug/status). ADMIN+
 * only. Guards: caller not ADMIN+ → 403; id not under this operator → 404;
 * invalid field or media ref → 422. The slug is immutable and the status is
 * changed only via publish/unpublish.
 *
 * <p>Slots snapshot the experience's name/description at creation — when either
 * changes here, the snapshot is refreshed across the experience's slots in the
 * same transaction, so existing departures never show stale copy. Timing is
 * untouched (a slot's startAt/endAt are explicit; the experience's
 * durationMinutes is only the advertised length).
 */
public class UpdateExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final SlotRepository slotRepository;
    private final MediaReferenceValidator mediaReferenceValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public UpdateExperienceUseCase(ExperienceRepository experienceRepository,
                                   SlotRepository slotRepository,
                                   MediaReferenceValidator mediaReferenceValidator,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner) {
        this.experienceRepository = experienceRepository;
        this.slotRepository = slotRepository;
        this.mediaReferenceValidator = mediaReferenceValidator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId, ExperienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));

        var mediaIds = ExperienceInputMapper.mediaIds(input);
        mediaReferenceValidator.validate(tourOperatorId, mediaIds, input.thumbnailMediaId());

        String nameBefore = experience.getName().value();
        String descriptionBefore = experience.getDescription().value();

        experience.update(
                ExperienceInputMapper.name(input),
                ExperienceInputMapper.description(input),
                ExperienceInputMapper.longDescription(input),
                input.featured(),
                ExperienceInputMapper.tags(input),
                ExperienceInputMapper.included(input),
                ExperienceInputMapper.notIncluded(input),
                ExperienceInputMapper.highlights(input),
                mediaIds,
                input.thumbnailMediaId(),
                ExperienceInputMapper.durationMinutes(input),
                ExperienceInputMapper.bookingCutoffHours(input));

        boolean snapshotChanged = !experience.getName().value().equals(nameBefore)
                || !experience.getDescription().value().equals(descriptionBefore);

        transactionRunner.run(() -> {
            experienceRepository.save(experience);
            if (snapshotChanged) {
                slotRepository.propagateExperienceSnapshot(
                        experience.getId(),
                        experience.getName().value(),
                        experience.getDescription().value());
            }
        });
    }
}
