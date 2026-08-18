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
import static org.mockito.Mockito.doCallRealMethod;
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

    /**
     * The operator's timezone, used for the {@code OperatorTimezoneQuery} stub
     * <b>and</b> for every date this class builds — they must be the same zone.
     *
     * <p>{@code CreateSlotUseCase} judges "is this date in the past" against
     * {@code LocalDate.now(zone)} for the <em>operator's</em> zone, which is
     * right: a departure is a wall-clock event where the tour runs. A test that
     * stubs the zone but builds its dates from the machine's default clock is
     * therefore comparing two different calendars, and agrees only while that
     * default zone happens to name the same day as this one.
     *
     * <p>That is not hypothetical: it failed at 00:34 CEST on 2026-08-07, when
     * Madrid had rolled over and UTC had not, so "yesterday" was still today to
     * the use case and nothing was rejected. It passed again at 02:00. A
     * container running UTC — which is what the Docker build is — can never
     * observe it, so this class is only honest if it names its own zone.
     */
    private static final ZoneId OPERATOR_ZONE = ZoneId.of("UTC");

    /** Today <b>in the operator's zone</b> — never the machine's default clock. */
    private static LocalDate today() {
        return LocalDate.now(OPERATOR_ZONE);
    }

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
        // requireByIdAndTourOperatorId is a default method, so Mockito would
        // stub it to null and every 404 assertion below would pass vacuously.
        doCallRealMethod().when(experienceRepository).requireByIdAndTourOperatorId(any(), any());
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
        when(operatorTimezoneQuery.findZoneId(OP)).thenReturn(Optional.of(OPERATOR_ZONE));
        when(idGenerator.newId()).thenReturn(SLOT);
        when(slotRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Experience parent = mock(Experience.class);
        when(parent.getId()).thenReturn(EXP);
        when(parent.getName()).thenReturn(new ExperienceName("Sunset Tour"));
        when(parent.getDescription()).thenReturn(new Description("A guided walk"));
        when(experienceRepository.findByIdAndTourOperatorId(EXP, OP)).thenReturn(Optional.of(parent));
        when(pricingResolver.buildRows(any(), any())).thenReturn(List.of());
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


    @Test
    void singleCreatePersistsSlotAndReturnsId() {
        LocalDateTime start = today().plusDays(2).atTime(10, 0);
        UUID id = createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, start, start.plusHours(3), prices()));

        assertThat(id).isEqualTo(SLOT);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(slotRepository).save(any());
    }

    @Test
    void singleCreateRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        LocalDateTime start = today().plusDays(2).atTime(10, 0);
        assertThatThrownBy(() -> createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, start, start.plusHours(3), prices())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void singleCreateRejectsPastDate() {
        LocalDateTime past = today().minusDays(1).atTime(10, 0);
        assertThatThrownBy(() -> createSingle().execute(new CreateSlotInput(
                USER, OP, EXP, past, past.plusHours(3), prices())))
                .isInstanceOf(InvalidFieldException.class);
    }


    @Test
    void recurringGeneratesOneSlotPerMatchingDay() {
        LocalDate from = today().plusDays(1);
        LocalDate to = from.plusDays(13); // a 14-day window
        CreateSlotsInput input = new CreateSlotsInput(
                USER, OP, EXP, List.of(0, 1, 2, 3, 4, 5, 6), // all weekdays
                LocalTime.of(9, 0), LocalTime.of(11, 0), from, to, prices());

        createRecurring().execute(input);

        verify(slotRepository, times(14)).save(any());
    }

    @Test
    void recurringRollsCrossMidnightEndToNextDay() {
        LocalDate day = today().plusDays(1);
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
        LocalDate day = today().plusDays(1);
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
        LocalDate day = today().plusDays(1);
        assertThatThrownBy(() -> createRecurring().execute(new CreateSlotsInput(
                USER, OP, EXP, List.of(7), LocalTime.of(9, 0), LocalTime.of(11, 0), day, day, prices())))
                .isInstanceOf(InvalidFieldException.class);
    }


    private UpdateSlotUseCase update() {
        return new UpdateSlotUseCase(slotRepository, pricingRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    private Slot availableSlot() {
        LocalDateTime start = today().plusDays(2).atTime(10, 0);
        return new Slot(SLOT, EXP, OP, start, start.plusHours(2), "Sunset Tour", "A guided walk");
    }

    @Test
    void updateRejectsAnyEditOfACancelledSlot() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP))
                .thenReturn(Optional.of(availableSlot().cancel()));

        // Capacity is the only thing this endpoint edits, so this guard is the
        // whole of "a cancelled slot is terminal" — it used to succeed, because
        // the check lived on a status transition the capacity path never took.
        assertThatThrownBy(() -> update().execute(OP, SLOT, USER, new UpdateSlotInput(
                List.of(new UpdateSlotInput.TierCapacity(AUD, 99)))))
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
                List.of(new UpdateSlotInput.TierCapacity(AUD, 2)))))
                .isInstanceOf(InvalidFieldException.class);
    }

    /**
     * <b>An unchanged tier is not rewritten.</b> The editor resends the whole tier
     * list on every save, so without this the common case — touch one tier of five —
     * issues five UPDATEs, four of them writing the value already there.
     *
     * <p>Only the refusal paths were pinned before, with {@code never()}. Nothing
     * asserted the save count on a successful edit, so moving the write back outside
     * its guard left the suite green.
     */
    @Test
    void updateWritesOnlyTheTiersThatActuallyChanged() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP)).thenReturn(Optional.of(availableSlot()));
        UUID other = UUID.randomUUID();
        SlotAudiencePricing unchanged = new SlotAudiencePricing(
                UUID.randomUUID(), SLOT, AUD, "Adults", new BigDecimal("30.00"), 10, 1, 0);
        SlotAudiencePricing changing = new SlotAudiencePricing(
                UUID.randomUUID(), SLOT, other, "Children", new BigDecimal("15.00"), 10, 1, 0);
        when(pricingRepository.findBySlotId(SLOT)).thenReturn(List.of(unchanged, changing));

        update().execute(OP, SLOT, USER, new UpdateSlotInput(List.of(
                new UpdateSlotInput.TierCapacity(AUD, 10),     // resent unchanged
                new UpdateSlotInput.TierCapacity(other, 20)))); // actually edited

        ArgumentCaptor<SlotAudiencePricing> saved = ArgumentCaptor.forClass(SlotAudiencePricing.class);
        verify(pricingRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().audienceId()).isEqualTo(other);
        assertThat(saved.getValue().capacity()).isEqualTo(20);
    }

    /** A PATCH that changes nothing writes nothing and records nothing. */
    @Test
    void updateThatChangesNoTierWritesNothing() {
        when(slotRepository.findByIdAndTourOperatorId(SLOT, OP)).thenReturn(Optional.of(availableSlot()));
        when(pricingRepository.findBySlotId(SLOT)).thenReturn(List.of(new SlotAudiencePricing(
                UUID.randomUUID(), SLOT, AUD, "Adults", new BigDecimal("30.00"), 10, 1, 0)));

        update().execute(OP, SLOT, USER, new UpdateSlotInput(
                List.of(new UpdateSlotInput.TierCapacity(AUD, 10))));

        verify(pricingRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }
}
