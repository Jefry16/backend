package com.vointika.touroperator.infrastructure.config;

import com.vointika.shared.port.DiagnosticLogPort;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.InvitedUserProvisioning;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.shared.service.HandleGenerator;
import com.vointika.touroperator.application.policy.TourOperatorMembershipPolicy;
import com.vointika.touroperator.application.usecase.AcceptInvitationUseCase;
import com.vointika.touroperator.application.usecase.ChangeMemberRoleUseCase;
import com.vointika.touroperator.application.usecase.CreateMenuUseCase;
import com.vointika.touroperator.application.port.StorefrontPasswordGeneratorPort;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.DeleteMenuUseCase;
import com.vointika.touroperator.application.usecase.GetMemberUseCase;
import com.vointika.touroperator.application.usecase.GetMenuUseCase;
import com.vointika.touroperator.application.usecase.GetStorefrontPasswordUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorLocalesUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationPreviewUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationUseCase;
import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ListInvitationsUseCase;
import com.vointika.touroperator.application.usecase.ListMembersUseCase;
import com.vointika.touroperator.application.usecase.ListMenusUseCase;
import com.vointika.touroperator.application.usecase.RemoveTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.RenameMenuUseCase;
import com.vointika.touroperator.application.usecase.ReplaceMenuItemsUseCase;
import com.vointika.touroperator.application.usecase.ResendInvitationUseCase;
import com.vointika.touroperator.application.usecase.RevokeInvitationUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorLocalesUseCase;
import com.vointika.touroperator.application.usecase.UpdateStorefrontPasswordUseCase;
import com.vointika.touroperator.domain.repository.MenuItemRepository;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.touroperator.application.usecase.UpdateOperatorSeoUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorSeoUseCase;
import com.vointika.touroperator.application.usecase.DeleteOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListOperatorTranslationsUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.UpsertOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.application.usecase.GetPolicyUseCase;
import com.vointika.touroperator.application.usecase.GetTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.UpdateTourOperatorUseCase;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.touroperator.application.usecase.GetBrandUseCase;
import com.vointika.touroperator.application.usecase.UpdateBrandUseCase;
import com.vointika.touroperator.application.usecase.CreatePolicyUseCase;
import com.vointika.touroperator.application.usecase.UpdatePolicyUseCase;
import com.vointika.touroperator.application.usecase.DeletePolicyUseCase;
import com.vointika.touroperator.application.usecase.ListPolicyTranslationsUseCase;
import com.vointika.touroperator.application.usecase.UpsertPolicyTranslationUseCase;
import com.vointika.touroperator.application.usecase.DeletePolicyTranslationUseCase;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;

@Configuration("tourOperatorUseCaseConfig")
public class TourOperatorUseCaseConfig {

    /** Hand-wired like every other application-layer collaborator: the policy is a
     * plain POJO so the layer stays framework-free (ArchUnit enforces it). */
    @Bean
    public TourOperatorMembershipPolicy tourOperatorMembershipPolicy(
            TourOperatorMemberRepository memberRepository) {
        return new TourOperatorMembershipPolicy(memberRepository);
    }

