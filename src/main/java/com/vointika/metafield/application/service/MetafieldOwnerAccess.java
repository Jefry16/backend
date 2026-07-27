package com.vointika.metafield.application.service;

import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.PageOwnershipQuery;

import java.util.UUID;

/**
 * The owner-type dispatch for value reads/writes: verifies the owning
 * resource belongs to the operator (byte-identical 404 per kind) through the
 * owning context's shared seam. Adding an owner type = one enum value, one
 * seam, one case here.
 */
public class MetafieldOwnerAccess {

    private final ExperienceOwnershipQuery experienceOwnershipQuery;
    private final PageOwnershipQuery pageOwnershipQuery;

    public MetafieldOwnerAccess(ExperienceOwnershipQuery experienceOwnershipQuery,
                                PageOwnershipQuery pageOwnershipQuery) {
        this.experienceOwnershipQuery = experienceOwnershipQuery;
        this.pageOwnershipQuery = pageOwnershipQuery;
    }

    /** @throws ResourceNotFoundException when the owner is missing or another operator's */
    public void ensureOwned(MetafieldOwnerType ownerType, UUID ownerId, UUID tourOperatorId) {
        boolean owned = switch (ownerType) {
            case EXPERIENCE -> experienceOwnershipQuery.existsForTourOperator(ownerId, tourOperatorId);
            case PAGE -> pageOwnershipQuery.existsForTourOperator(ownerId, tourOperatorId);
        };
        if (!owned) {
            throw new ResourceNotFoundException(switch (ownerType) {
                case EXPERIENCE -> "Experience not found";
                case PAGE -> "Page not found";
            });
        }
    }
}
