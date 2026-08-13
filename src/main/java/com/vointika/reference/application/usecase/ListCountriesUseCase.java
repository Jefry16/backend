package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.repository.CountryRepository;

import java.util.List;

public class ListCountriesUseCase {

    private final CountryRepository countryRepository;

    public ListCountriesUseCase(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public List<Country> execute() {
        return countryRepository.findAll();
    }
}
