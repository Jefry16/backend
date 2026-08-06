package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorBrandColorJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorBrandJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorBrandSocialLinkJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorPolicyJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorPolicyTranslationJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorTranslationJpaRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * touroperator's adapter for the shared {@link StorefrontShopQuery} seam.
 *
 * <p>Every media reference the chrome needs — the OG image and the brand's four
 * images — is resolved in <b>one</b> batch call: that is what
 * {@link MediaAssetBatchQuery} is for, and this runs on every storefront
 * request. An id the operator no longer owns is simply absent from the result,
 * so the asset stays null and the page renders without that image rather than
 * erroring.
 *
 * <p>Every overlay here is nullable-wins-canonical, per the port — the
 * operator's own translation row, and now the policies' as well. The gate's
 * message overlays from the <b>primary</b> locale, because the gate answers
 * before any locale has been resolved.
 *
 * <p>The currency is a direct read of {@code reference}, not a second port:
 * {@code reference} is a shared kernel every context may import, and this
 * context already FKs into it.
 *
 * <p><b>An operator with no brand row is not an error.</b> V10 backfilled one
 * per operator, but an operator created since has none — the create path does
 * not write one — so the empty brand is a real case and answers with null fields
 * rather than with a null object.
 */
@Component
public class StorefrontShopQueryImpl implements StorefrontShopQuery {

    private static final StorefrontBrandColorsView NO_COLORS =
            new StorefrontBrandColorsView(List.of(), List.of());

    private static final StorefrontBrandView NO_BRAND =
            new StorefrontBrandView(null, null, null, null, null, null, NO_COLORS, List.of());

    private final TourOperatorJpaRepository operatorRepository;
    private final TourOperatorTranslationJpaRepository translationRepository;
    private final TourOperatorBrandJpaRepository brandRepository;
    private final TourOperatorBrandColorJpaRepository brandColorRepository;
    private final TourOperatorBrandSocialLinkJpaRepository brandSocialLinkRepository;
    private final TourOperatorPolicyJpaRepository policyRepository;
    private final TourOperatorPolicyTranslationJpaRepository policyTranslationRepository;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final CurrencyRepository currencyRepository;
    private final TimezoneRepository timezoneRepository;

    public StorefrontShopQueryImpl(TourOperatorJpaRepository operatorRepository,
                                   TourOperatorTranslationJpaRepository translationRepository,
                                   TourOperatorBrandJpaRepository brandRepository,
                                   TourOperatorBrandColorJpaRepository brandColorRepository,
                                   TourOperatorBrandSocialLinkJpaRepository brandSocialLinkRepository,
                                   TourOperatorPolicyJpaRepository policyRepository,
                                   TourOperatorPolicyTranslationJpaRepository policyTranslationRepository,
                                   MediaAssetBatchQuery mediaAssetBatchQuery,
                                   CurrencyRepository currencyRepository,
                                   TimezoneRepository timezoneRepository) {
        this.operatorRepository = operatorRepository;
        this.translationRepository = translationRepository;
        this.brandRepository = brandRepository;
        this.brandColorRepository = brandColorRepository;
        this.brandSocialLinkRepository = brandSocialLinkRepository;
        this.policyRepository = policyRepository;
        this.policyTranslationRepository = policyTranslationRepository;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.currencyRepository = currencyRepository;
        this.timezoneRepository = timezoneRepository;
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

    /**
     * An unrecognised name is empty rather than an exception: it arrives from a
     * URL segment, and {@code PolicyType.valueOf} would turn
     * {@code /policies/refunds} into a 500 in the API's JSON error shape, on an
     * HTML page.
     */
    @Override
    public Optional<StorefrontPolicyView> findPolicy(UUID tourOperatorId, String type, String locale) {
        return PolicyType.from(type)
                .flatMap(policyType -> policyRepository.findByTourOperatorIdAndType(tourOperatorId, policyType))
                .map(policy -> toPolicyView(policy, tourOperatorId, locale));
    }

    private StorefrontPolicyView toPolicyView(TourOperatorPolicyJpaEntity policy,
                                              UUID tourOperatorId,
                                              String locale) {
        Optional<TourOperatorPolicyTranslationJpaEntity> translation = policyTranslationRepository
                .findByTourOperatorIdAndTypeAndLocale(tourOperatorId, policy.getType(), locale);
        return new StorefrontPolicyView(
                policy.getType().name(),
                overlay(translation.map(TourOperatorPolicyTranslationJpaEntity::getTitle).orElse(null),
                        policy.getTitle()),
                overlay(translation.map(TourOperatorPolicyTranslationJpaEntity::getBody).orElse(null),
                        policy.getBody()));
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
        Optional<TourOperatorBrandJpaEntity> brand = brandRepository.findById(operator.getId());
        Map<UUID, MediaAsset> assets = mediaAssets(operator, brand.orElse(null));
        // A NOT NULL FK, so the row is there; absent is treated like an absent
        // media key rather than thrown, because a public page must still render.
        Optional<Currency> currency = currencyRepository.findById(operator.getCurrencyId());
        Optional<Timezone> timezone = timezoneRepository.findById(operator.getTimezoneId());
        return new StorefrontShopView(
                operator.getName(),
                operator.getAddress(),
                operator.getPhone(),
                operator.getEmail(),
                keyOf(assets, operator.getOgImageMediaId()),
                currency.map(Currency::getCode).orElse(null),
                currency.map(Currency::getSymbol).orElse(null),
                timezone.map(Timezone::getName).orElse(null),
                timezone.map(Timezone::getCityName).orElse(null),
                overlay(translation.map(TourOperatorTranslationJpaEntity::getSeoTitle).orElse(null),
                        operator.getSeoTitle()),
                overlay(translation.map(TourOperatorTranslationJpaEntity::getSeoDescription).orElse(null),
                        operator.getSeoDescription()),
                overlay(translation.map(TourOperatorTranslationJpaEntity::getPasswordMessage).orElse(null),
                        operator.getPasswordMessage()),
                brand.map(row -> toBrandView(row, translation.orElse(null), assets)).orElse(NO_BRAND),
                policySummaries(operator.getId(), locale));
    }

    /**
     * The footer's links: the policies this operator has written, each title
     * overlaid for the locale. Two queries rather than four — one for the rows
     * and one for the locale's whole overlay set — because this runs on every
     * page and there are at most four of each.
     *
     * <p>The order is the query's, and re-sorting here would be a second copy of
     * the rule.
     */
    private List<StorefrontPolicySummaryView> policySummaries(UUID tourOperatorId, String locale) {
        List<TourOperatorPolicyJpaEntity> policies =
                policyRepository.findByTourOperatorIdOrderByTypeAsc(tourOperatorId);
        if (policies.isEmpty()) {
            return List.of();
        }
        Map<PolicyType, TourOperatorPolicyTranslationJpaEntity> translations =
                policyTranslationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale).stream()
                        .collect(Collectors.toMap(TourOperatorPolicyTranslationJpaEntity::getType,
                                Function.identity(), (first, second) -> first, () -> new EnumMap<>(PolicyType.class)));
        return policies.stream()
                .map(policy -> new StorefrontPolicySummaryView(
                        policy.getType().name(),
                        overlay(title(translations.get(policy.getType())), policy.getTitle())))
                .toList();
    }

