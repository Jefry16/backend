package com.vointika.audience.infrastructure.config;

import com.vointika.audience.application.usecase.CreateAudienceUseCase;
import com.vointika.audience.application.usecase.DeleteAudienceUseCase;
import com.vointika.audience.application.usecase.GetAudienceUseCase;
import com.vointika.audience.application.usecase.ListAudiencesUseCase;
import com.vointika.audience.application.usecase.UpdateAudienceUseCase;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.shared.port.SlotAudienceSnapshotPropagator;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("audienceUseCaseConfig")
public class AudienceUseCaseConfig {

    @Bean
    public CreateAudienceUseCase createAudienceUseCase(
            AudienceRepository audienceRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner) {
        return new CreateAudienceUseCase(audienceRepository, membershipCheck, idGenerator, transactionRunner);
    }

    @Bean
    public UpdateAudienceUseCase updateAudienceUseCase(
            AudienceRepository audienceRepository,
            SlotAudienceSnapshotPropagator slotAudienceSnapshotPropagator,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner) {
        return new UpdateAudienceUseCase(audienceRepository, slotAudienceSnapshotPropagator,
                membershipCheck, transactionRunner);
    }

    @Bean
    public GetAudienceUseCase getAudienceUseCase(
            AudienceRepository audienceRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetAudienceUseCase(audienceRepository, membershipCheck);
    }

    @Bean
    public ListAudiencesUseCase listAudiencesUseCase(
            AudienceRepository audienceRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListAudiencesUseCase(audienceRepository, membershipCheck);
    }

    @Bean
    public DeleteAudienceUseCase deleteAudienceUseCase(
            AudienceRepository audienceRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new DeleteAudienceUseCase(audienceRepository, membershipCheck);
    }
}
