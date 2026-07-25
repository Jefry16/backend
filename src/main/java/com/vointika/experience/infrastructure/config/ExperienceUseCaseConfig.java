package com.vointika.experience.infrastructure.config;

import com.vointika.experience.application.service.AudiencePricingResolver;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.application.usecase.CancelSlotUseCase;
import com.vointika.experience.application.usecase.CreateExperienceUseCase;
import com.vointika.experience.application.usecase.CreateSlotUseCase;
import com.vointika.experience.application.usecase.CreateSlotsUseCase;
import com.vointika.experience.application.usecase.GetSlotUseCase;
import com.vointika.experience.application.usecase.ListSlotsUseCase;
import com.vointika.experience.application.usecase.UpdateSlotUseCase;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.port.AudienceOwnershipQuery;
import com.vointika.shared.port.OperatorTimezoneQuery;
import com.vointika.experience.application.usecase.DeleteExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.GetExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.GetExperienceUseCase;
import com.vointika.experience.application.usecase.ListExperienceTranslationsUseCase;
import com.vointika.experience.application.usecase.ListExperiencesUseCase;
import com.vointika.experience.application.usecase.PublishExperienceUseCase;
import com.vointika.experience.application.usecase.UnpublishExperienceUseCase;
import com.vointika.experience.application.usecase.UpdateExperienceUseCase;
import com.vointika.experience.application.usecase.UpsertExperienceTranslationUseCase;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.service.SlugGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("experienceUseCaseConfig")
public class ExperienceUseCaseConfig {

    @Bean
    public MediaReferenceValidator mediaReferenceValidator(MediaKeyBatchQuery mediaKeyBatchQuery) {
        return new MediaReferenceValidator(mediaKeyBatchQuery);
    }

    @Bean
    public CreateExperienceUseCase createExperienceUseCase(
            ExperienceRepository experienceRepository,
            MediaReferenceValidator mediaReferenceValidator,
            TourOperatorMembershipCheck membershipCheck,
            SlugGenerator slugGenerator,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner) {
        return new CreateExperienceUseCase(experienceRepository, mediaReferenceValidator,
                membershipCheck, slugGenerator, idGenerator, transactionRunner);
    }

    @Bean
    public ListExperiencesUseCase listExperiencesUseCase(
            ExperienceRepository experienceRepository,
            MediaUrlBatchResolver mediaUrlBatchResolver,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListExperiencesUseCase(experienceRepository, mediaUrlBatchResolver, membershipCheck);
    }

    @Bean
    public GetExperienceUseCase getExperienceUseCase(
            ExperienceRepository experienceRepository,
            MediaUrlBatchResolver mediaUrlBatchResolver,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetExperienceUseCase(experienceRepository, mediaUrlBatchResolver, membershipCheck);
    }

    @Bean
    public UpdateExperienceUseCase updateExperienceUseCase(
            ExperienceRepository experienceRepository,
            MediaReferenceValidator mediaReferenceValidator,
            TourOperatorMembershipCheck membershipCheck) {
        return new UpdateExperienceUseCase(experienceRepository, mediaReferenceValidator, membershipCheck);
    }

    @Bean
    public PublishExperienceUseCase publishExperienceUseCase(
            ExperienceRepository experienceRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new PublishExperienceUseCase(experienceRepository, membershipCheck);
    }

    @Bean
    public UnpublishExperienceUseCase unpublishExperienceUseCase(
            ExperienceRepository experienceRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new UnpublishExperienceUseCase(experienceRepository, membershipCheck);
    }

    @Bean
    public UpsertExperienceTranslationUseCase upsertExperienceTranslationUseCase(
            ExperienceRepository experienceRepository,
            ExperienceTranslationRepository translationRepository,
            OperatorLocalesQuery operatorLocalesQuery,
            SlugGenerator slugGenerator,
            TourOperatorMembershipCheck membershipCheck) {
        return new UpsertExperienceTranslationUseCase(experienceRepository, translationRepository,
                operatorLocalesQuery, slugGenerator, membershipCheck);
    }

    @Bean
    public GetExperienceTranslationUseCase getExperienceTranslationUseCase(
            ExperienceRepository experienceRepository,
            ExperienceTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetExperienceTranslationUseCase(experienceRepository, translationRepository, membershipCheck);
    }

    @Bean
    public ListExperienceTranslationsUseCase listExperienceTranslationsUseCase(
            ExperienceRepository experienceRepository,
            ExperienceTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListExperienceTranslationsUseCase(experienceRepository, translationRepository, membershipCheck);
    }

    @Bean
    public DeleteExperienceTranslationUseCase deleteExperienceTranslationUseCase(
            ExperienceRepository experienceRepository,
            ExperienceTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new DeleteExperienceTranslationUseCase(experienceRepository, translationRepository, membershipCheck);
    }

    // ---- slots ----

    @Bean
    public AudiencePricingResolver audiencePricingResolver(
            AudienceOwnershipQuery audienceOwnershipQuery, IdGenerator idGenerator) {
        return new AudiencePricingResolver(audienceOwnershipQuery, idGenerator);
    }

    @Bean
    public CreateSlotUseCase createSlotUseCase(
            ExperienceRepository experienceRepository,
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            AudiencePricingResolver audiencePricingResolver,
            OperatorTimezoneQuery operatorTimezoneQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator) {
        return new CreateSlotUseCase(experienceRepository, slotRepository, slotAudiencePricingRepository,
                audiencePricingResolver, operatorTimezoneQuery, membershipCheck, transactionRunner, idGenerator);
    }

    @Bean
    public CreateSlotsUseCase createSlotsUseCase(
            ExperienceRepository experienceRepository,
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            AudiencePricingResolver audiencePricingResolver,
            OperatorTimezoneQuery operatorTimezoneQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator) {
        return new CreateSlotsUseCase(experienceRepository, slotRepository, slotAudiencePricingRepository,
                audiencePricingResolver, operatorTimezoneQuery, membershipCheck, transactionRunner, idGenerator);
    }

    @Bean
    public GetSlotUseCase getSlotUseCase(
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetSlotUseCase(slotRepository, slotAudiencePricingRepository, membershipCheck);
    }

    @Bean
    public ListSlotsUseCase listSlotsUseCase(
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListSlotsUseCase(slotRepository, slotAudiencePricingRepository, membershipCheck);
    }

    @Bean
    public CancelSlotUseCase cancelSlotUseCase(
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new CancelSlotUseCase(slotRepository, slotAudiencePricingRepository, membershipCheck);
    }

    @Bean
    public UpdateSlotUseCase updateSlotUseCase(
            SlotRepository slotRepository,
            SlotAudiencePricingRepository slotAudiencePricingRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner) {
        return new UpdateSlotUseCase(slotRepository, slotAudiencePricingRepository, membershipCheck, transactionRunner);
    }
}
