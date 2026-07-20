package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListCurrenciesUseCaseTest {

    private final CurrencyRepository repository = mock(CurrencyRepository.class);
    private final ListCurrenciesUseCase useCase = new ListCurrenciesUseCase(repository);

    @Test
    void returnsRepositoryFindAll() {
        Currency eur = new Currency(UUID.randomUUID(), "EUR", "Euro", "€");
        Currency usd = new Currency(UUID.randomUUID(), "USD", "US Dollar", "$");
        when(repository.findAll()).thenReturn(List.of(eur, usd));

        List<Currency> result = useCase.execute();

        assertThat(result).containsExactly(eur, usd);
        verify(repository).findAll();
    }

    @Test
    void returnsEmptyListWhenNoneExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
