package com.vointika.contact.infrastructure.config;

import com.vointika.contact.application.usecase.DeleteContactMessageUseCase;
import com.vointika.contact.application.usecase.GetContactMessageUseCase;
import com.vointika.contact.application.usecase.ListContactMessagesUseCase;
import com.vointika.contact.application.usecase.SetContactMessageReadUseCase;
import com.vointika.contact.application.usecase.SubmitContactMessageUseCase;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("contactUseCaseConfig")
public class ContactUseCaseConfig {

    @Bean
    public ListContactMessagesUseCase listContactMessagesUseCase(
            ContactMessageRepository messageRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListContactMessagesUseCase(messageRepository, membershipCheck);
    }

    @Bean
    public GetContactMessageUseCase getContactMessageUseCase(
            ContactMessageRepository messageRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetContactMessageUseCase(messageRepository, membershipCheck);
    }

    @Bean
    public SetContactMessageReadUseCase setContactMessageReadUseCase(
            ContactMessageRepository messageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner) {
        return new SetContactMessageReadUseCase(messageRepository, membershipCheck,
                transactionRunner);
    }

    @Bean
    public DeleteContactMessageUseCase deleteContactMessageUseCase(
            ContactMessageRepository messageRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteContactMessageUseCase(messageRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public SubmitContactMessageUseCase submitContactMessageUseCase(
            ContactMessageRepository messageRepository,
            StorefrontOperatorQuery storefrontOperatorQuery,
            RateLimiterPort rateLimiter,
            IdGenerator idGenerator) {
        return new SubmitContactMessageUseCase(
                messageRepository, storefrontOperatorQuery, rateLimiter, idGenerator);
    }
}