    @Bean
    public CreateTourOperatorUseCase createTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMemberRepository tourOperatorMemberRepository,
            MenuRepository menuRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            CountryRepository countryRepository,
            HandleGenerator handleGenerator,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            UserAccountQuery userAccountQuery,
            EventPublisherPort eventPublisher,
            AuditTrailPort auditTrailPort,
            DiagnosticLogPort diagnosticLog,
            StorefrontPasswordGeneratorPort storefrontPasswordGenerator) {
        return new CreateTourOperatorUseCase(
                tourOperatorRepository,
                tourOperatorMemberRepository,
                menuRepository,
                timezoneRepository,
                currencyRepository,
                countryRepository,
                handleGenerator,
                transactionRunner,
                idGenerator,
                userAccountQuery,
                eventPublisher,
                auditTrailPort,
                diagnosticLog,
                storefrontPasswordGenerator);
    }

    @Bean
    public InviteTeamMemberUseCase inviteTeamMemberUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorMemberRepository memberRepository,
            TourOperatorRepository tourOperatorRepository,
            UserAccountQuery userAccountQuery,
            TourOperatorMembershipCheck membershipCheck,
            InvitationTokenPort invitationTokenPort,
            IdGenerator idGenerator,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new InviteTeamMemberUseCase(
                invitationRepository, memberRepository, tourOperatorRepository,
                userAccountQuery, membershipCheck, invitationTokenPort,
                idGenerator, eventPublisher, transactionRunner, auditTrailPort);
    }

    @Bean
    public AcceptInvitationUseCase acceptInvitationUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorMemberRepository memberRepository,
            TourOperatorRepository tourOperatorRepository,
            UserAccountQuery userAccountQuery,
            InvitedUserProvisioning invitedUserProvisioning,
            InvitationTokenPort invitationTokenPort,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new AcceptInvitationUseCase(
                invitationRepository, memberRepository, tourOperatorRepository,
                userAccountQuery, invitedUserProvisioning, invitationTokenPort,
                idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetInvitationPreviewUseCase getInvitationPreviewUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorRepository tourOperatorRepository,
            InvitationTokenPort invitationTokenPort) {
        return new GetInvitationPreviewUseCase(
                invitationRepository, tourOperatorRepository, invitationTokenPort);
    }

    @Bean
    public GetOperatorLocalesUseCase getOperatorLocalesUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetOperatorLocalesUseCase(tourOperatorRepository, membershipCheck);
    }

    @Bean
    public UpdateOperatorLocalesUseCase updateOperatorLocalesUseCase(
            TourOperatorRepository tourOperatorRepository,
            LanguageRepository languageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateOperatorLocalesUseCase(tourOperatorRepository, languageRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }



    @Bean
    public ListInvitationsUseCase listInvitationsUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListInvitationsUseCase(invitationRepository, membershipCheck);
    }

    @Bean
    public GetInvitationUseCase getInvitationUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetInvitationUseCase(invitationRepository, membershipCheck);
    }

    @Bean
    public RevokeInvitationUseCase revokeInvitationUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RevokeInvitationUseCase(invitationRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public ResendInvitationUseCase resendInvitationUseCase(
            TourOperatorInvitationRepository invitationRepository,
            TourOperatorRepository tourOperatorRepository,
            UserAccountQuery userAccountQuery,
            TourOperatorMembershipCheck membershipCheck,
            InvitationTokenPort invitationTokenPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new ResendInvitationUseCase(invitationRepository, tourOperatorRepository,
                userAccountQuery, membershipCheck, invitationTokenPort, eventPublisher,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public ListMembersUseCase listMembersUseCase(
            TourOperatorMemberRepository memberRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMembersUseCase(memberRepository, membershipCheck);
    }

    @Bean
    public GetMemberUseCase getMemberUseCase(
            TourOperatorMemberRepository memberRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMemberUseCase(memberRepository, membershipCheck);
    }

    @Bean
    public ChangeMemberRoleUseCase changeMemberRoleUseCase(
            TourOperatorMemberRepository memberRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new ChangeMemberRoleUseCase(memberRepository, membershipCheck, transactionRunner,
                auditTrailPort);
    }

    @Bean
    public RemoveTeamMemberUseCase removeTeamMemberUseCase(
            TourOperatorMemberRepository memberRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RemoveTeamMemberUseCase(memberRepository, membershipCheck, transactionRunner,
                auditTrailPort);
    }

    @Bean
    public CreateMenuUseCase createMenuUseCase(
            MenuRepository menuRepository,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new CreateMenuUseCase(menuRepository, membershipCheck, idGenerator,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public RenameMenuUseCase renameMenuUseCase(
            MenuRepository menuRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new RenameMenuUseCase(menuRepository, membershipCheck, transactionRunner,
                auditTrailPort);
    }

    @Bean
    public ListMenusUseCase listMenusUseCase(
            MenuRepository menuRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMenusUseCase(menuRepository, membershipCheck);
    }

    @Bean
    public GetMenuUseCase getMenuUseCase(
            MenuRepository menuRepository,
            MenuItemRepository menuItemRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMenuUseCase(menuRepository, menuItemRepository, membershipCheck);
    }

    @Bean
    public DeleteMenuUseCase deleteMenuUseCase(
            MenuRepository menuRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMenuUseCase(menuRepository, membershipCheck, transactionRunner,
                auditTrailPort);
    }

    @Bean
    public GetStorefrontPasswordUseCase getStorefrontPasswordUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetStorefrontPasswordUseCase(tourOperatorRepository, membershipCheck);
    }

    @Bean
    public UpdateStorefrontPasswordUseCase updateStorefrontPasswordUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateStorefrontPasswordUseCase(
                tourOperatorRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public ReplaceMenuItemsUseCase replaceMenuItemsUseCase(
            MenuRepository menuRepository,
            MenuItemRepository menuItemRepository,
            TourOperatorRepository tourOperatorRepository,
            ExperienceOwnershipQuery experienceOwnershipQuery,
            PageOwnershipQuery pageOwnershipQuery,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new ReplaceMenuItemsUseCase(menuRepository, menuItemRepository,
                tourOperatorRepository, experienceOwnershipQuery, pageOwnershipQuery,
                membershipCheck, idGenerator, transactionRunner, auditTrailPort);
    }

    @Bean
    public UpsertOperatorTranslationUseCase upsertOperatorTranslationUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorTranslationRepository translationRepository,
            OperatorLocalesQuery operatorLocalesQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertOperatorTranslationUseCase(tourOperatorRepository, translationRepository,
                operatorLocalesQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetOperatorTranslationUseCase getOperatorTranslationUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetOperatorTranslationUseCase(
                tourOperatorRepository, translationRepository, membershipCheck);
    }

    @Bean
    public ListOperatorTranslationsUseCase listOperatorTranslationsUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListOperatorTranslationsUseCase(
                tourOperatorRepository, translationRepository, membershipCheck);
    }

    @Bean
    public DeleteOperatorTranslationUseCase deleteOperatorTranslationUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteOperatorTranslationUseCase(tourOperatorRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public GetOperatorSeoUseCase getOperatorSeoUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetOperatorSeoUseCase(tourOperatorRepository, membershipCheck);
    }

    @Bean
    public UpdateOperatorSeoUseCase updateOperatorSeoUseCase(
            TourOperatorRepository tourOperatorRepository,
            MediaAssetBatchQuery mediaAssetBatchQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateOperatorSeoUseCase(tourOperatorRepository, mediaAssetBatchQuery,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    // ---- store policies ----

    @Bean
    public ListPoliciesUseCase listPoliciesUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListPoliciesUseCase(policyRepository, membershipCheck);
    }

    @Bean
    public GetPolicyUseCase getPolicyUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetPolicyUseCase(policyRepository, membershipCheck);
    }

    @Bean
    public CreatePolicyUseCase createPolicyUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorPolicyRepository policyRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort,
            IdGenerator idGenerator) {
        return new CreatePolicyUseCase(tourOperatorRepository, policyRepository,
                membershipCheck, transactionRunner, auditTrailPort, idGenerator);
    }

    @Bean
    public UpdatePolicyUseCase updatePolicyUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdatePolicyUseCase(policyRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public DeletePolicyUseCase deletePolicyUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeletePolicyUseCase(policyRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public ListPolicyTranslationsUseCase listPolicyTranslationsUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorPolicyTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListPolicyTranslationsUseCase(policyRepository, translationRepository,
                membershipCheck);
    }

    @Bean
    public UpsertPolicyTranslationUseCase upsertPolicyTranslationUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorPolicyTranslationRepository translationRepository,
            OperatorLocalesQuery operatorLocalesQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpsertPolicyTranslationUseCase(policyRepository, translationRepository,
                operatorLocalesQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public DeletePolicyTranslationUseCase deletePolicyTranslationUseCase(
            TourOperatorPolicyRepository policyRepository,
            TourOperatorPolicyTranslationRepository translationRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeletePolicyTranslationUseCase(policyRepository, translationRepository,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    // ---- brand ----

    @Bean
    public GetBrandUseCase getBrandUseCase(
            TourOperatorBrandRepository brandRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetBrandUseCase(brandRepository, membershipCheck);
    }

    @Bean
    public UpdateBrandUseCase updateBrandUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorBrandRepository brandRepository,
            MediaAssetBatchQuery mediaAssetBatchQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateBrandUseCase(tourOperatorRepository, brandRepository,
                mediaAssetBatchQuery, membershipCheck, transactionRunner, auditTrailPort);
    }

    // ---- the operator's own details ----

    @Bean
    public GetTourOperatorUseCase getTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck,
            CountryRepository countryRepository) {
        return new GetTourOperatorUseCase(tourOperatorRepository, membershipCheck, countryRepository);
    }

    @Bean
    public UpdateTourOperatorUseCase updateTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            CountryRepository countryRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UpdateTourOperatorUseCase(tourOperatorRepository, countryRepository,
                timezoneRepository, currencyRepository, membershipCheck, transactionRunner,
                auditTrailPort);
    }
}
