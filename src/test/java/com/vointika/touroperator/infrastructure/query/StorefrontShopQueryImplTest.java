package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandColorView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandSocialLinkView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicySummaryView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicyView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandSocialLinkJpaEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The brand half of touroperator's storefront seam. The rest of the view
 * (currency, timezone, SEO) has been exercised end to end by the storefront's
 * own tests since #88; what V10 adds is a second row, two child tables, one more
 * overlay pair and four more media references sharing the OG image's batch call.
 */
class StorefrontShopQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("01900000-0000-7000-8000-000000000002");
    private static final UUID CURRENCY = UUID.fromString("01900000-0000-7000-8000-0000000000c1");
    private static final UUID TIMEZONE = UUID.fromString("01900000-0000-7000-8000-0000000000d1");
    private static final UUID LOGO = UUID.fromString("01900000-0000-7000-8000-0000000000b1");
    private static final UUID FAVICON = UUID.fromString("01900000-0000-7000-8000-0000000000b2");
    private static final UUID OG_IMAGE = UUID.fromString("01900000-0000-7000-8000-0000000000b3");

    private TourOperatorJpaRepository operatorRepository;
    private TourOperatorTranslationJpaRepository translationRepository;
    private TourOperatorBrandJpaRepository brandRepository;
    private TourOperatorBrandColorJpaRepository brandColorRepository;
    private TourOperatorBrandSocialLinkJpaRepository brandSocialLinkRepository;
    private TourOperatorPolicyJpaRepository policyRepository;
    private TourOperatorPolicyTranslationJpaRepository policyTranslationRepository;
    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private StorefrontShopQueryImpl query;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorJpaRepository.class);
        translationRepository = mock(TourOperatorTranslationJpaRepository.class);
        brandRepository = mock(TourOperatorBrandJpaRepository.class);
        brandColorRepository = mock(TourOperatorBrandColorJpaRepository.class);
        brandSocialLinkRepository = mock(TourOperatorBrandSocialLinkJpaRepository.class);
        policyRepository = mock(TourOperatorPolicyJpaRepository.class);
        policyTranslationRepository = mock(TourOperatorPolicyTranslationJpaRepository.class);
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
        TimezoneRepository timezoneRepository = mock(TimezoneRepository.class);
        query = new StorefrontShopQueryImpl(operatorRepository, translationRepository, brandRepository,
                brandColorRepository, brandSocialLinkRepository, policyRepository,
                policyTranslationRepository, mediaAssetBatchQuery, currencyRepository, timezoneRepository);

        when(operatorRepository.findById(OPERATOR)).thenReturn(Optional.of(operator()));
        when(translationRepository.findByTourOperatorIdAndLocale(any(), any())).thenReturn(Optional.empty());
        when(brandRepository.findById(OPERATOR)).thenReturn(Optional.empty());
        when(brandColorRepository.findByTourOperatorIdOrderByPositionAsc(OPERATOR)).thenReturn(List.of());
        when(brandSocialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(OPERATOR)).thenReturn(List.of());
        when(policyRepository.findByTourOperatorIdOrderByTypeAsc(OPERATOR)).thenReturn(List.of());
        when(policyTranslationRepository.findByTourOperatorIdAndLocale(any(), any())).thenReturn(List.of());
        when(policyRepository.findByTourOperatorIdAndType(any(), any())).thenReturn(Optional.empty());
        when(policyTranslationRepository.findByTourOperatorIdAndTypeAndLocale(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(currencyRepository.findById(CURRENCY))
                .thenReturn(Optional.of(new Currency(CURRENCY, "EUR", "Euro", "€")));
        when(timezoneRepository.findById(TIMEZONE))
                .thenReturn(Optional.of(new Timezone(TIMEZONE, "Europe/Madrid", "Madrid", null)));
    }

    @Test
    void aTranslationOverlaysTheSloganAndTheShortDescription() {
        brand("Sail the coast", "Small-group sailing since 2011");
        translated("es", "Navega la costa", "Salidas en grupos pequeños");

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().slogan()).isEqualTo("Navega la costa");
        assertThat(shop.brand().shortDescription()).isEqualTo("Salidas en grupos pequeños");
    }

    /** A row overlays, it does not replace: a null column falls back per column, not per row. */
    @Test
    void aColumnTheTranslationLeftNullFallsBackToTheCanonicalValue() {
        brand("Sail the coast", "Small-group sailing since 2011");
        translated("es", "Navega la costa", null);

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().slogan()).isEqualTo("Navega la costa");
        assertThat(shop.brand().shortDescription()).isEqualTo("Small-group sailing since 2011");
    }

    @Test
    void aLocaleWithNoTranslationAtAllGetsTheCanonicalBrand() {
        brand("Sail the coast", "Small-group sailing since 2011");

        StorefrontShopView shop = query.findContent(OPERATOR, "fr").orElseThrow();

        assertThat(shop.brand().slogan()).isEqualTo("Sail the coast");
        assertThat(shop.brand().shortDescription()).isEqualTo("Small-group sailing since 2011");
    }

    /**
     * The palette arrives split by role — a role is this context's enum and may
     * not cross the seam — with the query's order preserved inside each list.
     */
    @Test
    void theColoursComeBackSplitByRoleInTheQuerysOrder() {
        brand("Sail the coast", null);
        when(brandColorRepository.findByTourOperatorIdOrderByPositionAsc(OPERATOR)).thenReturn(List.of(
                color(BrandColorRole.PRIMARY, (short) 0, "#0b3d5c"),
                color(BrandColorRole.SECONDARY, (short) 0, "#f2a541"),
                color(BrandColorRole.PRIMARY, (short) 1, "#1c7ba8")));

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().colors().primary())
                .extracting(StorefrontBrandColorView::background)
                .containsExactly("#0b3d5c", "#1c7ba8");
        assertThat(shop.brand().colors().secondary())
                .extracting(StorefrontBrandColorView::background)
                .containsExactly("#f2a541");
    }

    /**
     * <b>The palette's order lives in the derived query's name and nowhere
     * else</b> — the adapter groups the rows and hands them through, so no
     * assertion over a mocked repository can observe an ORDER BY.
     *
     * <p>So the name is parsed the way Spring Data parses it and the resulting
     * {@code Sort} is compared against the rule written out as a property.
     * Asserting the method name against a string literal would not do: the
     * literal renames along with the method under any mechanical edit, and the
     * test would stay green while the palette reordered.
     */
    @Test
    void thePaletteIsOrderedByPosition() {
        Sort sort = new PartTree(paletteQueryName(), TourOperatorBrandColorJpaEntity.class).getSort();

        assertThat(sort)
                .withFailMessage("The brand palette is ordered by position, because colors.primary[0] is a "
                        + "promise a theme indexes into and an unordered palette reorders between "
                        + "requests. The derived query now sorts: %s", sort)
                .containsExactly(Sort.Order.asc("position"));
    }

    @Test
    void theSocialLinksComeBackAsPlatformNames() {
        brand("Sail the coast", null);
        when(brandSocialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(OPERATOR)).thenReturn(List.of(
                link(BrandSocialPlatform.FACEBOOK, "https://facebook.com/acmetours"),
                link(BrandSocialPlatform.INSTAGRAM, "https://instagram.com/acmetours")));

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().socialLinks())
                .extracting(StorefrontBrandSocialLinkView::platform)
                .containsExactly("FACEBOOK", "INSTAGRAM");
    }

    /**
     * V10 backfilled a brand row per operator, but one created since has none —
     * nothing writes one at creation. The page must render on that, so the brand
     * is an object with nothing in it rather than a null, and the child tables
     * are not even asked.
     */
    @Test
    void anOperatorWithNoBrandRowStillRendersAPage() {
        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand()).isNotNull();
        assertThat(shop.brand().slogan()).isNull();
        assertThat(shop.brand().logo()).isNull();
        assertThat(shop.brand().colors().primary()).isEmpty();
        assertThat(shop.brand().colors().secondary()).isEmpty();
        assertThat(shop.brand().socialLinks()).isEmpty();
        verify(mediaAssetBatchQuery, never()).findAssetsByIds(any(), anySet());
    }

    /**
     * Five media references — four brand images plus the OG image — and one
     * lookup, because this runs on every storefront request. An unset image is
     * absent from the id set rather than a null key, which an immutable
     * {@code Map.of()} would throw on.
     */
    @Test
    void everyImageOnThePageResolvesThroughOneBatchCall() {
        brandWithMedia(LOGO, null, FAVICON, null);
        when(operatorRepository.findById(OPERATOR)).thenReturn(Optional.of(operatorWithOgImage()));
        when(mediaAssetBatchQuery.findAssetsByIds(OPERATOR, Set.of(LOGO, FAVICON, OG_IMAGE))).thenReturn(Map.of(
                LOGO, new MediaAsset("brand/logo.png", "The Acme burgee", 400, 200),
                FAVICON, new MediaAsset("brand/favicon.png", null, null, null),
                OG_IMAGE, new MediaAsset("og.png", null, null, null)));

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().logo())
                .isEqualTo(new MediaAsset("brand/logo.png", "The Acme burgee", 400, 200));
        assertThat(shop.brand().favicon().storageKey()).isEqualTo("brand/favicon.png");
        assertThat(shop.brand().squareLogo()).isNull();
        assertThat(shop.brand().coverImage()).isNull();
        assertThat(shop.ogImageKey()).isEqualTo("og.png");
        verify(mediaAssetBatchQuery).findAssetsByIds(OPERATOR, Set.of(LOGO, FAVICON, OG_IMAGE));
    }

    /**
     * A media id the operator no longer owns is absent from the batch result,
     * which is a brand without that image rather than an error.
     */
    @Test
    void anImageIdThatNoLongerResolvesIsSimplyNoImage() {
        brandWithMedia(LOGO, null, null, null);
        when(mediaAssetBatchQuery.findAssetsByIds(OPERATOR, Set.of(LOGO))).thenReturn(Map.of());

        assertThat(query.findContent(OPERATOR, "es").orElseThrow().brand().logo()).isNull();
    }

    /**
     * The footer's list: the rows that exist, each title overlaid for the locale.
     * The order is the query's — a footer whose links move between requests reads
     * as a bug — and the adapter does not sort again.
     */
    @Test
    void thePolicySummariesOverlayTheirTitlesAndKeepTheQuerysOrder() {
        policies(policy(PolicyType.CANCELLATION, "Cancellation policy", "<p>48 hours.</p>"),
                policy(PolicyType.PRIVACY, "Privacy policy", "<p>What we collect.</p>"));
        when(policyTranslationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es")).thenReturn(List.of(
                policyTranslation(PolicyType.CANCELLATION, "es", "Política de cancelación", null)));

        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.policies())
                .extracting(StorefrontPolicySummaryView::type, StorefrontPolicySummaryView::title)
                .containsExactly(
                        tuple("CANCELLATION", "Política de cancelación"),
                        tuple("PRIVACY", "Privacy policy"));
    }

    /**
     * An operator who has written none is an empty list, not a failure — and the
     * overlay is not even asked for, because there is nothing to overlay.
     */
    @Test
    void anOperatorWithNoPoliciesGetsAnEmptyListRatherThanFailing() {
        StorefrontShopView shop = query.findContent(OPERATOR, "es").orElseThrow();

        assertThat(shop.policies()).isEmpty();
        verify(policyTranslationRepository, never()).findByTourOperatorIdAndLocale(any(), any());
    }

    @Test
    void aPolicysTitleAndBodyBothOverlay() {
        policy(PolicyType.TERMS, "Terms of service", "<p>English terms.</p>", "es",
                "Términos del servicio", "<p>Términos en español.</p>");

        StorefrontPolicyView policy = query.findPolicy(OPERATOR, "TERMS", "es").orElseThrow();

        assertThat(policy.type()).isEqualTo("TERMS");
        assertThat(policy.title()).isEqualTo("Términos del servicio");
        assertThat(policy.body()).isEqualTo("<p>Términos en español.</p>");
    }

    /** A row overlays, it does not replace: a translated title over a canonical body. */
    @Test
    void aColumnThePolicyTranslationLeftNullFallsBackToTheCanonicalValue() {
        policy(PolicyType.TERMS, "Terms of service", "<p>English terms.</p>", "es",
                "Términos del servicio", null);

        StorefrontPolicyView policy = query.findPolicy(OPERATOR, "TERMS", "es").orElseThrow();

        assertThat(policy.title()).isEqualTo("Términos del servicio");
        assertThat(policy.body()).isEqualTo("<p>English terms.</p>");
    }

    @Test
    void aLocaleWithNoPolicyTranslationGetsTheCanonicalDocument() {
        policies(policy(PolicyType.TERMS, "Terms of service", "<p>English terms.</p>"));
        when(policyRepository.findByTourOperatorIdAndType(OPERATOR, PolicyType.TERMS))
                .thenReturn(Optional.of(policy(PolicyType.TERMS, "Terms of service", "<p>English terms.</p>")));

        StorefrontPolicyView policy = query.findPolicy(OPERATOR, "TERMS", "fr").orElseThrow();

        assertThat(policy.title()).isEqualTo("Terms of service");
        assertThat(policy.body()).isEqualTo("<p>English terms.</p>");
    }

    /**
     * <b>The name comes off a URL segment, so an unknown one must not reach
     * {@code valueOf}.</b> {@code /policies/refunds} would otherwise be an
     * {@code IllegalArgumentException} — a 500 in the API's JSON error shape, on
     * a public HTML page — instead of a 404. The type is not even asked of the
     * database.
     */
    @Test
    void aTypeNoPolicyTypeIsNamedAfterIsEmptyRatherThanAnException() {
        assertThat(query.findPolicy(OPERATOR, "REFUNDS", "es")).isEmpty();

        verify(policyRepository, never()).findByTourOperatorIdAndType(any(), any());
    }

    /** A type the operator has not written is the same answer: there is no draft state to report. */
    @Test
    void aTypeTheOperatorHasNotWrittenIsEmpty() {
        assertThat(query.findPolicy(OPERATOR, "LEGAL_NOTICE", "es")).isEmpty();
    }

    private void policies(TourOperatorPolicyJpaEntity... policies) {
        when(policyRepository.findByTourOperatorIdOrderByTypeAsc(OPERATOR)).thenReturn(List.of(policies));
    }

    private void policy(PolicyType type, String title, String body,
                        String locale, String translatedTitle, String translatedBody) {
        when(policyRepository.findByTourOperatorIdAndType(OPERATOR, type))
                .thenReturn(Optional.of(policy(type, title, body)));
        when(policyTranslationRepository.findByTourOperatorIdAndTypeAndLocale(OPERATOR, type, locale))
                .thenReturn(Optional.of(policyTranslation(type, locale, translatedTitle, translatedBody)));
    }

    private static TourOperatorPolicyJpaEntity policy(PolicyType type, String title, String body) {
        return new TourOperatorPolicyJpaEntity(OPERATOR, type, title, body, Instant.EPOCH, Instant.EPOCH);
    }

    private static TourOperatorPolicyTranslationJpaEntity policyTranslation(
            PolicyType type, String locale, String title, String body) {
        return new TourOperatorPolicyTranslationJpaEntity(OPERATOR, type, locale, title, body);
    }

    private static String paletteQueryName() {
        return Arrays.stream(TourOperatorBrandColorJpaRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("findByTourOperatorId"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the brand palette query is gone from the repository"));
    }

    private void brand(String slogan, String shortDescription) {
        when(brandRepository.findById(OPERATOR)).thenReturn(Optional.of(new TourOperatorBrandJpaEntity(
                OPERATOR, slogan, shortDescription, null, null, null, null,
                Instant.EPOCH, Instant.EPOCH)));
    }

    private void brandWithMedia(UUID logo, UUID squareLogo, UUID favicon, UUID coverImage) {
        when(brandRepository.findById(OPERATOR)).thenReturn(Optional.of(new TourOperatorBrandJpaEntity(
                OPERATOR, "Sail the coast", null, logo, squareLogo, favicon, coverImage,
                Instant.EPOCH, Instant.EPOCH)));
    }

    private void translated(String locale, String slogan, String shortDescription) {
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, locale))
                .thenReturn(Optional.of(new TourOperatorTranslationJpaEntity(
                        OPERATOR, locale, null, null, null, slogan, shortDescription)));
    }

    private static TourOperatorBrandColorJpaEntity color(BrandColorRole role, short position, String background) {
        return new TourOperatorBrandColorJpaEntity(OPERATOR, role, position, background, "#ffffff");
    }

    private static TourOperatorBrandSocialLinkJpaEntity link(BrandSocialPlatform platform, String url) {
        return new TourOperatorBrandSocialLinkJpaEntity(OPERATOR, platform, url);
    }

    private static TourOperatorJpaEntity operator() {
        return operator(null);
    }

    private static TourOperatorJpaEntity operatorWithOgImage() {
        return operator(OG_IMAGE);
    }

    private static TourOperatorJpaEntity operator(UUID ogImageMediaId) {
        return new TourOperatorJpaEntity(
                OPERATOR, "Acme Tours", "acme", TIMEZONE, CURRENCY, "Calle Mayor 1",
                null, null, UUID.randomUUID(), Instant.EPOCH, Instant.EPOCH,
                "es", false, null, null, null, null, ogImageMediaId,
                new LinkedHashSet<>(Set.of("es", "en", "fr")));
    }
}
