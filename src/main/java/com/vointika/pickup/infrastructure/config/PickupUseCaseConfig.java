package com.vointika.pickup.infrastructure.config;

import com.vointika.pickup.application.usecase.CreatePickupLocationUseCase;
import com.vointika.pickup.application.usecase.DeletePickupLocationUseCase;
import com.vointika.pickup.application.usecase.GetPickupLocationUseCase;
import com.vointika.pickup.application.usecase.ListPickupLocationsUseCase;
import com.vointika.pickup.application.usecase.UpdatePickupLocationUseCase;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.port.AuditTrailPort;
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
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            AuditTrailPort auditTrailPort) {
        return new CreatePickupLocationUseCase(pickupLocationRepository,
                membershipCheck, transactionRunner, idGenerator, auditTrailPort);
    }

    @Bean
    public UpdatePickupLocationUseCase updatePickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdatePickupLocationUseCase(pickupLocationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public DeletePickupLocationUseCase deletePickupLocationUseCase(
            PickupLocationRepository pickupLocationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeletePickupLocationUseCase(pickupLocationRepository, membershipCheck,
                transactionRunner, auditTrailPort);
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
