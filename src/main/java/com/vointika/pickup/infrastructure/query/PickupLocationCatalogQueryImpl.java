package com.vointika.pickup.infrastructure.query;

import com.vointika.pickup.infrastructure.persistence.repository.PickupLocationJpaRepository;
import com.vointika.shared.port.PickupLocationCatalogQuery;
import com.vointika.shared.port.PickupLocationView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * pickup's adapter for the shared {@link PickupLocationCatalogQuery} seam: the
 * operator's full catalog, for the experience context to snapshot onto slots at
 * creation time.
 */
@Component
public class PickupLocationCatalogQueryImpl implements PickupLocationCatalogQuery {

    private final PickupLocationJpaRepository jpaRepository;

    public PickupLocationCatalogQueryImpl(PickupLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<PickupLocationView> findAllForTourOperator(UUID tourOperatorId) {
        return jpaRepository.findAllByTourOperatorId(tourOperatorId).stream()
                .map(p -> new PickupLocationView(p.getId(), p.getName(), p.getTime()))
                .toList();
    }
}
