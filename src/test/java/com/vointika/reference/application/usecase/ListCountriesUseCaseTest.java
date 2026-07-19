package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.repository.CountryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListCountriesUseCaseTest {

    private final CountryRepository repository = mock(CountryRepository.class);
    private final ListCountriesUseCase useCase = new ListCountriesUseCase(repository);

    @Test
    void returnsRepositoryFindAll() {
        Country spain = new Country(UUID.randomUUID(), "ES", "Spain", "flags/es.svg");
        Country us = new Country(UUID.randomUUID(), "US", "United States", "flags/us.svg");
        when(repository.findAll()).thenReturn(List.of(spain, us));

        List<Country> result = useCase.execute();

        assertThat(result).containsExactly(spain, us);
        verify(repository).findAll();
    }

    @Test
    void returnsEmptyListWhenNoneExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
