package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Lists every value stored on one resource, joined with its definition
 * (namespace, key, type, display name). Definitions without a value on this
 * resource simply don't appear — the admin editor overlays this onto the
 * definitions list. Any member; owner must belong to the operator (404).
 */
public class ListMetafieldValuesUseCase {

    private final MetafieldValueRepository valueRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetafieldValuesUseCase(MetafieldValueRepository valueRepository,
                                      MetafieldOwnerAccess ownerAccess,
                                      TourOperatorMembershipCheck membershipCheck) {
        this.valueRepository = valueRepository;
        this.ownerAccess = ownerAccess;
        this.membershipCheck = membershipCheck;
    }

    public List<MetafieldValueWithDefinition> execute(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        ownerAccess.ensureOwned(ownerType, ownerId, tourOperatorId);
        return valueRepository.listForOwner(tourOperatorId, ownerType, ownerId);
    }
}
