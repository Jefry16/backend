package com.vointika.metafield.infrastructure.integration;

import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.shared.port.MetafieldValueCleanup;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Metafield's side of {@link MetafieldValueCleanup}. */
@Component
public class MetafieldValueCleanupImpl implements MetafieldValueCleanup {

    private final MetafieldValueRepository valueRepository;

    public MetafieldValueCleanupImpl(MetafieldValueRepository valueRepository) {
        this.valueRepository = valueRepository;
    }

    @Override
    public void deleteValuesOwnedBy(UUID ownerId) {
        valueRepository.deleteByOwnerId(ownerId);
    }
}
