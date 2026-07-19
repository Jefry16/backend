package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListTimezonesUseCaseTest {

    private final TimezoneRepository repository = mock(TimezoneRepository.class);
    private final ListTimezonesUseCase useCase = new ListTimezonesUseCase(repository);

    @Test
    void returnsRepositoryFindAll() {
        Timezone madrid = new Timezone(UUID.randomUUID(), "Europe/Madrid", "Madrid",
                new Country(UUID.randomUUID(), "ES", "Spain", null));
        Timezone newYork = new Timezone(UUID.randomUUID(), "America/New_York", "New York",
                new Country(UUID.randomUUID(), "US", "United States", null));
        when(repository.findAll()).thenReturn(List.of(madrid, newYork));

        List<Timezone> result = useCase.execute();

        assertThat(result).containsExactly(madrid, newYork);
        verify(repository).findAll();
    }

    @Test
    void returnsEmptyListWhenNoneExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
