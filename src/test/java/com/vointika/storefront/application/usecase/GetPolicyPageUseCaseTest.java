package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandColorsView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicySummaryView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicyView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.PolicyPageOutput;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPolicyPageUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private StorefrontShopQuery storefrontShopQuery;
    private GetPolicyPageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new GetPolicyPageUseCase(storefrontShopQuery);
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new StorefrontGateView(
                OPERATOR, false, null, null, "es", Set.of("es", "en", "fr"))));
        when(storefrontShopQuery.findContent(any(), anyString())).thenReturn(Optional.of(shop()));
        when(storefrontShopQuery.findPolicy(any(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void aBarePathRendersThePolicyInThePrimaryLocale() {
        policy("es", "CANCELLATION", "Política de cancelación", "<h2>Cancelación gratuita</h2>");

        PolicyPageOutput page = useCase.execute("acme", null, "cancellation").orElseThrow();

        assertThat(page.envelope().localization().locale()).isEqualTo("es");
        assertThat(page.policy().type()).isEqualTo("CANCELLATION");
        assertThat(page.policy().title()).isEqualTo("Política de cancelación");
        assertThat(page.policy().body()).isEqualTo("<h2>Cancelación gratuita</h2>");
    }

    /**
     * <b>The slug is not the type</b>, and this is the pair that proves it: the
     * URL says {@code legal-notice} and the port is asked for
     * {@code LEGAL_NOTICE}.
     */
    @Test
    void theSlugIsTranslatedIntoTheTypeTheQueryPortSpeaks() {
        policy("es", "LEGAL_NOTICE", "Aviso legal", "<p>Acme Tours S.L.</p>");

        assertThat(useCase.execute("acme", null, "legal-notice")).isPresent();

        verify(storefrontShopQuery).findPolicy(OPERATOR, "LEGAL_NOTICE", "es");
    }

    /**
     * <b>The first page type with a title of its own.</b> Every page before this
     * took the shop's SEO title, which is exactly the conflation {@code page} was
     * split off from {@code shop} to stop — and a policy has no SEO overrides, so
     * its title <em>is</em> the title tag.
     */
    @Test
    void thePagesTitleIsThePolicysRatherThanTheShops() {
        policy("es", "TERMS", "Términos del servicio", "<p>…</p>");

        PolicyPageOutput page = useCase.execute("acme", null, "terms").orElseThrow();

        assertThat(page.envelope().page().title()).isEqualTo("Términos del servicio");
        assertThat(page.envelope().shop().name()).isEqualTo("Acme Tours");
        // Everything else on the envelope is untouched — the shop's description is
        // still the meta description, because a policy body is not one.
        assertThat(page.envelope().page().description()).isEqualTo("Salidas en velero");
        assertThat(page.envelope().page().ogImageKey()).isEqualTo("og.png");
    }

    /** The footer's links come with the envelope on this page too — it is the same chrome. */
    @Test
    void theEnvelopeStillCarriesEveryPolicyLink() {
        policy("es", "CANCELLATION", "Política de cancelación", "<p>…</p>");

        PolicyPageOutput page = useCase.execute("acme", null, "cancellation").orElseThrow();

        assertThat(page.envelope().shop().policies()).hasSize(2);
    }

    @Test
    void aSupportedSecondaryRendersUnderItsPrefix() {
        policy("en", "CANCELLATION", "Cancellation policy", "<p>48 hours.</p>");

        PolicyPageOutput page = useCase.execute("acme", "en", "cancellation").orElseThrow();

        assertThat(page.envelope().localization().locale()).isEqualTo("en");
        assertThat(page.policy().title()).isEqualTo("Cancellation policy");
    }

    /**
     * <b>Three different misses, one answer</b>, and that is the point: telling
     * an anonymous visitor which of them happened tells them what exists.
     */
    @Test
    void aTypeTheOperatorHasNotWrittenIsNoPage() {
        assertThat(useCase.execute("acme", null, "privacy")).isEmpty();
    }

    @Test
    void aSlugNoTypeIsNamedAfterIsNoPage() {
        assertThat(useCase.execute("acme", null, "refunds")).isEmpty();

        verify(storefrontShopQuery).findPolicy(OPERATOR, "REFUNDS", "es");
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null, "cancellation")).isEmpty();
    }

    /** The primary lives unprefixed; a second URL for it would be duplicate content. */
    @Test
    void thePrimaryUnderAPrefixIsNoPage() {
        assertThat(useCase.execute("acme", "es", "cancellation")).isEmpty();

        verify(storefrontShopQuery, never()).findPolicy(any(), anyString(), anyString());
    }

    @Test
    void anUnsupportedLocaleIsNoPage() {
        assertThat(useCase.execute("acme", "de", "cancellation")).isEmpty();

        verify(storefrontShopQuery, never()).findPolicy(any(), anyString(), anyString());
    }

    private void policy(String locale, String type, String title, String body) {
        when(storefrontShopQuery.findPolicy(OPERATOR, type, locale))
                .thenReturn(Optional.of(new StorefrontPolicyView(type, title, body)));
    }

    private static StorefrontShopView shop() {
        return new StorefrontShopView("Acme Tours", "Calle Mayor 1", null, null, "og.png",
                "EUR", "€", "Europe/Madrid", "Madrid", "Acme Tours — excursiones", "Salidas en velero", null,
                new StorefrontBrandView(null, null, null, null, null, null,
                        new StorefrontBrandColorsView(List.of(), List.of()), List.of()),
                List.of(new StorefrontPolicySummaryView("CANCELLATION", "Política de cancelación"),
                        new StorefrontPolicySummaryView("TERMS", "Términos del servicio")));
    }
}
