package com.vointika.audit.infrastructure.config;

import com.vointika.audit.application.usecase.GetAuditLogEntryUseCase;
import com.vointika.audit.application.usecase.ListAuditLogUseCase;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditUseCaseConfig {

    @Bean
    public ListAuditLogUseCase listAuditLogUseCase(AuditLogEntryRepository repository,
                                                   TourOperatorMembershipCheck membershipCheck) {
        return new ListAuditLogUseCase(repository, membershipCheck);
    }

    @Bean
    public GetAuditLogEntryUseCase getAuditLogEntryUseCase(AuditLogEntryRepository repository,
                                                           TourOperatorMembershipCheck membershipCheck) {
        return new GetAuditLogEntryUseCase(repository, membershipCheck);
    }
}
