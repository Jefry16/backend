package com.vointika.pickup.infrastructure.config;

import com.vointika.pickup.application.usecase.CreatePickupLocationUseCase;
import com.vointika.pickup.application.usecase.DeletePickupLocationUseCase;
import com.vointika.pickup.application.usecase.GetPickupLocationUseCase;
import com.vointika.pickup.application.usecase.ListPickupLocationsUseCase;
import com.vointika.pickup.application.usecase.UpdatePickupLocationUseCase;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.port.SlotPickupLocationBackfillPort;
import com.vointika.shared.port.SlotPickupLocationSnapshotPropagator;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("pickupUseCaseConfig")
public class PickupUseCaseConfig {

    @Bean
    public CreatePickupLocationUseCase createPickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            SlotPickupLocationBackfillPort slotPickupLocationBackfillPort,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator) {
        return new CreatePickupLocationUseCase(pickupLocationRepository, slotPickupLocationBackfillPort,
                membershipCheck, transactionRunner, idGenerator);
    }

    @Bean
    public UpdatePickupLocationUseCase updatePickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            SlotPickupLocationSnapshotPropagator slotPickupLocationSnapshotPropagator,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner) {
        return new UpdatePickupLocationUseCase(pickupLocationRepository, slotPickupLocationSnapshotPropagator,
                membershipCheck, transactionRunner);
    }

    @Bean
    public DeletePickupLocationUseCase deletePickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            SlotPickupLocationSnapshotPropagator slotPickupLocationSnapshotPropagator,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner) {
        return new DeletePickupLocationUseCase(pickupLocationRepository, slotPickupLocationSnapshotPropagator,
                membershipCheck, transactionRunner);
    }

    @Bean
    public GetPickupLocationUseCase getPickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetPickupLocationUseCase(pickupLocationRepository, membershipCheck);
    }

    @Bean
    public ListPickupLocationsUseCase listPickupLocationsUseCase(
            PickupLocationRepository pickupLocationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListPickupLocationsUseCase(pickupLocationRepository, membershipCheck);
    }
}
