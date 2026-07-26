package com.vointika.touroperator.infrastructure.config;

import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.InvitedUserProvisioning;
import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.shared.service.SlugGenerator;
import com.vointika.touroperator.application.usecase.AcceptInvitationUseCase;
import com.vointika.touroperator.application.usecase.ChangeMemberRoleUseCase;
import com.vointika.touroperator.application.usecase.ClearOperatorLogoUseCase;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.GetMemberUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorLocalesUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationPreviewUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationUseCase;
import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ListInvitationsUseCase;
import com.vointika.touroperator.application.usecase.ListMembersUseCase;
import com.vointika.touroperator.application.usecase.RemoveTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ResendInvitationUseCase;
import com.vointika.touroperator.application.usecase.RevokeInvitationUseCase;
import com.vointika.touroperator.application.usecase.SetOperatorLogoUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorLocalesUseCase;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("tourOperatorUseCaseConfig")
public class TourOperatorUseCaseConfig {

    @Bean
    public CreateTourOperatorUseCase createTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMemberRepository tourOperatorMemberRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            SlugGenerator slugGenerator,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            UserAccountQuery userAccountQuery,
            EventPublisherPort eventPublisher,
            AuditTrailPort auditTrailPort) {
        return new CreateTourOperatorUseCase(
                tourOperatorRepository,
                tourOperatorMemberRepository,
                timezoneRepository,
                currencyRepository,
                slugGenerator,
                transactionRunner,
                idGenerator,
                userAccountQuery,
                eventPublisher,
                auditTrailPort
        );
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
    public SetOperatorLogoUseCase setOperatorLogoUseCase(
            TourOperatorRepository tourOperatorRepository,
            MediaKeyBatchQuery mediaKeyBatchQuery,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new SetOperatorLogoUseCase(tourOperatorRepository, mediaKeyBatchQuery,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Bean
    public ClearOperatorLogoUseCase clearOperatorLogoUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new ClearOperatorLogoUseCase(tourOperatorRepository, membershipCheck,
                transactionRunner, auditTrailPort);
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
}
