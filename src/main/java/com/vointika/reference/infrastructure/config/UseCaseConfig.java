package com.vointika.reference.infrastructure.config;

import com.vointika.reference.application.usecase.ListCountriesUseCase;
import com.vointika.reference.application.usecase.ListTimezonesUseCase;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("referenceUseCaseConfig")
public class UseCaseConfig {

    @Bean
    public ListCountriesUseCase listCountriesUseCase(CountryRepository countryRepository) {
        return new ListCountriesUseCase(countryRepository);
    }

    @Bean
    public ListTimezonesUseCase listTimezonesUseCase(TimezoneRepository timezoneRepository) {
        return new ListTimezonesUseCase(timezoneRepository);
    }
}
