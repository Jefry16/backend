package com.vointika.metafield.infrastructure.config;

import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.port.JsonSyntaxPort;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.application.usecase.CreateMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetafieldValueUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldDefinitionsUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldValuesUseCase;
import com.vointika.metafield.application.usecase.UpdateMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.DeleteMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldValueUseCase;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("metafieldUseCaseConfig")
public class MetafieldUseCaseConfig {

    @Bean
    public MetafieldValueValidator metafieldValueValidator(JsonSyntaxPort jsonSyntax) {
        return new MetafieldValueValidator(jsonSyntax);
    }

    @Bean
    public MetafieldOwnerAccess metafieldOwnerAccess(
            ExperienceOwnershipQuery experienceOwnershipQuery,
            PageOwnershipQuery pageOwnershipQuery) {
        return new MetafieldOwnerAccess(experienceOwnershipQuery, pageOwnershipQuery);
    }

    @Bean
    public CreateMetafieldDefinitionUseCase createMetafieldDefinitionUseCase(
            MetafieldDefinitionRepository definitionRepository,
            MetaobjectDefinitionRepository metaobjectDefinitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreateMetafieldDefinitionUseCase(definitionRepository,
                metaobjectDefinitionRepository, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpdateMetafieldDefinitionUseCase updateMetafieldDefinitionUseCase(
            MetafieldDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateMetafieldDefinitionUseCase(definitionRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public GetMetafieldDefinitionUseCase getMetafieldDefinitionUseCase(
            MetafieldDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMetafieldDefinitionUseCase(definitionRepository, membershipCheck);
    }

    @Bean
    public ListMetafieldDefinitionsUseCase listMetafieldDefinitionsUseCase(
            MetafieldDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetafieldDefinitionsUseCase(definitionRepository, membershipCheck);
    }

    @Bean
    public DeleteMetafieldDefinitionUseCase deleteMetafieldDefinitionUseCase(
            MetafieldDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetafieldDefinitionUseCase(definitionRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public UpsertMetafieldValueUseCase upsertMetafieldValueUseCase(
            MetafieldDefinitionRepository definitionRepository,
            MetafieldValueRepository valueRepository,
            MetaobjectEntryRepository metaobjectEntryRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            MetafieldValueValidator metafieldValueValidator,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertMetafieldValueUseCase(definitionRepository, valueRepository,
                metaobjectEntryRepository, metafieldOwnerAccess, metafieldValueValidator, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpsertMetafieldTranslationsUseCase upsertMetafieldTranslationsUseCase(
            MetafieldDefinitionRepository definitionRepository,
            MetafieldValueRepository valueRepository,
            MetafieldValueTranslationRepository translationRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            MetafieldValueValidator metafieldValueValidator,
            OperatorLocalesQuery operatorLocalesQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertMetafieldTranslationsUseCase(definitionRepository, valueRepository,
                translationRepository, metafieldOwnerAccess, metafieldValueValidator,
                operatorLocalesQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetMetafieldTranslationsUseCase getMetafieldTranslationsUseCase(
            MetafieldValueTranslationRepository translationRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMetafieldTranslationsUseCase(
                translationRepository, metafieldOwnerAccess, membershipCheck);
    }

    @Bean
    public ListMetafieldTranslationLocalesUseCase listMetafieldTranslationLocalesUseCase(
            MetafieldValueTranslationRepository translationRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetafieldTranslationLocalesUseCase(
                translationRepository, metafieldOwnerAccess, membershipCheck);
    }

    @Bean
    public DeleteMetafieldTranslationsUseCase deleteMetafieldTranslationsUseCase(
            MetafieldValueTranslationRepository translationRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetafieldTranslationsUseCase(translationRepository, metafieldOwnerAccess,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public ListMetafieldValuesUseCase listMetafieldValuesUseCase(
            MetafieldValueRepository valueRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetafieldValuesUseCase(valueRepository, metafieldOwnerAccess, membershipCheck);
    }

    @Bean
    public DeleteMetafieldValueUseCase deleteMetafieldValueUseCase(
            MetafieldDefinitionRepository definitionRepository,
            MetafieldValueRepository valueRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetafieldValueUseCase(definitionRepository, valueRepository,
                metafieldOwnerAccess, membershipCheck, transactionRunner, auditTrailPort);
    }
}
