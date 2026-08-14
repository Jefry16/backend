package com.vointika.pickup.application.usecase;

import com.vointika.pickup.application.dto.input.PickupLocationInput;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickupLocationUseCasesTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PICKUP = UUID.fromString("dddddddd-0000-4000-8000-000000000001");

    private PickupLocationRepository repository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        repository = mock(PickupLocationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        when(transactionRunner.call(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(PICKUP);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private PickupLocation pickup(String name, LocalTime time) {
        return new PickupLocation(PICKUP, OP,
                new PickupLocationName(name), new PickupLocationTime(time), USER, Instant.now());
    }

    private CreatePickupLocationUseCase create() {
        return new CreatePickupLocationUseCase(repository, membershipCheck,
                transactionRunner, idGenerator, auditTrailPort);
    }

    private UpdatePickupLocationUseCase update() {
        return new UpdatePickupLocationUseCase(repository, membershipCheck, transactionRunner, auditTrailPort);
    }

    private DeletePickupLocationUseCase delete() {
        return new DeletePickupLocationUseCase(repository, membershipCheck, transactionRunner, auditTrailPort);
    }


    @Test
    void createPersists() {
        when(repository.existsByTourOperatorIdAndName(OP, "Old Port")).thenReturn(false);

        UUID id = create().execute(OP, USER, new PickupLocationInput("Old Port", "09:30"));

        assertThat(id).isEqualTo(PICKUP);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
    }

    @Test
    void createRejectsDuplicateNameUpFront() {
        when(repository.existsByTourOperatorIdAndName(OP, "Old Port")).thenReturn(true);

        assertThatThrownBy(() -> create().execute(OP, USER, new PickupLocationInput("Old Port", "09:30")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);

        assertThatThrownBy(() -> create().execute(OP, USER, new PickupLocationInput("Old Port", "09:30")))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).save(any());
    }


    @Test
    void updateRenames() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP))
                .thenReturn(Optional.of(pickup("Old Port", LocalTime.of(9, 30))));
        when(repository.existsByTourOperatorIdAndNameExcluding(eq(OP), anyString(), eq(PICKUP)))
                .thenReturn(false);

        update().execute(OP, PICKUP, USER, new PickupLocationInput("Marina", null));

        verify(repository).save(any());
    }

    @Test
    void updateTimeOnlyIsPartial() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP))
                .thenReturn(Optional.of(pickup("Old Port", LocalTime.of(9, 30))));

        update().execute(OP, PICKUP, USER, new PickupLocationInput(null, "10:15"));

        verify(repository).save(any());
    }

    @Test
    void updateNoChangesIsNoOp() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP))
                .thenReturn(Optional.of(pickup("Old Port", LocalTime.of(9, 30))));

        update().execute(OP, PICKUP, USER, new PickupLocationInput("Old Port", "09:30"));

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsDuplicateName() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP))
                .thenReturn(Optional.of(pickup("Old Port", LocalTime.of(9, 30))));
        when(repository.existsByTourOperatorIdAndNameExcluding(OP, "Marina", PICKUP)).thenReturn(true);

        assertThatThrownBy(() -> update().execute(OP, PICKUP, USER, new PickupLocationInput("Marina", null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateMissingIs404() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> update().execute(OP, PICKUP, USER, new PickupLocationInput("Marina", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void deleteRemovesCatalogRow() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP))
                .thenReturn(Optional.of(pickup("Old Port", LocalTime.of(9, 30))));

        delete().execute(OP, PICKUP, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).deleteById(PICKUP);
    }

    @Test
    void deleteMissingIs404() {
        when(repository.findByIdAndTourOperatorId(PICKUP, OP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delete().execute(OP, PICKUP, USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
