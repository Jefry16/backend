package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListCurrenciesUseCase;
import com.vointika.reference.presentation.response.CurrencyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final ListCurrenciesUseCase listCurrenciesUseCase;

    public CurrencyController(ListCurrenciesUseCase listCurrenciesUseCase) {
        this.listCurrenciesUseCase = listCurrenciesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> list() {
        var currencies = listCurrenciesUseCase.execute().stream()
                .map(c -> new CurrencyResponse(
                        c.getId(), c.getCode(), c.getName(), c.getSymbol()))
                .toList();
        return ResponseEntity.ok(currencies);
    }
}
