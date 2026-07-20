package com.vointika.touroperator.infrastructure.config;

import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.service.SlugGenerator;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("tourOperatorUseCaseConfig")
public class TourOperatorUseCaseConfig {

    @Bean
    public SlugGenerator slugGenerator() {
        return new SlugGenerator();
    }

    @Bean
    public CreateTourOperatorUseCase createTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMemberRepository tourOperatorMemberRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            SlugGenerator slugGenerator,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator) {
        return new CreateTourOperatorUseCase(
                tourOperatorRepository,
                tourOperatorMemberRepository,
                timezoneRepository,
                currencyRepository,
                slugGenerator,
                transactionRunner,
                idGenerator
        );
    }
}
