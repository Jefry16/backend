package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.output.MetaobjectDefinitionView;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** A single definition with its ordered fields. Any member; cross-tenant → 404. */
public class GetMetaobjectDefinitionUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMetaobjectDefinitionUseCase(MetaobjectDefinitionRepository definitionRepository,
                                          TourOperatorMembershipCheck membershipCheck) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
    }

    public MetaobjectDefinitionView execute(UUID tourOperatorId, UUID definitionId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        MetaobjectDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));
        return new MetaobjectDefinitionView(definition, definitionRepository.fieldsOf(definitionId));
    }
}
