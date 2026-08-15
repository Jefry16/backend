package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.DiagnosticLogPort;
import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.event.TourOperatorWelcomeEmailRequestedEvent;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.dto.input.AddressInput;
import com.vointika.touroperator.application.dto.input.CreateTourOperatorInput;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.shared.service.HandleGenerator;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateTourOperatorUseCaseTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorRepository tourOperatorRepository;
    private TourOperatorMemberRepository memberRepository;
    private MenuRepository menuRepository;
    private TimezoneRepository timezoneRepository;
    private CurrencyRepository currencyRepository;
    private CountryRepository countryRepository;
    private IdGenerator idGenerator;
    private UserAccountQuery userAccountQuery;
    private EventPublisherPort eventPublisher;
    private DiagnosticLogPort diagnosticLog;
    private CreateTourOperatorUseCase useCase;

    /** A fixed value so the assertions can name it; the real one is 12 random characters. */
    private static final String GENERATED_PASSWORD = "Kx7mQp2rTvWy";

    private final UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final UUID timezoneId = UUID.randomUUID();
    private final UUID currencyId = UUID.randomUUID();
    private final UUID countryId = UUID.randomUUID();
    private final AddressInput ADDRESS =
            new AddressInput("123 Beach Rd", null, "Punta Cana", null, "23000", countryId);

    @BeforeEach
    void setUp() {
        tourOperatorRepository = mock(TourOperatorRepository.class);
        memberRepository = mock(TourOperatorMemberRepository.class);
        menuRepository = mock(MenuRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        currencyRepository = mock(CurrencyRepository.class);
        countryRepository = mock(CountryRepository.class);
        idGenerator = mock(IdGenerator.class);
        userAccountQuery = mock(UserAccountQuery.class);
        eventPublisher = mock(EventPublisherPort.class);
        diagnosticLog = mock(DiagnosticLogPort.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new CreateTourOperatorUseCase(
                tourOperatorRepository, memberRepository, menuRepository,
                timezoneRepository, currencyRepository, countryRepository,
                new HandleGenerator(), transactionRunner, idGenerator,
                userAccountQuery, eventPublisher, auditTrailPort, diagnosticLog,
                () -> GENERATED_PASSWORD);

        // Happy-path defaults; individual tests override.
        when(timezoneRepository.findById(any())).thenReturn(Optional.of(mock(Timezone.class)));
        when(currencyRepository.findById(any())).thenReturn(Optional.of(mock(Currency.class)));
        when(countryRepository.findById(any())).thenReturn(Optional.of(mock(Country.class)));
        when(idGenerator.newId()).thenAnswer(inv -> UUID.randomUUID());
        when(tourOperatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(menuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountQuery.findContact(any()))
                .thenReturn(Optional.of(new UserContactView("owner@example.com", "Ada Owner", "es")));
    }

    private CreateTourOperatorInput input() {
        return new CreateTourOperatorInput(userId, "Acme Tours", ADDRESS, timezoneId, currencyId);
    }

    /**
     * <b>A brand-new store is private</b>, the way a new Shopify store is: it
     * exists at its address and answers the gate rather than the storefront. Without
     * this, the first request after an operator is created serves its (empty)
     * storefront to anyone who guesses the handle.
     *
     * <p>The password is generated rather than left for the operator to set,
     * because enabling protection with none stored is a 422 — the gate would be
     * on with no way through it.
     */
    @Test
    void aNewOperatorStartsBehindTheGateWithAGeneratedPassword() {
        useCase.execute(input());

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        TourOperator saved = opCaptor.getValue();
        assertTrue(saved.isPasswordEnabled());
        assertEquals(GENERATED_PASSWORD, saved.getStorefrontPassword());
    }

    /**
     * A storefront lives at {@code {handle}.vointika.com}, so an operator called
     * "API" would otherwise mint the handle {@code api} and claim the API host.
     *
     * <p>It is not refused — the name is the operator's and only the *handle*
     * has to be free — so a reserved label counts as taken and the generator
     * suffixes past it exactly as it does any collision.
     */
    @Test
    void anOperatorNamedAfterInfrastructureDoesNotClaimThatHost() {
        CreateTourOperatorInput reserved = new CreateTourOperatorInput(
                userId, "API", ADDRESS, timezoneId, currencyId);

        useCase.execute(reserved);

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        TourOperator saved = opCaptor.getValue();
        assertEquals("API", saved.getName().value());
        assertEquals("api-2", saved.getHandle().value());
    }

    @Test
    void createsOperatorAndOwnerMemberInOneTransaction() {
        CreateTourOperatorOutput out = useCase.execute(input());

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        TourOperator saved = opCaptor.getValue();
        assertEquals("Acme Tours", saved.getName().value());
        assertEquals("acme-tours", saved.getHandle().value());
        assertEquals(userId, saved.getCreatedBy());
        assertEquals(saved.getId(), out.id());

        ArgumentCaptor<TourOperatorMember> memberCaptor = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        TourOperatorMember member = memberCaptor.getValue();
        assertEquals(MemberRole.OWNER, member.getRole());
        assertEquals(userId, member.getUserId());
        assertEquals(saved.getId(), member.getTourOperatorId());
        assertTrue(member.isDefault(), "the user's first operator is their default");
    }

    @Test
    void seedsTheTwoDefaultMenusInTheCreateTransaction() {
        CreateTourOperatorOutput out = useCase.execute(input());

        ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository, times(2)).save(menuCaptor.capture());
        List<Menu> menus = menuCaptor.getAllValues();
        assertEquals("main-menu", menus.get(0).getHandle().value());
        assertEquals("Main menu", menus.get(0).getTitle());
        assertEquals("footer", menus.get(1).getHandle().value());
        assertEquals("Footer", menus.get(1).getTitle());
        assertEquals(out.id(), menus.get(0).getTourOperatorId());
        assertEquals(userId, menus.get(0).getCreatedBy());
    }

    @Test
    void rejectedCreateSeedsNoMenus() {
        when(tourOperatorRepository.existsByOwnerAndName(eq(userId), any())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> useCase.execute(input()));
        verify(menuRepository, never()).save(any());
    }

    @Test
    void publishesWelcomeEmailToTheCreatorAfterCreate() {
        useCase.execute(input());

        ArgumentCaptor<TourOperatorWelcomeEmailRequestedEvent> captor =
                ArgumentCaptor.forClass(TourOperatorWelcomeEmailRequestedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        TourOperatorWelcomeEmailRequestedEvent event = captor.getValue();
        assertEquals("owner@example.com", event.email());
        assertEquals("Ada Owner", event.name());
        assertEquals("Acme Tours", event.operatorName());
        assertEquals("es", event.locale(), "welcome email uses the creator's UI language");
    }

    @Test
    void doesNotFailCreateWhenWelcomeEmailCannotBeEnqueued() {
        // Publishing the welcome email is fire-and-forget: a broker blip must not
        // fail an operator that committed. (The contact LOOKUP is now required —
        // it populates the owner member's name/email — so it stays on the happy
        // path, stubbed in @BeforeEach.)
        doThrow(new RuntimeException("broker down")).when(eventPublisher).publish(any());

        CreateTourOperatorOutput out = useCase.execute(input());

        assertNotNull(out.id(), "create still succeeds when the welcome email fails to publish");
        verify(tourOperatorRepository).save(any());
    }

    @Test
    void failsWhenCreatorContactCannotBeResolved() {
        // The owner member's name/email are NOT NULL, so the creator's contact
        // MUST resolve. An unresolvable creator (never happens for a real
        // authenticated caller) fails closed rather than persisting a bad row.
        when(userAccountQuery.findContact(any())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(input()));
    }

    @Test
    void secondOperatorForSameUserIsNotDefault() {
        when(memberRepository.existsByUserId(userId)).thenReturn(true);

        useCase.execute(input());

        ArgumentCaptor<TourOperatorMember> memberCaptor = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertFalse(memberCaptor.getValue().isDefault());
    }

    @Test
    void duplicateNameForOwnerThrowsConflictAndSavesNothing() {
        when(tourOperatorRepository.existsByOwnerAndName(eq(userId), any())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> useCase.execute(input()));
        verify(tourOperatorRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void missingTimezoneThrowsInvalidField() {
        when(timezoneRepository.findById(timezoneId)).thenReturn(Optional.empty());
        assertThrows(InvalidFieldException.class, () -> useCase.execute(input()));
    }

    @Test
    void missingCurrencyThrowsInvalidField() {
        when(currencyRepository.findById(currencyId)).thenReturn(Optional.empty());
        assertThrows(InvalidFieldException.class, () -> useCase.execute(input()));
    }

    @Test
    void aMissingAuthenticatedUserThrowsUnauthorized() {
        CreateTourOperatorInput bad = new CreateTourOperatorInput(
                null, "Acme Tours", ADDRESS, timezoneId, currencyId);
        assertThrows(UnauthorizedException.class, () -> useCase.execute(bad));
    }

    @Test
    void handleCollisionAppendsANumericSuffix() {
        when(tourOperatorRepository.existsByHandle("acme-tours")).thenReturn(true);
        when(tourOperatorRepository.existsByHandle("acme-tours-2")).thenReturn(false);

        useCase.execute(input());

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        assertEquals("acme-tours-2", opCaptor.getValue().getHandle().value());
    }

    /**
     * The country is a reference row, so its existence is the use case's to check
     * — a value object cannot query. Same seam and same 422 as the timezone and
     * currency beside it.
     */
    @Test
    void unknownCountryThrowsInvalidField() {
        when(countryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(InvalidFieldException.class, () -> useCase.execute(input()));
        verify(tourOperatorRepository, never()).save(any());
    }

    /** An address is still required at creation; it is the read-back that was missing. */
    @Test
    void missingAddressThrowsInvalidField() {
        CreateTourOperatorInput noAddress = new CreateTourOperatorInput(
                userId, "Acme Tours", null, timezoneId, currencyId);

        assertThrows(InvalidFieldException.class, () -> useCase.execute(noAddress));
    }
}
