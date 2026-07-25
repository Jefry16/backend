package com.vointika.experience.application.service;

import com.vointika.experience.application.dto.input.AudiencePricingInput;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.valueobject.Capacity;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AudienceOwnershipQuery;
import com.vointika.shared.port.AudienceView;
import com.vointika.shared.service.IdGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared by the single + recurring slot creators: validates the per-audience
 * pricing input, resolves each audience against the operator's catalog (snapshot
 * source), and builds the frozen pricing rows for a slot. Price + capacity are
 * frozen here; name + paxPerUnit are snapshotted from the resolved audience.
 */
public class AudiencePricingResolver {

    private final AudienceOwnershipQuery audienceOwnershipQuery;
    private final IdGenerator idGenerator;

    public AudiencePricingResolver(AudienceOwnershipQuery audienceOwnershipQuery, IdGenerator idGenerator) {
        this.audienceOwnershipQuery = audienceOwnershipQuery;
        this.idGenerator = idGenerator;
    }

    /**
     * Validates (non-empty, no duplicate audience, valid price/capacity) and
     * resolves each audience to the operator's catalog entry — in input order.
     * Unknown / foreign audience → 404. Runs before any slot is written so a bad
     * batch fails whole.
     */
    public List<AudienceView> validateAndResolve(List<AudiencePricingInput> prices, UUID tourOperatorId) {
        if (prices == null || prices.isEmpty()) {
            throw new InvalidFieldException("At least one audience price is required");
        }
        Set<UUID> seen = new HashSet<>();
        List<AudienceView> resolved = new ArrayList<>(prices.size());
        for (AudiencePricingInput ap : prices) {
            if (ap.audienceId() == null || !seen.add(ap.audienceId())) {
                throw new InvalidFieldException("Duplicate or missing audience in audiencePrices");
            }
            // Validate the money/seat value objects eagerly (422 before any insert).
            new Price(ap.price());
            new Capacity(ap.capacity());
            resolved.add(audienceOwnershipQuery.findForTourOperator(ap.audienceId(), tourOperatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Audience not found")));
        }
        return resolved;
    }

    /** Builds the frozen pricing rows for a slot from the already-resolved audiences. */
    public List<SlotAudiencePricing> buildRows(UUID slotId,
                                               List<AudiencePricingInput> prices,
                                               List<AudienceView> resolved) {
        List<SlotAudiencePricing> rows = new ArrayList<>(prices.size());
        for (int i = 0; i < prices.size(); i++) {
            AudiencePricingInput ap = prices.get(i);
            AudienceView view = resolved.get(i);
            rows.add(new SlotAudiencePricing(
                    idGenerator.newId(),
                    slotId,
                    view.id(),
                    view.name(),
                    new Price(ap.price()).value(),
                    new Capacity(ap.capacity()).value(),
                    view.paxPerUnit()));
        }
        return rows;
    }
}
