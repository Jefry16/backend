package com.vointika.metafield.infrastructure.config;

import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.application.usecase.AddMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.CreateMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.CreateMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectDefinitionsUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectEntriesUseCase;
import com.vointika.metafield.application.usecase.PublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.RemoveMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.RenameMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.UnpublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectEntryUseCase;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("metaobjectUseCaseConfig")
public class MetaobjectUseCaseConfig {

    @Bean
    public CreateMetaobjectDefinitionUseCase createMetaobjectDefinitionUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreateMetaobjectDefinitionUseCase(
                definitionRepository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpdateMetaobjectDefinitionUseCase updateMetaobjectDefinitionUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateMetaobjectDefinitionUseCase(
                definitionRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetMetaobjectDefinitionUseCase getMetaobjectDefinitionUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMetaobjectDefinitionUseCase(definitionRepository, membershipCheck);
    }

    @Bean
    public ListMetaobjectDefinitionsUseCase listMetaobjectDefinitionsUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetaobjectDefinitionsUseCase(definitionRepository, membershipCheck);
    }

    @Bean
    public DeleteMetaobjectDefinitionUseCase deleteMetaobjectDefinitionUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetaobjectDefinitionUseCase(
                definitionRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public AddMetaobjectFieldUseCase addMetaobjectFieldUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new AddMetaobjectFieldUseCase(
                definitionRepository, membershipCheck, idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public RenameMetaobjectFieldUseCase renameMetaobjectFieldUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RenameMetaobjectFieldUseCase(
                definitionRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public RemoveMetaobjectFieldUseCase removeMetaobjectFieldUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RemoveMetaobjectFieldUseCase(
                definitionRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public CreateMetaobjectEntryUseCase createMetaobjectEntryUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            MetaobjectEntryRepository entryRepository,
            MetafieldValueValidator valueValidator,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreateMetaobjectEntryUseCase(
                definitionRepository, entryRepository, valueValidator, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpdateMetaobjectEntryUseCase updateMetaobjectEntryUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            MetaobjectEntryRepository entryRepository,
            MetafieldValueValidator valueValidator,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateMetaobjectEntryUseCase(
                definitionRepository, entryRepository, valueValidator, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetMetaobjectEntryUseCase getMetaobjectEntryUseCase(
            MetaobjectDefinitionRepository definitionRepository,
            MetaobjectEntryRepository entryRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMetaobjectEntryUseCase(definitionRepository, entryRepository, membershipCheck);
    }

    @Bean
    public ListMetaobjectEntriesUseCase listMetaobjectEntriesUseCase(
            MetaobjectEntryRepository entryRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMetaobjectEntriesUseCase(entryRepository, membershipCheck);
    }

    @Bean
    public DeleteMetaobjectEntryUseCase deleteMetaobjectEntryUseCase(
            MetaobjectEntryRepository entryRepository,
            MetafieldValueRepository metafieldValueRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMetaobjectEntryUseCase(
                entryRepository, metafieldValueRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public PublishMetaobjectEntryUseCase publishMetaobjectEntryUseCase(
            MetaobjectEntryRepository entryRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new PublishMetaobjectEntryUseCase(
                entryRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public UnpublishMetaobjectEntryUseCase unpublishMetaobjectEntryUseCase(
            MetaobjectEntryRepository entryRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UnpublishMetaobjectEntryUseCase(
                entryRepository, membershipCheck, transactionRunner, auditTrailPort);
    }
}
