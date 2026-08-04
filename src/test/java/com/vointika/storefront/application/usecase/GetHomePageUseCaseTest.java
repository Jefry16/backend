package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
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
                "logo.png", "og.png",
                "EUR", "€", "Europe/Madrid", "Madrid", "Acme Tours — excursiones", "Salidas en velero"));

        StorefrontPageData page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.localization().locale()).isEqualTo("es");
        assertThat(page.shop().name()).isEqualTo("Acme Tours");
        assertThat(page.shop().address()).isEqualTo("Calle Mayor 1");
        assertThat(page.shop().phone()).isEqualTo("+34 910 000 000");
        assertThat(page.shop().email()).isEqualTo("hola@acme.test");
        assertThat(page.shop().logoKey()).isEqualTo("logo.png");
        assertThat(page.shop().currency().code()).isEqualTo("EUR");
        assertThat(page.shop().currency().symbol()).isEqualTo("€");
        assertThat(page.shop().description()).isEqualTo("Salidas en velero");
        assertThat(page.shop().timezone().name()).isEqualTo("Europe/Madrid");
        assertThat(page.shop().timezone().city()).isEqualTo("Madrid");
        // The id comes off the gate row, not the content row: the caller already
        // resolved the tenant, so the content query is not asked for it again.
        assertThat(page.shop().id()).isEqualTo(OPERATOR);
        assertThat(page.page().title()).isEqualTo("Acme Tours — excursiones");
        assertThat(page.page().description()).isEqualTo("Salidas en velero");
        assertThat(page.page().ogImageKey()).isEqualTo("og.png");
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

        assertThat(page.shop().logoKey()).isNull();
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
        return new StorefrontShopView("Acme Tours", "Calle Mayor 1", null, null,
                null, null, "EUR", "€", "Europe/Madrid", "Madrid", seoTitle, null);
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
