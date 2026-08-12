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
 *
 * <p><b>{@code TOUR_OPERATOR} needs no seam</b>, which is the one case that
 * looks like an omission and is not: the owner <em>is</em> the tenant, so
 * "does this row belong to the operator" is an equality rather than a question
 * for another context. The check still has to happen — a caller could name any
 * UUID — and it still answers the same 404 the other two do.
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
            case TOUR_OPERATOR -> tourOperatorId.equals(ownerId);
        };
        if (!owned) {
            throw new ResourceNotFoundException(switch (ownerType) {
                case EXPERIENCE -> "Experience not found";
                case PAGE -> "Page not found";
                case TOUR_OPERATOR -> "Tour operator not found";
            });
        }
    }
}
