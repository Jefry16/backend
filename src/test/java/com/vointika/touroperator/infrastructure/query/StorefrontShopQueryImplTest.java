package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The mapping this adapter does is the storefront's published contract, and
 * nothing above it can see the parts that matter: the use-case tests stub the
 * port, so the overlay, the role split and the colour order all run only here.
 */
class StorefrontShopQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID CURRENCY = UUID.fromString("cccc0002-0000-0000-0000-000000000002");
    private static final UUID ZONE = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private TourOperatorJpaRepository operatorRepository;
    private TourOperatorTranslationJpaRepository translationRepository;
    private TourOperatorBrandJpaRepository brandRepository;
    private TourOperatorBrandColorJpaRepository colorRepository;
    private TourOperatorBrandSocialLinkJpaRepository socialLinkRepository;
    private TourOperatorPolicyJpaRepository policyRepository;
    private TourOperatorPolicyTranslationJpaRepository policyTranslationRepository;
    private StorefrontShopQueryImpl query;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorJpaRepository.class);
        translationRepository = mock(TourOperatorTranslationJpaRepository.class);
        brandRepository = mock(TourOperatorBrandJpaRepository.class);
        colorRepository = mock(TourOperatorBrandColorJpaRepository.class);
        socialLinkRepository = mock(TourOperatorBrandSocialLinkJpaRepository.class);
        policyRepository = mock(TourOperatorPolicyJpaRepository.class);
        policyTranslationRepository = mock(TourOperatorPolicyTranslationJpaRepository.class);
        CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
        TimezoneRepository timezoneRepository = mock(TimezoneRepository.class);

        Currency euro = mock(Currency.class);
        when(euro.getCode()).thenReturn("EUR");
        when(euro.getSymbol()).thenReturn("€");
        when(currencyRepository.findById(CURRENCY)).thenReturn(Optional.of(euro));

        Timezone madrid = mock(Timezone.class);
        when(madrid.getName()).thenReturn("Europe/Madrid");
        when(madrid.getCityName()).thenReturn("Madrid");
        when(timezoneRepository.findById(ZONE)).thenReturn(Optional.of(madrid));

        when(colorRepository.findByTourOperatorIdOrderByPositionAsc(any())).thenReturn(List.of());
        when(socialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(any())).thenReturn(List.of());
        when(policyRepository.findByTourOperatorIdOrderByTypeAsc(any())).thenReturn(List.of());
        when(policyTranslationRepository.findByTourOperatorIdAndLocale(any(), anyString())).thenReturn(List.of());
        when(translationRepository.findByTourOperatorIdAndLocale(any(), anyString())).thenReturn(Optional.empty());
        when(brandRepository.findById(any())).thenReturn(Optional.empty());

        TourOperatorJpaEntity operator = mock(TourOperatorJpaEntity.class);
        when(operator.getId()).thenReturn(OPERATOR);
        when(operator.getName()).thenReturn("Acme Tours");
        when(operator.getHandle()).thenReturn("acme");
        when(operator.getPrimaryLocale()).thenReturn("es");
        when(operator.getSupportedLocales()).thenReturn(Set.of("es", "en"));
        when(operator.getCurrencyId()).thenReturn(CURRENCY);
        when(operator.getTimezoneId()).thenReturn(ZONE);
        when(operator.getSeoTitle()).thenReturn("Canonical title");
        when(operator.getSeoDescription()).thenReturn("Canonical description");
        when(operatorRepository.findById(OPERATOR)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByHandle("acme")).thenReturn(Optional.of(operator));

        query = new StorefrontShopQueryImpl(operatorRepository, translationRepository, brandRepository,
                colorRepository, socialLinkRepository, policyRepository, policyTranslationRepository,
                currencyRepository, timezoneRepository);
    }

    @Test
    void theLocalesReadAnswersTheTenantQuestion() {
        assertThat(query.findLocales("acme")).hasValueSatisfying(locales -> {
            assertThat(locales.tourOperatorId()).isEqualTo(OPERATOR);
            assertThat(locales.primaryLocale()).isEqualTo("es");
            assertThat(locales.supportedLocales()).containsExactlyInAnyOrder("es", "en");
        });
    }

    @Test
    void aHandleNoOperatorOwnsHasNoLocales() {
        when(operatorRepository.findByHandle("nope")).thenReturn(Optional.empty());

        assertThat(query.findLocales("nope")).isEmpty();
    }

    /**
     * The gate is read before any locale is chosen, so the visitor message is
     * overlaid with the operator's <b>own primary locale</b>. Overlay it with a
     * path locale instead and the gate starts answering differently per language,
     * which is the leak the ordering exists to prevent.
     */
    @Test
    void theGateOverlaysItsMessageWithThePrimaryLocale() {
        TourOperatorTranslationJpaEntity spanish = mock(TourOperatorTranslationJpaEntity.class);
        when(spanish.getPasswordMessage()).thenReturn("Abrimos el lunes");
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(Optional.of(spanish));

        assertThat(query.findGate("acme")).hasValueSatisfying(gate -> {
            assertThat(gate.tourOperatorId()).isEqualTo(OPERATOR);
            assertThat(gate.shopName()).isEqualTo("Acme Tours");
            assertThat(gate.passwordMessage()).isEqualTo("Abrimos el lunes");
        });
    }

    @Test
    void aHandleNoOperatorOwnsHasNoGate() {
        when(operatorRepository.findByHandle("nope")).thenReturn(Optional.empty());

        assertThat(query.findGate("nope")).isEmpty();
    }

    /**
     * Nullable-wins-canonical, and the null half is the one that breaks quietly:
     * a translation row that fills in the title but not the description must not
     * blank the description.
     */
    @Test
    void aTranslationOverlaysFieldItSetsAndFallsBackForTheRest() {
        TourOperatorTranslationJpaEntity translation = mock(TourOperatorTranslationJpaEntity.class);
        when(translation.getSeoTitle()).thenReturn("Título traducido");
        when(translation.getSeoDescription()).thenReturn(null);
        when(translationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(Optional.of(translation));

        ShopView shop = query.findShop(OPERATOR, "es").orElseThrow();

        assertThat(shop.seoTitle()).isEqualTo("Título traducido");
        assertThat(shop.seoDescription()).isEqualTo("Canonical description");
    }

    /** Nothing writes a brand row at creation, so its absence is ordinary — a page still renders. */
    @Test
    void anOperatorWithNoBrandRowGetsAnEmptyBrandNotNull() {
        ShopView shop = query.findShop(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand()).isNotNull();
        assertThat(shop.brand().slogan()).isNull();
        assertThat(shop.brand().primaryColors()).isEmpty();
        assertThat(shop.brand().socialLinks()).isEmpty();
    }

    /**
     * {@code colors.primary[0]} is an address a theme indexes into. The rows come
     * back ordered by position with the roles interleaved, so this asserts the
     * split keeps each role's order — swap the query for an unordered one and the
     * palette silently reshuffles.
     */
    @Test
    void thePaletteIsSplitByRoleAndKeepsItsOrder() {
        // Built before the stubbing below, never inside it: each of these stubs a
        // mock of its own, and Mockito treats a nested when() as an unfinished one.
        List<TourOperatorBrandColorJpaEntity> palette = List.of(
                color(BrandColorRole.PRIMARY, "#111111"),
                color(BrandColorRole.SECONDARY, "#aaaaaa"),
                color(BrandColorRole.PRIMARY, "#222222"),
                color(BrandColorRole.SECONDARY, "#bbbbbb"));
        when(brandRepository.findById(OPERATOR)).thenReturn(Optional.of(mock(TourOperatorBrandJpaEntity.class)));
        when(colorRepository.findByTourOperatorIdOrderByPositionAsc(OPERATOR)).thenReturn(palette);

        ShopView shop = query.findShop(OPERATOR, "es").orElseThrow();

        assertThat(shop.brand().primaryColors()).extracting("background")
                .containsExactly("#111111", "#222222");
        assertThat(shop.brand().secondaryColors()).extracting("background")
                .containsExactly("#aaaaaa", "#bbbbbb");
    }

    /**
     * The platform crosses the boundary as its name, not as the enum: the
     * storefront is fenced from this context and must not be handed a type it
     * cannot see.
     */
    @Test
    void socialLinksCrossAsStrings() {
        when(brandRepository.findById(OPERATOR)).thenReturn(Optional.of(mock(TourOperatorBrandJpaEntity.class)));
        TourOperatorBrandSocialLinkJpaEntity link = mock(TourOperatorBrandSocialLinkJpaEntity.class);
        when(link.getPlatform()).thenReturn(BrandSocialPlatform.INSTAGRAM);
        when(link.getUrl()).thenReturn("https://instagram.com/acme");
        when(socialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(OPERATOR)).thenReturn(List.of(link));

        assertThat(query.findShop(OPERATOR, "es").orElseThrow().brand().socialLinks())
                .singleElement()
                .satisfies(l -> assertThat(l.platform()).isEqualTo("INSTAGRAM"));
    }

    /**
     * A footer wants four links, not four legal documents — so the body is
     * deliberately absent from what every page carries, and the title is
     * translated like everything else.
     */
    @Test
    void policiesCarryTranslatedTitlesAndNoBody() {
        TourOperatorPolicyJpaEntity policy = mock(TourOperatorPolicyJpaEntity.class);
        when(policy.getId()).thenReturn(OPERATOR);
        when(policy.getType()).thenReturn(PolicyType.LEGAL_NOTICE);
        when(policy.getTitle()).thenReturn("Legal notice");
        when(policyRepository.findByTourOperatorIdOrderByTypeAsc(OPERATOR)).thenReturn(List.of(policy));

        TourOperatorPolicyTranslationJpaEntity translated = mock(TourOperatorPolicyTranslationJpaEntity.class);
        when(translated.getType()).thenReturn(PolicyType.LEGAL_NOTICE);
        when(translated.getTitle()).thenReturn("Aviso legal");
        when(policyTranslationRepository.findByTourOperatorIdAndLocale(OPERATOR, "es"))
                .thenReturn(List.of(translated));

        assertThat(query.findShop(OPERATOR, "es").orElseThrow().policies())
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.type()).isEqualTo("LEGAL_NOTICE");
                    assertThat(p.title()).isEqualTo("Aviso legal");
                });
    }

    private static TourOperatorBrandColorJpaEntity color(BrandColorRole role, String background) {
        TourOperatorBrandColorJpaEntity color = mock(TourOperatorBrandColorJpaEntity.class);
        when(color.getRole()).thenReturn(role);
        when(color.getBackground()).thenReturn(background);
        return color;
    }
}
