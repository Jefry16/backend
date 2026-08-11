package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.StorefrontTenantQuery;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import org.springframework.stereotype.Component;

/**
 * touroperator's side of {@link StorefrontTenantQuery} — what is left of
 * {@code StorefrontShopQueryImpl} after the storefront became a placeholder.
 *
 * <p>The handle is matched exactly, as it was before: it is the tenant's
 * address, and a case-folded or trimmed match would make one storefront
 * reachable at two hostnames. {@code TenantHandleResolver} lowercases the host
 * before it gets here, and handles are stored lowercase.
 */
@Component
public class StorefrontTenantQueryImpl implements StorefrontTenantQuery {

    private final TourOperatorJpaRepository operatorRepository;

    public StorefrontTenantQueryImpl(TourOperatorJpaRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    @Override
    public boolean exists(String handle) {
        return operatorRepository.existsByHandle(handle);
    }
}
