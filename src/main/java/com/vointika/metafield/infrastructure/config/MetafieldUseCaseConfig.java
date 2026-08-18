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
import com.vointika.metafield.application.usecase.UpsertMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectFieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldValueUseCase;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.service.OperatorLocaleCheck;
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
    public UpsertMetaobjectFieldTranslationsUseCase upsertMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectDefinitionRepository metaobjectDefinitionRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            MetafieldValueValidator metafieldValueValidator,
            OperatorLocaleCheck operatorLocaleCheck,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertMetaobjectFieldTranslationsUseCase(entryRepository,
                metaobjectDefinitionRepository, translationRepository, metafieldValueValidator,
                operatorLocaleCheck, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetMetaobjectFieldTranslationsUseCase getMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMetaobjectFieldTranslationsUseCase(
                entryRepository, translationRepository, membershipCheck);
    }

    @Bean
    public ListMetaobjectFieldTranslationLocalesUseCase listMetaobjectFieldTranslationLocalesUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetaobjectFieldTranslationLocalesUseCase(
                entryRepository, translationRepository, membershipCheck);
    }

    @Bean
    public DeleteMetaobjectFieldTranslationsUseCase deleteMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetaobjectFieldTranslationsUseCase(entryRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpsertMetafieldTranslationsUseCase upsertMetafieldTranslationsUseCase(
            MetafieldValueRepository valueRepository,
            MetafieldValueTranslationRepository translationRepository,
            MetafieldOwnerAccess metafieldOwnerAccess,
            MetafieldValueValidator metafieldValueValidator,
            OperatorLocaleCheck operatorLocaleCheck,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertMetafieldTranslationsUseCase(valueRepository,
                translationRepository, metafieldOwnerAccess, metafieldValueValidator,
                operatorLocaleCheck, membershipCheck, transactionRunner, auditTrailPort);
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
