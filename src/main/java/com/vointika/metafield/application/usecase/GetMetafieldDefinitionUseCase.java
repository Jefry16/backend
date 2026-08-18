package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** A single definition. Any member; cross-tenant → byte-identical 404. */
public class GetMetafieldDefinitionUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMetafieldDefinitionUseCase(MetafieldDefinitionRepository definitionRepository,
                                         TourOperatorMembershipCheck membershipCheck) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
    }

    public MetafieldDefinition execute(UUID tourOperatorId, UUID definitionId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return definitionRepository.requireByIdAndTourOperatorId(definitionId, tourOperatorId);
    }
}
