package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.AudiencePricingInput;
import com.vointika.experience.application.dto.input.CreateSlotInput;
import com.vointika.experience.application.dto.input.CreateSlotsInput;
import com.vointika.experience.application.dto.input.UpdateSlotInput;
import com.vointika.experience.application.service.AudiencePricingResolver;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.OperatorTimezoneQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlotUseCasesTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private static final UUID OP = UUID.randomUUID();
    private static final UUID EXP = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID AUD = UUID.randomUUID();
    private static final UUID SLOT = UUID.randomUUID();

    private ExperienceRepository experienceRepository;
    private SlotRepository slotRepository;
    private SlotAudiencePricingRepository pricingRepository;
    private AudiencePricingResolver pricingResolver;
    private OperatorTimezoneQuery operatorTimezoneQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceRepository.class);
        slotRepository = mock(SlotRepository.class);
        pricingRepository = mock(SlotAudiencePricingRepository.class);
        pricingResolver = mock(AudiencePricingResolver.class);
        operatorTimezoneQuery = mock(OperatorTimezoneQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);

        when(transactionRunner.call(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; })
                .when(transactionRunner).run(any());
        when(operatorTimezoneQuery.findZoneId(OP)).thenReturn(Optional.of(ZoneId.of("UTC")));
        when(idGenerator.newId()).thenReturn(SLOT);
        when(slotRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Experience parent = mock(Experience.class);
        when(parent.getId()).thenReturn(EXP);
        when(parent.getName()).thenReturn(new ExperienceName("Sunset Tour"));
        when(parent.getDescription()).thenReturn(new Description("A guided walk"));
        when(experienceRepository.findByIdAndTourOperatorId(EXP, OP)).thenReturn(Optional.of(parent));
        when(pricingResolver.buildRows(any(), any(), any())).thenReturn(List.of());
    }

    private List<AudiencePricingInput> prices() {
        return List.of(new AudiencePricingInput(AUD, new BigDecimal("30.00"), 10));
    }

    private CreateSlotUseCase createSingle() {
        return new CreateSlotUseCase(experienceRepository, slotRepository, pricingRepository,
                pricingResolver, operatorTimezoneQuery, membershipCheck, transactionRunner, idGenerator, auditTrailPort);
    }

    private CreateSlotsUseCase createRecurring() {
        return new CreateSlotsUseCase(experienceRepository, slotRepository, pricingRepository,
                pricingResolver, operatorTimezoneQuery, membershipCheck, transactionRunner, idGenerator, auditTrailPort);
    }

    // ---- single ----

    @Test
    void singleCreatePersistsSlotAndReturnsId() {
        LocalDateTime start = LocalDate.now().plusDays(2).atTime(10, 0);
        UUID id = createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, start, start.plusHours(3), prices()));

        assertThat(id).isEqualTo(SLOT);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(slotRepository).save(any());
    }

    @Test
    void singleCreateRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        LocalDateTime start = LocalDate.now().plusDays(2).atTime(10, 0);
        assertThatThrownBy(() -> createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, start, start.plusHours(3), prices())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void singleCreateRejectsPastDate() {
        LocalDateTime past = LocalDate.now().minusDays(1).atTime(10, 0);
        assertThatThrownBy(() -> createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, past, past.plusHours(3), prices())))
                .isInstanceOf(InvalidFieldException.class);
    }

    // ---- recurring ----

    @Test
    void recurringGeneratesOneSlotPerMatchingDay() {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(13); // a 14-day window
        CreateSlotsInput input = new CreateSlotsInput(
                USER, OP, EXP, List.of(0, 1, 2, 3, 4, 5, 6), // all weekdays
                LocalTime.of(9, 0), LocalTime.of(11, 0), from, to, prices());

        createRecurring().execute(input);

        verify(slotRepository, times(14)).save(any());
    }

    @Test
    void recurringRollsCrossMidnightEndToNextDay() {
        LocalDate day = LocalDate.now().plusDays(1);
        int dow = day.getDayOfWeek().getValue() % 7;
        CreateSlotsInput input = new CreateSlotsInput(
                USER, OP, EXP, List.of(dow),
                LocalTime.of(22, 0), LocalTime.of(1, 0), day, day, prices());

        createRecurring().execute(input);

        ArgumentCaptor<Slot> captor = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepository).save(captor.capture());
        Slot saved = captor.getValue();
        assertThat(saved.startAt()).isEqualTo(day.atTime(22, 0));
        assertThat(saved.endAt()).isEqualTo(day.plusDays(1).atTime(1, 0));
    }

    @Test
    void recurringRejectsAPatternThatMatchesNothing() {
        // A one-day window on a Monday, asking for the other six weekdays.
        LocalDate day = LocalDate.now().plusDays(1);
        int dow = day.getDayOfWeek().getValue() % 7;
        List<Integer> everyOtherDay = IntStream.range(0, 7).boxed().filter(d -> d != dow).toList();

        assertThatThrownBy(() -> createRecurring().execute(new CreateSlotsInput(
                USER, OP, EXP, everyOtherDay,
                LocalTime.of(9, 0), LocalTime.of(11, 0), day, day, prices())))
                .isInstanceOf(InvalidFieldException.class);

        verify(slotRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void recurringRejectsInvalidDay() {
        LocalDate day = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> createRecurring().execute(new CreateSlotsInput(
                USER, OP, EXP, List.of(7), LocalTime.of(9, 0), LocalTime.of(11, 0), day, day, prices())))
                .isInstanceOf(InvalidFieldException.class);
    }

    // ---- update ----

    private UpdateSlotUseCase update() {
        return new UpdateSlotUseCase(slotRepository, pricingRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    private Slot availableSlot() {
        LocalDateTime start = LocalDate.now().plusDays(2).atTime(10, 0);
        return new Slot(SLOT, EXP, OP, start, start.plusHours(2), "Sunset Tour", "A guided walk");
    }

    @Test
    void updateSetsSoldOut() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP)).thenReturn(Optional.of(availableSlot()));
        when(pricingRepository.findBySlotId(SLOT)).thenReturn(List.of());

        update().execute(OP, SLOT, USER, new UpdateSlotInput("SOLD_OUT", null));

        ArgumentCaptor<Slot> captor = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(SlotStatus.SOLD_OUT);
    }

    @Test
    void updateRejectsAnyEditOfACancelledSlot() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP))
                .thenReturn(Optional.of(availableSlot().cancel()));

        // The capacity-only path never touched changeStatus, so the terminal
        // guard that lived there did not defend it: this used to succeed.
        assertThatThrownBy(() -> update().execute(OP, SLOT, USER, new UpdateSlotInput(
                null, List.of(new UpdateSlotInput.TierCapacity(AUD, 99)))))
                .isInstanceOf(ConflictException.class);

        verify(pricingRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void updateRejectsCapacityBelowBooked() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP)).thenReturn(Optional.of(availableSlot()));
        SlotAudiencePricing row = new SlotAudiencePricing(
                UUID.randomUUID(), SLOT, AUD, "Adults", new BigDecimal("30.00"), 10, 1, 4);
        when(pricingRepository.findBySlotId(SLOT)).thenReturn(List.of(row));

        assertThatThrownBy(() -> update().execute(OP, SLOT, USER, new UpdateSlotInput(
                null, List.of(new UpdateSlotInput.TierCapacity(AUD, 2)))))
                .isInstanceOf(InvalidFieldException.class);
    }
}
