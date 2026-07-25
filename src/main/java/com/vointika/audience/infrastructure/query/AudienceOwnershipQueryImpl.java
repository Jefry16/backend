package com.vointika.audience.infrastructure.query;

import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.shared.port.AudienceOwnershipQuery;
import com.vointika.shared.port.AudienceView;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * audience's adapter for the shared {@link AudienceOwnershipQuery} seam: resolves
 * an operator's audience for the experience context to validate + snapshot at
 * slot creation.
 */
@Component
public class AudienceOwnershipQueryImpl implements AudienceOwnershipQuery {

    private final AudienceRepository audienceRepository;

    public AudienceOwnershipQueryImpl(AudienceRepository audienceRepository) {
        this.audienceRepository = audienceRepository;
    }

    @Override
    public Optional<AudienceView> findForTourOperator(UUID audienceId, UUID tourOperatorId) {
        return audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId)
                .map(a -> new AudienceView(a.getId(), a.getName().value(), a.getPaxPerUnit().value()));
    }
}
