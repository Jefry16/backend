package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * touroperator's adapter for the shared {@link StorefrontShopQuery} seam.
 *
 * <p>The two media references are resolved in <b>one</b> batch call — that is
 * what {@link MediaKeyBatchQuery} is for, and the storefront's home page is on
 * the render path for every request. An id the operator no longer owns is
 * simply absent from the result, so the key stays null and the page renders
 * without that image rather than erroring.
 */
@Component
public class StorefrontShopQueryImpl implements StorefrontShopQuery {

    private final TourOperatorJpaRepository operatorRepository;
    private final MediaKeyBatchQuery mediaKeyBatchQuery;

    public StorefrontShopQueryImpl(TourOperatorJpaRepository operatorRepository,
                                   MediaKeyBatchQuery mediaKeyBatchQuery) {
        this.operatorRepository = operatorRepository;
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
    }

    @Override
    public Optional<StorefrontShopView> findByHandle(String handle) {
        return operatorRepository.findByHandle(handle).map(this::toView);
    }

    private StorefrontShopView toView(TourOperatorJpaEntity operator) {
        Map<UUID, String> keys = mediaKeys(operator);
        return new StorefrontShopView(
                operator.getName(),
                operator.getHandle(),
                keyOf(keys, operator.getLogoMediaId()),
                keyOf(keys, operator.getOgImageMediaId()),
                operator.getSeoTitle(),
                operator.getSeoDescription());
    }

    /** Never {@code keys.get(null)} — an immutable {@code Map.of()} throws on a null key. */
    private static String keyOf(Map<UUID, String> keys, UUID mediaId) {
        return mediaId == null ? null : keys.get(mediaId);
    }

    private Map<UUID, String> mediaKeys(TourOperatorJpaEntity operator) {
        Set<UUID> ids = new HashSet<>();
        if (operator.getLogoMediaId() != null) {
            ids.add(operator.getLogoMediaId());
        }
        if (operator.getOgImageMediaId() != null) {
            ids.add(operator.getOgImageMediaId());
        }
        return ids.isEmpty() ? Map.of() : mediaKeyBatchQuery.findKeysByIds(operator.getId(), ids);
    }
}
