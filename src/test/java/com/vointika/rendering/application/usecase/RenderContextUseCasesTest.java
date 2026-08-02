package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.NavigationAssembler;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontNavigationQuery;
import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenderContextUseCasesTest {

    private static final String SLUG = "acme";
    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private StorefrontOperatorQuery storefrontOperatorQuery;
    private GetShopRenderContextUseCase getShopUseCase;
    private VerifyStorefrontPasswordUseCase verifyPasswordUseCase;

    /** A tenant with no menus — navigation is not what these tests are about. */
    private NavigationAssembler emptyNavigation() {
        StorefrontNavigationQuery navigationQuery = mock(StorefrontNavigationQuery.class);
        when(navigationQuery.findMenus(any(), any())).thenReturn(List.of());
        return new NavigationAssembler(
                navigationQuery, mock(StorefrontExperienceQuery.class), mock(StorefrontPageQuery.class));
    }

    @BeforeEach
    void setUp() {
        storefrontOperatorQuery = mock(StorefrontOperatorQuery.class);
        getShopUseCase = new GetShopRenderContextUseCase(new TenantResolver(storefrontOperatorQuery, emptyNavigation()));
        verifyPasswordUseCase = new VerifyStorefrontPasswordUseCase(storefrontOperatorQuery);
    }

    private StorefrontOperatorView operator(String primary, List<String> supported) {
        return new StorefrontOperatorView(
                OP,
                "Acme Tours",
                SLUG,
                "https://media.example.com/logo.png",
                primary,
                supported,
                "USD",
                "America/Santo_Domingo",
                false,
                null, null, null, null, java.util.Map.of());
    }

    private void givenOperator(StorefrontOperatorView view) {
        when(storefrontOperatorQuery.findBySlug(SLUG)).thenReturn(Optional.of(view));
    }

    // ---------- shop render context ----------

    @Test
    void returns_the_tenant_and_its_primary_locale_for_the_bare_path() {
        givenOperator(operator("en", List.of("en", "es")));

        ShopRenderContext context = getShopUseCase.execute(SLUG, null);

        assertThat(context.shop().name()).isEqualTo("Acme Tours");
        assertThat(context.shop().logoUrl()).isEqualTo("https://media.example.com/logo.png");
        assertThat(context.shop().currency()).isEqualTo("USD");
        assertThat(context.locale()).isEqualTo("en");
    }

    @Test
    void renders_in_a_requested_locale_the_operator_publishes() {
        givenOperator(operator("en", List.of("en", "es")));

        assertThat(getShopUseCase.execute(SLUG, "es").locale()).isEqualTo("es");
    }

    @Test
    void normalizes_a_requested_locale_to_lowercase() {
        givenOperator(operator("en", List.of("en", "es")));

        assertThat(getShopUseCase.execute(SLUG, "ES").locale()).isEqualTo("es");
    }

    @Test
    void normalizes_a_requested_locale_independently_of_the_jvm_default_locale() {
        givenOperator(operator("en", List.of("en", "it")));
        Locale original = Locale.getDefault();
        try {
            // Turkish lowercases "I" to the dotless "ı", so a default-locale
            // toLowerCase() turns "IT" into "ıt" and the Italian page silently
            // renders in English — on some machines and not others.
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(getShopUseCase.execute(SLUG, "IT").locale()).isEqualTo("it");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void trims_a_padded_locale() {
        givenOperator(operator("en", List.of("en", "es")));

        assertThat(getShopUseCase.execute(SLUG, " es ").locale()).isEqualTo("es");
    }

    @Test
    void falls_back_to_primary_for_a_locale_the_operator_does_not_publish() {
        givenOperator(operator("en", List.of("en", "es")));

        // Lenient by design: the BFF 404s an unpublished URL prefix before it
        // gets here, so anything arriving by another route renders, not breaks.
        assertThat(getShopUseCase.execute(SLUG, "fr").locale()).isEqualTo("en");
    }

    @Test
    void falls_back_to_primary_for_a_malformed_locale() {
        givenOperator(operator("en", List.of("en", "es")));

        assertThat(getShopUseCase.execute(SLUG, "not-a-locale").locale()).isEqualTo("en");
    }

    @Test
    void unknown_slug_is_not_found() {
        when(storefrontOperatorQuery.findBySlug("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getShopUseCase.execute("nobody", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void carries_the_gate_state_so_the_bff_always_knows_to_gate() {
        givenOperator(new StorefrontOperatorView(
                OP, "Acme Tours", SLUG, null, "en", List.of("en"), "USD",
                "America/Santo_Domingo", true, "We open on Monday.", null, null, null, java.util.Map.of()));

        ShopRenderContext context = getShopUseCase.execute(SLUG, null);

        assertThat(context.shop().passwordEnabled()).isTrue();
        assertThat(context.shop().passwordMessage()).isEqualTo("We open on Monday.");
    }

    // ---------- password verification ----------

    @Test
    void verifies_a_correct_password() {
        when(storefrontOperatorQuery.verifyStorefrontPassword(SLUG, "opensesame")).thenReturn(true);

        assertThat(verifyPasswordUseCase.execute(SLUG, "opensesame")).isTrue();
    }

    @Test
    void rejects_a_wrong_password_without_throwing() {
        when(storefrontOperatorQuery.verifyStorefrontPassword(SLUG, "guess")).thenReturn(false);

        assertThat(verifyPasswordUseCase.execute(SLUG, "guess")).isFalse();
    }

    @Test
    void an_unknown_tenant_answers_exactly_like_a_wrong_password() {
        when(storefrontOperatorQuery.verifyStorefrontPassword("nobody", "guess")).thenReturn(false);

        assertThat(verifyPasswordUseCase.execute("nobody", "guess")).isFalse();
    }
}
