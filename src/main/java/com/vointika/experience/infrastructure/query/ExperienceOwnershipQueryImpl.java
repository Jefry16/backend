package com.vointika.experience.infrastructure.query;

import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** experience's adapter for the shared ownership seam (metafield value writes). */
@Component
public class ExperienceOwnershipQueryImpl implements ExperienceOwnershipQuery {

    private final ExperienceRepository experienceRepository;

    public ExperienceOwnershipQueryImpl(ExperienceRepository experienceRepository) {
        this.experienceRepository = experienceRepository;
    }

    @Override
    public boolean existsForTourOperator(UUID experienceId, UUID tourOperatorId) {
        return experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId).isPresent();
    }
}
