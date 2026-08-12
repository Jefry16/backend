package com.vointika.metafield.infrastructure.query;

import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * metafield's implementation of the storefront's read.
 *
 * <p>It reuses the same {@code listForOwner} the admin editor uses, with the
 * operator as both tenant and owner — the ordering the storefront needs
 * (namespace, then key) is already that query's promise, so there is no second
 * query and no second place for the order to be decided.
 */
@Component
public class StorefrontMetafieldQueryImpl implements StorefrontMetafieldQuery {

    private final MetafieldValueRepository valueRepository;

    public StorefrontMetafieldQueryImpl(MetafieldValueRepository valueRepository) {
        this.valueRepository = valueRepository;
    }

    @Override
    public List<MetafieldView> findForOperator(UUID tourOperatorId) {
        return valueRepository
                .listForOwner(tourOperatorId, MetafieldOwnerType.TOUR_OPERATOR, tourOperatorId)
                .stream()
                .map(v -> new MetafieldView(v.namespace(), v.key(), v.type().code(), v.value()))
                .toList();
    }
}
