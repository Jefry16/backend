package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
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
    private TimezoneRepository timezoneRepository;
    private com.vointika.shared.media.MediaUrlBatchResolver mediaUrlBatchResolver;
    private UserTourOperatorMembershipsQueryImpl query;

    private final UUID userId = UUID.randomUUID();
    private final UUID tzId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberJpaRepository.class);
        operatorRepository = mock(TourOperatorJpaRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        mediaUrlBatchResolver = mock(com.vointika.shared.media.MediaUrlBatchResolver.class);
        query = new UserTourOperatorMembershipsQueryImpl(
                memberRepository, operatorRepository, timezoneRepository, mediaUrlBatchResolver);
        when(timezoneRepository.findAll()).thenReturn(List.of(
                new Timezone(tzId, "America/Santo_Domingo", "Santo Domingo", null)));
    }

    private TourOperatorMemberJpaEntity member(UUID operatorId, MemberRole role, boolean isDefault) {
        return new TourOperatorMemberJpaEntity(
                UUID.randomUUID(), operatorId, userId, role, isDefault, Instant.now(),
                "Test User", "test@example.test");
    }

    private TourOperatorJpaEntity operator(UUID id, String name, String slug) {
        return operator(id, name, slug, null);
    }

    private TourOperatorJpaEntity operator(UUID id, String name, String slug, UUID logoMediaId) {
        return new TourOperatorJpaEntity(
                id, name, slug, tzId, UUID.randomUUID(), "some address", logoMediaId,
                UUID.randomUUID(), Instant.now(), Instant.now(),
                "en", false, null, null,
                new java.util.LinkedHashSet<>(java.util.Set.of("en")));
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
        assertNull(first.logoUrl(), "no logo set → resolver returns null");

        assertEquals("Acme Tours", views.get(1).name());
        assertFalse(views.get(1).isDefault());
        assertEquals("ADMIN", views.get(1).role());
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
                .thenReturn(List.of(operator(op, "Logo Co", "logo-co", logoId)));
        when(mediaUrlBatchResolver.resolveOne(op, logoId))
                .thenReturn("https://media.example.com/tour-operators/x/logo.png");

        TourOperatorMembershipView view = query.findForUser(userId).get(0);

        assertEquals("https://media.example.com/tour-operators/x/logo.png", view.logoUrl());
    }
}
