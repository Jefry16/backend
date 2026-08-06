package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandColorView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandColorsView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandSocialLinkView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicySummaryView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.BrandData.ColorData;
import com.vointika.storefront.application.dto.output.BrandData.SocialLinkData;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
import com.vointika.storefront.application.dto.output.PolicyData;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetHomePageUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private StorefrontShopQuery storefrontShopQuery;
    private GetHomePageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new GetHomePageUseCase(storefrontShopQuery);
        gate(false, null);
    }

    @Test
    void aBarePathRendersThePrimaryLocale() {
        content("es", new StorefrontShopView("Acme Tours", "Calle Mayor 1", "+34 910 000 000", "hola@acme.test",
                "og.png",
                "EUR", "€", "Europe/Madrid", "Madrid", "Acme Tours — excursiones", "Salidas en velero",
                "Ask us for the password", brand(), List.of()));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.localization().locale()).isEqualTo("es");
        assertThat(page.shop().name()).isEqualTo("Acme Tours");
        assertThat(page.shop().address()).isEqualTo("Calle Mayor 1");
        assertThat(page.shop().phone()).isEqualTo("+34 910 000 000");
        assertThat(page.shop().email()).isEqualTo("hola@acme.test");
        assertThat(page.shop().currency().code()).isEqualTo("EUR");
        assertThat(page.shop().currency().symbol()).isEqualTo("€");
        assertThat(page.shop().description()).isEqualTo("Salidas en velero");
        assertThat(page.shop().timezone().name()).isEqualTo("Europe/Madrid");
        assertThat(page.shop().timezone().city()).isEqualTo("Madrid");
        // Overlaid for the locale being rendered, unlike the gate's copy, which is
        // primary-only because the gate answers before a locale is resolved.
        assertThat(page.shop().passwordMessage()).isEqualTo("Ask us for the password");
        // The id comes off the gate row, not the content row: the caller already
        // resolved the tenant, so the content query is not asked for it again.
        assertThat(page.shop().id()).isEqualTo(OPERATOR);
        assertThat(page.page().title()).isEqualTo("Acme Tours — excursiones");
        assertThat(page.page().description()).isEqualTo("Salidas en velero");
        assertThat(page.page().ogImageKey()).isEqualTo("og.png");
    }

    /**
     * {@code shop.brand} is where the logo lives — Shopify's shop object has none
     * and neither does ours. The palette arrives already split by role, ordered
     * within each, because the role is the owning context's enum and cannot cross
     * the seam.
     */
    @Test
    void theBrandCarriesTheLogoThePaletteAndTheLinks() {
        content("es", new StorefrontShopView("Acme Tours", "Calle Mayor 1", null, null, null,
                "EUR", "€", "Europe/Madrid", "Madrid", null, null, null, brand(), List.of()));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.shop().brand().slogan()).isEqualTo("Navega la costa");
        assertThat(page.shop().brand().shortDescription()).isEqualTo("Salidas en grupos pequeños");
        assertThat(page.shop().brand().logo().storageKey()).isEqualTo("brand/logo.png");
        assertThat(page.shop().brand().logo().alt()).isEqualTo("The Acme burgee");
        assertThat(page.shop().brand().logo().width()).isEqualTo(400);
        assertThat(page.shop().brand().logo().height()).isEqualTo(200);
        assertThat(page.shop().brand().squareLogo()).isNull();
        assertThat(page.shop().brand().colors().primary())
                .extracting(ColorData::background)
                .containsExactly("#0b3d5c", "#1c7ba8");
        assertThat(page.shop().brand().colors().secondary())
                .extracting(ColorData::background)
                .containsExactly("#f2a541");
        assertThat(page.shop().brand().socialLinks())
                .extracting(SocialLinkData::platform)
                .containsExactly("INSTAGRAM");
    }

    /**
     * V10 backfilled a brand row per operator, but one created since has none —
     * and the brand still has no write path. A page must render on that, so the
     * envelope's brand is an object with nothing in it rather than a null.
     */
    @Test
    void anOperatorWithNoBrandRowStillGetsABrandObject() {
        content("es", shop(null));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.shop().brand()).isNotNull();
        assertThat(page.shop().brand().slogan()).isNull();
        assertThat(page.shop().brand().logo()).isNull();
        assertThat(page.shop().brand().colors().primary()).isEmpty();
        assertThat(page.shop().brand().colors().secondary()).isEmpty();
        assertThat(page.shop().brand().socialLinks()).isEmpty();
    }

    /**
     * <b>Where a policy lives is derived from its type, not stored.</b> The type
     * <em>is</em> the address, so the slug is the one transform between the two
     * vocabularies — and it is application's answer to "where", leaving
     * presentation to say what the URL is (PATTERNS §2a).
     */
    @Test
    void everyPolicyCarriesTheSlugItIsAddressedBy() {
        content("es", shop(null, List.of(
                new StorefrontPolicySummaryView("CANCELLATION", "Política de cancelación"),
                new StorefrontPolicySummaryView("LEGAL_NOTICE", "Aviso legal"))));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.shop().policies())
                .extracting(PolicyData::type, PolicyData::title, PolicyData::slug)
                .containsExactly(
                        tuple("CANCELLATION", "Política de cancelación", "cancellation"),
                        tuple("LEGAL_NOTICE", "Aviso legal", "legal-notice"));
    }

    /** An operator who has written none is a footer with no links, not a broken page. */
    @Test
    void anOperatorWithNoPoliciesGetsAnEmptyList() {
        content("es", shop(null));

        assertThat(useCase.execute("acme", null).orElseThrow().shop().policies()).isEmpty();
    }

    @Test
    void aSupportedSecondaryRendersItsOwnContent() {
        content("en", shop("Acme Tours — day trips"));

        StorefrontPageData page = useCase.execute("acme", "en").orElseThrow();

        assertThat(page.localization().locale()).isEqualTo("en");
        assertThat(page.page().title()).isEqualTo("Acme Tours — day trips");
    }

    /** The primary lives at {@code /}; a second URL for it would be duplicate content. */
    @Test
    void thePrimaryUnderAPrefixIsNoPage() {
        assertThat(useCase.execute("acme", "es")).isEmpty();
        verify(storefrontShopQuery, never()).findContent(any(), anyString());
    }

    @Test
    void anUnsupportedLocaleIsNoPage() {
        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(storefrontShopQuery, never()).findContent(any(), anyString());
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEmpty();
    }

    /**
     * The title is the <em>page's</em>, and it falls back through the shop's SEO
     * title to the shop's name. Splitting {@code page} from {@code shop} is what
     * keeps that legible: {@code shop.name} is the operator, {@code page.title} is
     * what goes in the tab.
     */
    @Test
    void theTitleFallsBackToTheShopNameWhenNoSeoTitleIsSet() {
        content("es", shop(null));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.page().title()).isEqualTo("Acme Tours");
        assertThat(page.shop().name()).isEqualTo("Acme Tours");
    }

    /** Absent media stay absent — nothing invents an empty string for the resolver. */
    @Test
    void nullMediaKeysPassThroughAsNull() {
        content("es", shop(null));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.shop().brand().logo()).isNull();
        assertThat(page.page().ogImageKey()).isNull();
        assertThat(page.page().description()).isNull();
    }

    /**
     * V9's columns are nullable and no write path fills them, so this is the
     * state every operator is in. Null, never {@code ""} — the footer guards them
     * with a section, and Mustache treats the empty string as truthy.
     */
    @Test
    void absentContactDetailsStayNull() {
        content("es", shop(null));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.shop().phone()).isNull();
        assertThat(page.shop().email()).isNull();
        assertThat(page.shop().address()).isEqualTo("Calle Mayor 1");
    }

    /**
     * <b>The switcher's contract.</b> Every locale the operator publishes appears
     * once, the primary leads (a {@code Set} has no order and a switcher that
     * reorders between requests reads as a bug), the primary is the one with no
     * path prefix, and exactly one entry is {@code current} — which is what lets a
     * theme mark the active language without comparing strings.
     */
    @Test
    void everyPublishedLocaleIsListedWithThePrimaryBareAndSecondariesPrefixed() {
        content("es", shop(null));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.localization().languages())
                .extracting(LanguageData::code, LanguageData::current, LanguageData::pathLocale)
                .containsExactly(
                        tuple("es", true, null),
                        tuple("en", false, "en"),
                        tuple("fr", false, "fr"));
    }

    /** {@code current} follows the rendered locale, not the primary. */
    @Test
    void theCurrentLanguageIsTheOneBeingRendered() {
        content("en", shop(null));

        StorefrontPageData page = useCase.execute("acme", "en").orElseThrow();

        assertThat(page.localization().languages())
                .filteredOn(LanguageData::current)
                .extracting(LanguageData::code)
                .containsExactly("en");
    }

    /**
     * <b>The gate's plaintext password must not reach a view model.</b> It exists
     * to be compared inside the gate; anything that carries it onto a page is a
     * plaintext password served to the public. This walks the whole envelope
     * reflectively rather than naming fields, so a component added anywhere below
     * it is covered on the day it is added.
     */
    @Test
    void theGatesPasswordNeverReachesTheEnvelope() {
        gate(true, "open-sesame");
        content("es", shop("Acme Tours"));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(everyStringIn(page)).isNotEmpty().doesNotContain("open-sesame");
    }

    private void gate(boolean passwordEnabled, String password) {
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new StorefrontGateView(
                OPERATOR, passwordEnabled, password, null, "es", Set.of("es", "en", "fr"))));
    }

    private void content(String locale, StorefrontShopView shop) {
        when(storefrontShopQuery.findContent(OPERATOR, locale)).thenReturn(Optional.of(shop));
    }

    private static StorefrontShopView shop(String seoTitle) {
        return shop(seoTitle, List.of());
    }

    private static StorefrontShopView shop(String seoTitle, List<StorefrontPolicySummaryView> policies) {
        return new StorefrontShopView("Acme Tours", "Calle Mayor 1", null, null,
                null, "EUR", "€", "Europe/Madrid", "Madrid", seoTitle, null, null, noBrand(), policies);
    }

    /** The brand a real operator has: a translated slogan, an ordered palette, one link, one image. */
    private static StorefrontBrandView brand() {
        return new StorefrontBrandView(
                "Navega la costa", "Salidas en grupos pequeños",
                new MediaAsset("brand/logo.png", "The Acme burgee", 400, 200),
                null, null, null,
                new StorefrontBrandColorsView(
                        List.of(new StorefrontBrandColorView("#0b3d5c", "#ffffff"),
                                new StorefrontBrandColorView("#1c7ba8", "#ffffff")),
                        List.of(new StorefrontBrandColorView("#f2a541", "#1a1a1a"))),
                List.of(new StorefrontBrandSocialLinkView("INSTAGRAM", "https://instagram.com/acmetours")));
    }

    /** What an operator with no brand row gets — the port never answers null. */
    private static StorefrontBrandView noBrand() {
        return new StorefrontBrandView(null, null, null, null, null, null,
                new StorefrontBrandColorsView(List.of(), List.of()), List.of());
    }


    private static List<String> everyStringIn(Object value) {
        return switch (value) {
            case null -> List.of();
            case String string -> List.of(string);
            case Collection<?> collection -> collection.stream().flatMap(e -> everyStringIn(e).stream()).toList();
            case Record record -> Arrays.stream(record.getClass().getRecordComponents())
                    .flatMap(component -> everyStringIn(read(component, record)).stream())
                    .toList();
            default -> List.of();
        };
    }

    private static Object read(RecordComponent component, Object owner) {
        try {
            return component.getAccessor().invoke(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
