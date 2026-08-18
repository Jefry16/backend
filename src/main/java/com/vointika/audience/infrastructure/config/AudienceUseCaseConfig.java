package com.vointika.audience.infrastructure.config;

import com.vointika.audience.application.usecase.CreateAudienceUseCase;
import com.vointika.audience.application.usecase.DeleteAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.GetAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.ListAudienceTranslationsUseCase;
import com.vointika.audience.application.usecase.UpsertAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.GetAudienceUseCase;
import com.vointika.audience.application.usecase.ListAudiencesUseCase;
import com.vointika.audience.application.usecase.UpdateAudienceUseCase;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.service.OperatorLocaleCheck;
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
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreateAudienceUseCase(audienceRepository, membershipCheck, idGenerator,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public UpdateAudienceUseCase updateAudienceUseCase(
            AudienceRepository audienceRepository,
            SlotAudienceSnapshotPropagator slotAudienceSnapshotPropagator,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateAudienceUseCase(audienceRepository, slotAudienceSnapshotPropagator,
                membershipCheck, transactionRunner, auditTrailPort);
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
    public GetAudienceTranslationUseCase getAudienceTranslationUseCase(
            AudienceRepository audienceRepository,
            AudienceTranslationRepository audienceTranslationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetAudienceTranslationUseCase(audienceRepository, audienceTranslationRepository, membershipCheck);
    }

    @Bean
    public ListAudienceTranslationsUseCase listAudienceTranslationsUseCase(
            AudienceRepository audienceRepository,
            AudienceTranslationRepository audienceTranslationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListAudienceTranslationsUseCase(audienceRepository, audienceTranslationRepository, membershipCheck);
    }

    // Was MISSING entirely (the import existed, the bean did not) — a boot-time
    // failure for any context containing AudienceTranslationController.
    @Bean
    public UpsertAudienceTranslationUseCase upsertAudienceTranslationUseCase(
            AudienceRepository audienceRepository,
            AudienceTranslationRepository audienceTranslationRepository,
            OperatorLocaleCheck operatorLocaleCheck,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertAudienceTranslationUseCase(audienceRepository, audienceTranslationRepository,
                operatorLocaleCheck, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public DeleteAudienceTranslationUseCase deleteAudienceTranslationUseCase(
            AudienceRepository audienceRepository,
            AudienceTranslationRepository audienceTranslationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteAudienceTranslationUseCase(audienceRepository, audienceTranslationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }
}