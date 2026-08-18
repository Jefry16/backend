package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.ExperienceInputMapper;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.service.HandleGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.UUID;

/**
 * Creates a DRAFT experience. ADMIN+ only; membership enforced by the route
 * interceptor. Field validation is via the value objects
 * ({@link ExperienceInputMapper}); media references are validated against the
 * operator's library ({@link MediaReferenceValidator}, 422 on a foreign id).
 *
 * <p>The canonical handle is generated per-operator, unique across <em>both</em>
 * canonical and localized handles: the storefront resolves a handle against localized
 * handles first and canonical ones second, so a canonical handle that lands on some
 * experience's localized handle would shadow it (PATTERNS §4d). The handle is derived,
 * not operator-chosen, so a collision auto-suffixes rather than answering 409. On a
 * handle race the tx rolls back and we retry with a fresh handle (mirrors
 * CreateTourOperatorUseCase).
 */
public class CreateExperienceUseCase {

    private static final int MAX_HANDLE_ATTEMPTS = 5;

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final MediaReferenceValidator mediaReferenceValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final HandleGenerator handleGenerator;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateExperienceUseCase(ExperienceRepository experienceRepository,
                                   ExperienceTranslationRepository translationRepository,
                                   MediaReferenceValidator mediaReferenceValidator,
                                   TourOperatorMembershipCheck membershipCheck,
                                   HandleGenerator handleGenerator,
                                   IdGenerator idGenerator,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.mediaReferenceValidator = mediaReferenceValidator;
        this.membershipCheck = membershipCheck;
        this.handleGenerator = handleGenerator;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId, ExperienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        var name = ExperienceInputMapper.name(input);
        var description = ExperienceInputMapper.description(input);
        var longDescription = ExperienceInputMapper.longDescription(input);
        var mediaIds = ExperienceInputMapper.mediaIds(input);
        var cutoff = ExperienceInputMapper.bookingCutoffHours(input);

        mediaReferenceValidator.validate(tourOperatorId, mediaIds, input.thumbnailMediaId());

        Experience saved = null;
        for (int attempt = 0; attempt < MAX_HANDLE_ATTEMPTS; attempt++) {
            Handle handle = handleGenerator.generateUnique(name.value(), candidate ->
                    experienceRepository.existsByTourOperatorIdAndHandle(tourOperatorId, candidate)
                            || translationRepository.existsByHandleInAnyLocale(tourOperatorId, candidate));
            Experience experience = Experience.create(
                    idGenerator.newId(), tourOperatorId, callerUserId, handle,
                    name, description, longDescription, input.isFeatured(),
                    mediaIds, input.thumbnailMediaId(), cutoff,
                    ExperienceInputMapper.seoTitle(input), ExperienceInputMapper.seoDescription(input),
                    ExperienceInputMapper.startingPrice(input));
            try {
                saved = transactionRunner.call(() -> {
                    Experience persisted = experienceRepository.save(experience);
                    auditTrailPort.append(new NewAuditEntry(
                            tourOperatorId, AuditActor.user(callerUserId),
                            "EXPERIENCE", persisted.getId(), "experience.created", null));
                    return persisted;
                });
                break;
            } catch (UniqueConstraintViolationException e) {
                // handle race — regenerate and retry
            }
        }
        if (saved == null) {
            throw new InvalidFieldException("Could not generate a unique handle — please retry");
        }
        return saved.getId();
    }
}