    private static String title(TourOperatorPolicyTranslationJpaEntity translation) {
        return translation == null ? null : translation.getTitle();
    }

    private StorefrontBrandView toBrandView(TourOperatorBrandJpaEntity brand,
                                            TourOperatorTranslationJpaEntity translation,
                                            Map<UUID, MediaAsset> assets) {
        return new StorefrontBrandView(
                overlay(translation == null ? null : translation.getSlogan(), brand.getSlogan()),
                overlay(translation == null ? null : translation.getShortDescription(),
                        brand.getShortDescription()),
                assetOf(assets, brand.getLogoMediaId()),
                assetOf(assets, brand.getSquareLogoMediaId()),
                assetOf(assets, brand.getFaviconMediaId()),
                assetOf(assets, brand.getCoverImageMediaId()),
                colors(brand.getTourOperatorId()),
                socialLinks(brand.getTourOperatorId()));
    }

    /**
     * One ordered read, split by role in memory — a palette is a handful of rows,
     * and two queries would be two round trips for one page.
     *
     * <p>The order is the query's; sorting again here would be a second copy of
     * the rule, and the role never leaves this method.
     */
    private StorefrontBrandColorsView colors(UUID tourOperatorId) {
        Map<BrandColorRole, List<StorefrontBrandColorView>> byRole =
                brandColorRepository.findByTourOperatorIdOrderByPositionAsc(tourOperatorId).stream()
                        .collect(Collectors.groupingBy(
                                TourOperatorBrandColorJpaEntity::getRole,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        color -> new StorefrontBrandColorView(
                                                color.getBackground(), color.getForeground()),
                                        Collectors.toList())));
        return new StorefrontBrandColorsView(
                List.copyOf(byRole.getOrDefault(BrandColorRole.PRIMARY, List.of())),
                List.copyOf(byRole.getOrDefault(BrandColorRole.SECONDARY, List.of())));
    }

    private List<StorefrontBrandSocialLinkView> socialLinks(UUID tourOperatorId) {
        return brandSocialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(tourOperatorId).stream()
                .map(link -> new StorefrontBrandSocialLinkView(link.getPlatform().name(), link.getUrl()))
                .toList();
    }

    private Optional<TourOperatorTranslationJpaEntity> translation(UUID tourOperatorId, String locale) {
        return translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale);
    }

    /** Only {@code null} falls back — an operator who translated a field to blank meant blank. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }

    /** Never {@code assets.get(null)} — an immutable {@code Map.of()} throws on a null key. */
    private static MediaAsset assetOf(Map<UUID, MediaAsset> assets, UUID mediaId) {
        return mediaId == null ? null : assets.get(mediaId);
    }

    /** The OG image is a bare key: it is a meta tag, not an {@code <img>}. */
    private static String keyOf(Map<UUID, MediaAsset> assets, UUID mediaId) {
        MediaAsset asset = assetOf(assets, mediaId);
        return asset == null ? null : asset.storageKey();
    }

    private Map<UUID, MediaAsset> mediaAssets(TourOperatorJpaEntity operator, TourOperatorBrandJpaEntity brand) {
        Set<UUID> ids = new HashSet<>();
        addIfPresent(ids, operator.getOgImageMediaId());
        if (brand != null) {
            addIfPresent(ids, brand.getLogoMediaId());
            addIfPresent(ids, brand.getSquareLogoMediaId());
            addIfPresent(ids, brand.getFaviconMediaId());
            addIfPresent(ids, brand.getCoverImageMediaId());
        }
        return ids.isEmpty() ? Map.of() : mediaAssetBatchQuery.findAssetsByIds(operator.getId(), ids);
    }

    private static void addIfPresent(Set<UUID> ids, UUID mediaId) {
        if (mediaId != null) {
            ids.add(mediaId);
        }
    }
}
