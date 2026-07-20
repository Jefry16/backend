package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.dto.input.CreateTourOperatorInput;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.touroperator.application.service.SlugGenerator;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateTourOperatorUseCaseTest {

    private TourOperatorRepository tourOperatorRepository;
    private TourOperatorMemberRepository memberRepository;
    private TimezoneRepository timezoneRepository;
    private CurrencyRepository currencyRepository;
    private IdGenerator idGenerator;
    private CreateTourOperatorUseCase useCase;

    private final UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final UUID timezoneId = UUID.randomUUID();
    private final UUID currencyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tourOperatorRepository = mock(TourOperatorRepository.class);
        memberRepository = mock(TourOperatorMemberRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        currencyRepository = mock(CurrencyRepository.class);
        idGenerator = mock(IdGenerator.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new CreateTourOperatorUseCase(
                tourOperatorRepository, memberRepository,
                timezoneRepository, currencyRepository,
                new SlugGenerator(), transactionRunner, idGenerator);

        // Happy-path defaults; individual tests override.
        when(timezoneRepository.findById(any())).thenReturn(Optional.of(mock(Timezone.class)));
        when(currencyRepository.findById(any())).thenReturn(Optional.of(mock(Currency.class)));
        when(idGenerator.newId()).thenReturn(UUID.randomUUID(), UUID.randomUUID());
        when(tourOperatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateTourOperatorInput input() {
        return new CreateTourOperatorInput(userId.toString(), "Acme Tours", "123 Beach Rd", timezoneId, currencyId);
    }

    @Test
    void createsOperatorAndOwnerMemberInOneTransaction() {
        CreateTourOperatorOutput out = useCase.execute(input());

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        TourOperator saved = opCaptor.getValue();
        assertEquals("Acme Tours", saved.getName().value());
        assertEquals("acme-tours", saved.getSlug().value());
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
    void invalidAuthenticatedUserThrowsUnauthorized() {
        CreateTourOperatorInput bad = new CreateTourOperatorInput(
                "not-a-uuid", "Acme Tours", "123 Beach Rd", timezoneId, currencyId);
        assertThrows(UnauthorizedException.class, () -> useCase.execute(bad));
    }

    @Test
    void slugCollisionAppendsANumericSuffix() {
        when(tourOperatorRepository.existsBySlug("acme-tours")).thenReturn(true);
        when(tourOperatorRepository.existsBySlug("acme-tours-2")).thenReturn(false);

        useCase.execute(input());

        ArgumentCaptor<TourOperator> opCaptor = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(opCaptor.capture());
        assertEquals("acme-tours-2", opCaptor.getValue().getSlug().value());
    }
}
