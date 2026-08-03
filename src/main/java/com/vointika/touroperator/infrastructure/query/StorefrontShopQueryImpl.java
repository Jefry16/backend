package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorTranslationJpaRepository;
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
 *
 * <p>Both overlays are nullable-wins-canonical, per the port. The gate's message
 * overlays from the <b>primary</b> locale, because the gate answers before any
 * locale has been resolved.
 */
@Component
public class StorefrontShopQueryImpl implements StorefrontShopQuery {

    private final TourOperatorJpaRepository operatorRepository;
    private final TourOperatorTranslationJpaRepository translationRepository;
    private final MediaKeyBatchQuery mediaKeyBatchQuery;

    public StorefrontShopQueryImpl(TourOperatorJpaRepository operatorRepository,
                                   TourOperatorTranslationJpaRepository translationRepository,
                                   MediaKeyBatchQuery mediaKeyBatchQuery) {
        this.operatorRepository = operatorRepository;
        this.translationRepository = translationRepository;
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
    }

    @Override
    public Optional<StorefrontGateView> findGate(String handle) {
        return operatorRepository.findByHandle(handle).map(this::toGateView);
    }

    @Override
    public Optional<StorefrontShopView> findContent(UUID tourOperatorId, String locale) {
        return operatorRepository.findById(tourOperatorId)
                .map(operator -> toContentView(operator, locale));
    }

    private StorefrontGateView toGateView(TourOperatorJpaEntity operator) {
        String translatedMessage = translation(operator.getId(), operator.getPrimaryLocale())
                .map(TourOperatorTranslationJpaEntity::getPasswordMessage)
                .orElse(null);
        return new StorefrontGateView(
                operator.getId(),
                operator.isPasswordEnabled(),
                operator.getStorefrontPassword(),
                overlay(translatedMessage, operator.getPasswordMessage()),
                operator.getPrimaryLocale(),
                Set.copyOf(operator.getSupportedLocales()));
    }

    private StorefrontShopView toContentView(TourOperatorJpaEntity operator, String locale) {
        Optional<TourOperatorTranslationJpaEntity> translation = translation(operator.getId(), locale);
        Map<UUID, String> keys = mediaKeys(operator);
        return new StorefrontShopView(
                operator.getName(),
                keyOf(keys, operator.getLogoMediaId()),
                keyOf(keys, operator.getOgImageMediaId()),
                overlay(translation.map(TourOperatorTranslationJpaEntity::getSeoTitle).orElse(null),
                        operator.getSeoTitle()),
                overlay(translation.map(TourOperatorTranslationJpaEntity::getSeoDescription).orElse(null),
                        operator.getSeoDescription()));
    }

    private Optional<TourOperatorTranslationJpaEntity> translation(UUID tourOperatorId, String locale) {
        return translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale);
    }

    /** Only {@code null} falls back — an operator who translated a field to blank meant blank. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
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
