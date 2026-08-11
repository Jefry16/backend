package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * touroperator's implementation of the storefront's shop read.
 *
 * <p><b>The role split and the colour order happen here, not in the caller.</b>
 * {@code BrandColorRole} is this context's enum, so a role-tagged flat list
 * would make {@code storefront} compare against literals it is fenced from
 * seeing; and {@code colors.primary[0]} is an address a theme indexes into, so
 * the order is this query's promise. It lives in the derived query's name
 * ({@code findByTourOperatorIdOrderByPositionAsc}) and nowhere else — the split
 * below preserves it, because ordering by position keeps each role's rows in
 * relative order however they interleave.
 *
 * <p><b>An operator with no brand row still renders a page.</b> Nothing writes a
 * brand at creation, so its absence is ordinary rather than exceptional and the
 * view carries an empty brand, never null — the same answer the admin
 * {@code GET .../brand} gives.
 */
@Component
public class StorefrontShopQueryImpl implements StorefrontShopQuery {

    private static final BrandView EMPTY_BRAND =
            new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of());

    private final TourOperatorJpaRepository operatorRepository;
    private final TourOperatorTranslationJpaRepository translationRepository;
    private final TourOperatorBrandJpaRepository brandRepository;
    private final TourOperatorBrandColorJpaRepository colorRepository;
    private final TourOperatorBrandSocialLinkJpaRepository socialLinkRepository;
    private final TourOperatorPolicyJpaRepository policyRepository;
    private final TourOperatorPolicyTranslationJpaRepository policyTranslationRepository;
    private final CurrencyRepository currencyRepository;
    private final TimezoneRepository timezoneRepository;

    public StorefrontShopQueryImpl(TourOperatorJpaRepository operatorRepository,
                                   TourOperatorTranslationJpaRepository translationRepository,
                                   TourOperatorBrandJpaRepository brandRepository,
                                   TourOperatorBrandColorJpaRepository colorRepository,
                                   TourOperatorBrandSocialLinkJpaRepository socialLinkRepository,
                                   TourOperatorPolicyJpaRepository policyRepository,
                                   TourOperatorPolicyTranslationJpaRepository policyTranslationRepository,
                                   CurrencyRepository currencyRepository,
                                   TimezoneRepository timezoneRepository) {
        this.operatorRepository = operatorRepository;
        this.translationRepository = translationRepository;
        this.brandRepository = brandRepository;
        this.colorRepository = colorRepository;
        this.socialLinkRepository = socialLinkRepository;
        this.policyRepository = policyRepository;
        this.policyTranslationRepository = policyTranslationRepository;
        this.currencyRepository = currencyRepository;
        this.timezoneRepository = timezoneRepository;
    }

    @Override
    public Optional<ShopLocalesView> findLocales(String handle) {
        return operatorRepository.findByHandle(handle)
                .map(o -> new ShopLocalesView(o.getId(), o.getPrimaryLocale(),
                        Set.copyOf(o.getSupportedLocales())));
    }

    @Override
    public Optional<ShopView> findShop(UUID tourOperatorId, String locale) {
        return operatorRepository.findById(tourOperatorId)
                .map(operator -> buildShop(operator, locale));
    }

    private ShopView buildShop(TourOperatorJpaEntity operator, String locale) {
        UUID id = operator.getId();
        TourOperatorTranslationJpaEntity translation =
                translationRepository.findByTourOperatorIdAndLocale(id, locale).orElse(null);
        Currency currency = currencyRepository.findById(operator.getCurrencyId()).orElse(null);
        Timezone timezone = timezoneRepository.findById(operator.getTimezoneId()).orElse(null);

        return new ShopView(
                id,
                operator.getName(),
                operator.getHandle(),
                operator.getAddress(),
                operator.getPhone(),
                operator.getEmail(),
                overlay(translation == null ? null : translation.getSeoTitle(), operator.getSeoTitle()),
                overlay(translation == null ? null : translation.getSeoDescription(), operator.getSeoDescription()),
                operator.getOgImageMediaId(),
                overlay(translation == null ? null : translation.getPasswordMessage(), operator.getPasswordMessage()),
                currency == null ? null : currency.getCode(),
                currency == null ? null : currency.getSymbol(),
                timezone == null ? null : timezone.getName(),
                timezone == null ? null : timezone.getCityName(),
                brand(id, translation),
                policies(id, locale));
    }

    private BrandView brand(UUID tourOperatorId, TourOperatorTranslationJpaEntity translation) {
        Optional<TourOperatorBrandJpaEntity> row = brandRepository.findById(tourOperatorId);
        if (row.isEmpty()) {
            return EMPTY_BRAND;
        }
        TourOperatorBrandJpaEntity brand = row.get();

        Map<BrandColorRole, List<ColorView>> colors = new EnumMap<>(BrandColorRole.class);
        for (TourOperatorBrandColorJpaEntity color : colorRepository.findByTourOperatorIdOrderByPositionAsc(tourOperatorId)) {
            colors.computeIfAbsent(color.getRole(), r -> new ArrayList<>())
                    .add(new ColorView(color.getBackground(), color.getForeground()));
        }

        List<SocialLinkView> socialLinks =
                socialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(tourOperatorId).stream()
                        .map(link -> new SocialLinkView(link.getPlatform().name(), link.getUrl()))
                        .toList();

        return new BrandView(
                overlay(translation == null ? null : translation.getSlogan(), brand.getSlogan()),
                overlay(translation == null ? null : translation.getShortDescription(), brand.getShortDescription()),
                brand.getLogoMediaId(),
                brand.getSquareLogoMediaId(),
                brand.getFaviconMediaId(),
                brand.getCoverImageMediaId(),
                List.copyOf(colors.getOrDefault(BrandColorRole.PRIMARY, List.of())),
                List.copyOf(colors.getOrDefault(BrandColorRole.SECONDARY, List.of())),
                socialLinks);
    }

    private List<PolicyView> policies(UUID tourOperatorId, String locale) {
        Map<PolicyType, String> translatedTitles = new EnumMap<>(PolicyType.class);
        for (TourOperatorPolicyTranslationJpaEntity t :
                policyTranslationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale)) {
            if (t.getTitle() != null) {
                translatedTitles.put(t.getType(), t.getTitle());
            }
        }

        return policyRepository.findByTourOperatorIdOrderByTypeAsc(tourOperatorId).stream()
                .map(policy -> new PolicyView(
                        policy.getId(),
                        policy.getType().name(),
                        overlay(translatedTitles.get(policy.getType()), policy.getTitle())))
                .toList();
    }

    /** Nullable-wins-canonical: a translation column that is null falls back rather than blanking the field. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
