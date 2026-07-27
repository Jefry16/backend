package com.vointika.page.infrastructure.config;

import com.vointika.page.application.usecase.CreatePageUseCase;
import com.vointika.page.application.usecase.DeletePageTranslationUseCase;
import com.vointika.page.application.usecase.DeletePageUseCase;
import com.vointika.page.application.usecase.GetPageTranslationUseCase;
import com.vointika.page.application.usecase.GetPageUseCase;
import com.vointika.page.application.usecase.ListPageTranslationsUseCase;
import com.vointika.page.application.usecase.ListPagesUseCase;
import com.vointika.page.application.usecase.PublishPageUseCase;
import com.vointika.page.application.usecase.RenamePageUseCase;
import com.vointika.page.application.usecase.UnpublishPageUseCase;
import com.vointika.page.application.usecase.UpdatePageUseCase;
import com.vointika.page.application.usecase.UpsertPageTranslationUseCase;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.service.SlugGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("pageUseCaseConfig")
public class PageUseCaseConfig {

    @Bean
    public CreatePageUseCase createPageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreatePageUseCase(pageRepository, membershipCheck, idGenerator,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public UpdatePageUseCase updatePageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdatePageUseCase(pageRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetPageUseCase getPageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetPageUseCase(pageRepository, membershipCheck);
    }

    @Bean
    public ListPagesUseCase listPagesUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListPagesUseCase(pageRepository, membershipCheck);
    }

    @Bean
    public PublishPageUseCase publishPageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new PublishPageUseCase(pageRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public UnpublishPageUseCase unpublishPageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UnpublishPageUseCase(pageRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public RenamePageUseCase renamePageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RenamePageUseCase(pageRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public DeletePageUseCase deletePageUseCase(
            PageRepository pageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeletePageUseCase(pageRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpsertPageTranslationUseCase upsertPageTranslationUseCase(
            PageRepository pageRepository,
            PageTranslationRepository pageTranslationRepository,
            OperatorLocalesQuery operatorLocalesQuery,
            SlugGenerator slugGenerator,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertPageTranslationUseCase(pageRepository, pageTranslationRepository,
                operatorLocalesQuery, slugGenerator, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetPageTranslationUseCase getPageTranslationUseCase(
            PageRepository pageRepository,
            PageTranslationRepository pageTranslationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetPageTranslationUseCase(pageRepository, pageTranslationRepository, membershipCheck);
    }

    @Bean
    public ListPageTranslationsUseCase listPageTranslationsUseCase(
            PageRepository pageRepository,
            PageTranslationRepository pageTranslationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListPageTranslationsUseCase(pageRepository, pageTranslationRepository, membershipCheck);
    }

    @Bean
    public DeletePageTranslationUseCase deletePageTranslationUseCase(
            PageRepository pageRepository,
            PageTranslationRepository pageTranslationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeletePageTranslationUseCase(pageRepository, pageTranslationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }
}
