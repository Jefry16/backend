package com.vointika.reference.infrastructure.config;

import com.vointika.reference.application.usecase.ListTimezonesUseCase;
import com.vointika.reference.domain.repository.TimezoneRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("referenceUseCaseConfig")
public class UseCaseConfig {

    @Bean
    public ListTimezonesUseCase listTimezonesUseCase(TimezoneRepository timezoneRepository) {
        return new ListTimezonesUseCase(timezoneRepository);
    }
}
