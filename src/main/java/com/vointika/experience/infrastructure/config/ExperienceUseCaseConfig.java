package com.vointika.experience.infrastructure.config;

import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.application.usecase.CreateExperienceUseCase;
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
}
