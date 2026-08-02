package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontOperatorQueryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CURRENCY = UUID.randomUUID();
    private static final UUID TIMEZONE = UUID.randomUUID();

    private TourOperatorRepository tourOperatorRepository;
    private StorefrontOperatorQueryImpl query;

    @BeforeEach
    void setUp() {
        tourOperatorRepository = mock(TourOperatorRepository.class);
        CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
        TimezoneRepository timezoneRepository = mock(TimezoneRepository.class);
        MediaUrlBatchResolver mediaUrlBatchResolver = mock(MediaUrlBatchResolver.class);

        when(currencyRepository.findById(CURRENCY))
                .thenReturn(Optional.of(new Currency(CURRENCY, "DOP", "Dominican peso", "RD$")));
        when(timezoneRepository.findById(TIMEZONE))
                .thenReturn(Optional.of(new Timezone(TIMEZONE, "America/Santo_Domingo", "Santo Domingo", null)));
        when(mediaUrlBatchResolver.resolveOne(any(), any())).thenReturn(null);

        query = new StorefrontOperatorQueryImpl(
                tourOperatorRepository, currencyRepository, timezoneRepository, mediaUrlBatchResolver,
                translationRepository);
    }

    private final TourOperatorTranslationRepository translationRepository =
            mock(TourOperatorTranslationRepository.class);

    private TourOperator operator(LocaleCode primary, Set<LocaleCode> supported,
                                  boolean passwordEnabled, String password) {
        return new TourOperator(OP, new TourOperatorName("Acme Tours"), new Handle("acme"),
                TIMEZONE, CURRENCY, new TourOperatorAddress("Calle Mayor 1"),
                USER, Instant.now(), Instant.now(), null,
                primary, supported, passwordEnabled, password, null, null, null, null);
    }

    private void givenOperator(TourOperator operator) {
        when(tourOperatorRepository.findByHandle("acme")).thenReturn(Optional.of(operator));
    }

    @Test
    void resolves_currency_and_timezone_through_reference() {
        givenOperator(operator(LocaleCode.of("en"), Set.of(LocaleCode.of("en")), false, null));

        StorefrontOperatorView view = query.findByHandle("acme").orElseThrow();

        assertThat(view.currency()).isEqualTo("DOP");
        assertThat(view.timezone()).isEqualTo("America/Santo_Domingo");
        assertThat(view.handle()).isEqualTo("acme");
    }

    @Test
    void lists_the_primary_locale_first_then_the_rest_alphabetically() {
        // Deliberately inserted out of order — the language switcher renders this
        // list as-is, so the ordering must come from the query, not the caller.
        Set<LocaleCode> supported = new LinkedHashSet<>(List.of(
                LocaleCode.of("it"), LocaleCode.of("es"), LocaleCode.of("en"), LocaleCode.of("fr")));
        givenOperator(operator(LocaleCode.of("es"), supported, false, null));

        StorefrontOperatorView view = query.findByHandle("acme").orElseThrow();

        assertThat(view.supportedLocales()).containsExactly("es", "en", "fr", "it");
        assertThat(view.primaryLocale()).isEqualTo("es");
    }

    @Test
    void unknown_handle_resolves_to_empty() {
        when(tourOperatorRepository.findByHandle("nobody")).thenReturn(Optional.empty());

        assertThat(query.findByHandle("nobody")).isEmpty();
    }

    @Test
    void verifies_the_stored_password() {
        givenOperator(operator(LocaleCode.of("en"), Set.of(LocaleCode.of("en")), true, "opensesame"));

        assertThat(query.verifyStorefrontPassword("acme", "opensesame")).isTrue();
        assertThat(query.verifyStorefrontPassword("acme", "OpenSesame")).isFalse();
        assertThat(query.verifyStorefrontPassword("acme", "open")).isFalse();
        assertThat(query.verifyStorefrontPassword("acme", "")).isFalse();
        assertThat(query.verifyStorefrontPassword("acme", null)).isFalse();
    }

    @Test
    void a_disabled_gate_verifies_nothing() {
        // The password survives being disabled (re-enabling keeps it), so the
        // gate state — not the stored value — decides whether anything unlocks.
        givenOperator(operator(LocaleCode.of("en"), Set.of(LocaleCode.of("en")), false, "opensesame"));

        assertThat(query.verifyStorefrontPassword("acme", "opensesame")).isFalse();
    }

    @Test
    void an_unknown_tenant_verifies_nothing() {
        when(tourOperatorRepository.findByHandle("nobody")).thenReturn(Optional.empty());

        assertThat(query.verifyStorefrontPassword("nobody", "opensesame")).isFalse();
    }

    @Test
    void the_view_never_carries_the_password() {
        givenOperator(operator(LocaleCode.of("en"), Set.of(LocaleCode.of("en")), true, "opensesame"));

        // The record's own toString is the leak surface most likely to reach a
        // log line; assert the secret is absent from the whole view.
        assertThat(query.findByHandle("acme").orElseThrow().toString()).doesNotContain("opensesame");
    }
}
