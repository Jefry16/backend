package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorBrandJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTourOperatorMembershipsQueryImplTest {

    private TourOperatorMemberJpaRepository memberRepository;
    private TourOperatorJpaRepository operatorRepository;
    private TourOperatorBrandJpaRepository brandRepository;
    private TimezoneRepository timezoneRepository;
    private CurrencyRepository currencyRepository;
    private com.vointika.shared.media.MediaUrlBatchResolver mediaUrlBatchResolver;
    private UserTourOperatorMembershipsQueryImpl query;

    private final UUID userId = UUID.randomUUID();
    private final UUID tzId = UUID.randomUUID();
    private final UUID currencyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberJpaRepository.class);
        operatorRepository = mock(TourOperatorJpaRepository.class);
        brandRepository = mock(TourOperatorBrandJpaRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        currencyRepository = mock(CurrencyRepository.class);
        mediaUrlBatchResolver = mock(com.vointika.shared.media.MediaUrlBatchResolver.class);
        query = new UserTourOperatorMembershipsQueryImpl(
                memberRepository, operatorRepository, brandRepository, timezoneRepository,
                currencyRepository, mediaUrlBatchResolver);
        // An operator with no brand row is the ordinary case for anything created
        // since V10, and it is simply no logo.
        when(brandRepository.findAllById(any())).thenReturn(List.of());
        when(timezoneRepository.findAll()).thenReturn(List.of(
                new Timezone(tzId, "America/Santo_Domingo", "Santo Domingo", null)));
        when(currencyRepository.findAll()).thenReturn(List.of(
                new Currency(currencyId, "DOP", "Dominican Peso", "RD$")));
    }

    private TourOperatorMemberJpaEntity member(UUID operatorId, MemberRole role, boolean isDefault) {
        return new TourOperatorMemberJpaEntity(
                UUID.randomUUID(), operatorId, userId, role, isDefault, Instant.now(),
                "Test User", "test@example.test");
    }

    private TourOperatorJpaEntity operator(UUID id, String name, String handle) {
        return new TourOperatorJpaEntity(
                id, name, handle, tzId, currencyId, "some address", null, null,
                UUID.randomUUID(), Instant.now(), Instant.now(),
                "en", false, null, null,
                null, null, null,
                new java.util.LinkedHashSet<>(java.util.Set.of("en")));
    }

    /** The logo lives on the brand row since V10 — see {@code TourOperatorBrandJpaEntity}. */
    private TourOperatorBrandJpaEntity brandWithLogo(UUID operatorId, UUID logoMediaId) {
        return new TourOperatorBrandJpaEntity(
                operatorId, null, null, logoMediaId, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void returnsEmptyWhenUserHasNoMemberships() {
        when(memberRepository.findByUserId(userId)).thenReturn(List.of());
        assertTrue(query.findForUser(userId).isEmpty());
    }

    @Test
    void mapsFieldsAndPutsTheDefaultOperatorFirst() {
        UUID zeta = UUID.randomUUID(); // default, OWNER
        UUID acme = UUID.randomUUID(); // non-default, ADMIN
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(
                member(acme, MemberRole.ADMIN, false),
                member(zeta, MemberRole.OWNER, true)));
        when(operatorRepository.findByIdIn(any())).thenReturn(List.of(
                operator(zeta, "Zeta Tours", "zeta-tours"),
                operator(acme, "Acme Tours", "acme-tours")));

        List<TourOperatorMembershipView> views = query.findForUser(userId);

        assertEquals(2, views.size());
        // The default operator leads, even though "Acme" sorts first alphabetically.
        TourOperatorMembershipView first = views.get(0);
        assertEquals(zeta, first.id());
        assertEquals("Zeta Tours", first.name());
        assertTrue(first.isDefault());
        assertEquals("OWNER", first.role());
        assertEquals("America/Santo_Domingo", first.timezone());
        assertEquals("DOP", first.currency());
        assertNull(first.logoUrl(), "no brand row → no logo");

        assertEquals("Acme Tours", views.get(1).name());
        assertFalse(views.get(1).isDefault());
        assertEquals("ADMIN", views.get(1).role());
    }

    /**
     * The admin formats money on every experience and slot screen, and the code is
     * what {@code Intl.NumberFormat} needs — it derives the symbol and the
     * per-locale decimal count from it, which a symbol cannot. Resolving it here
     * rather than in the client is what stops prices rendering unformatted and
     * then flipping once the currency reference list lands.
     */
    @Test
    void resolvesTheCurrencyToItsIsoCodeRatherThanItsIdOrSymbol() {
        UUID acme = UUID.randomUUID();
        when(memberRepository.findByUserId(userId))
                .thenReturn(List.of(member(acme, MemberRole.OWNER, true)));
        when(operatorRepository.findByIdIn(any()))
                .thenReturn(List.of(operator(acme, "Acme Tours", "acme-tours")));

        TourOperatorMembershipView view = query.findForUser(userId).get(0);

        assertEquals("DOP", view.currency());
    }

    @Test
    void ordersNonDefaultsAlphabeticallyByName() {
        UUID beta = UUID.randomUUID();
        UUID acme = UUID.randomUUID();
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(
                member(beta, MemberRole.STAFF, false),
                member(acme, MemberRole.STAFF, false)));
        when(operatorRepository.findByIdIn(any())).thenReturn(List.of(
                operator(beta, "Beta Excursions", "beta-excursions"),
                operator(acme, "Acme Tours", "acme-tours")));

        List<String> names = query.findForUser(userId).stream()
                .map(TourOperatorMembershipView::name).toList();

        assertEquals(List.of("Acme Tours", "Beta Excursions"), names);
    }

    @Test
    void resolvesLogoUrlThroughTheMediaSeamWhenSet() {
        UUID op = UUID.randomUUID();
        UUID logoId = UUID.randomUUID();
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(member(op, MemberRole.OWNER, true)));
        when(operatorRepository.findByIdIn(any()))
                .thenReturn(List.of(operator(op, "Logo Co", "logo-co")));
        when(brandRepository.findAllById(any())).thenReturn(List.of(brandWithLogo(op, logoId)));
        when(mediaUrlBatchResolver.resolveOne(op, logoId))
                .thenReturn("https://media.example.com/tour-operators/x/logo.png");

        TourOperatorMembershipView view = query.findForUser(userId).get(0);

        assertEquals("https://media.example.com/tour-operators/x/logo.png", view.logoUrl());
    }
}
