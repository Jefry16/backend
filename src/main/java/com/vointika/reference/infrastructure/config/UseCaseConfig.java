package com.vointika.reference.infrastructure.config;

import com.vointika.reference.application.usecase.ListCountriesUseCase;
import com.vointika.reference.application.usecase.ListCurrenciesUseCase;
import com.vointika.reference.application.usecase.ListLanguagesUseCase;
import com.vointika.reference.application.usecase.ListTimezonesUseCase;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("referenceUseCaseConfig")
public class UseCaseConfig {

    @Bean
    public ListTimezonesUseCase listTimezonesUseCase(TimezoneRepository timezoneRepository) {
        return new ListTimezonesUseCase(timezoneRepository);
    }

    @Bean
    public ListCountriesUseCase listCountriesUseCase(CountryRepository countryRepository) {
        return new ListCountriesUseCase(countryRepository);
    }

    @Bean
    public ListCurrenciesUseCase listCurrenciesUseCase(CurrencyRepository currencyRepository) {
        return new ListCurrenciesUseCase(currencyRepository);
    }

    @Bean
    public ListLanguagesUseCase listLanguagesUseCase(LanguageRepository languageRepository) {
        return new ListLanguagesUseCase(languageRepository);
    }
}
