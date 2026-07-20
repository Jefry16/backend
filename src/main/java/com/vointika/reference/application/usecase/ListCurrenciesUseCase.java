package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.repository.CurrencyRepository;

import java.util.List;

public class ListCurrenciesUseCase {

    private final CurrencyRepository currencyRepository;

    public ListCurrenciesUseCase(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public List<Currency> execute() {
        return currencyRepository.findAll();
    }
}
