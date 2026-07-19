package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListCountriesUseCase;
import com.vointika.reference.presentation.response.CountryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final ListCountriesUseCase listCountriesUseCase;

    public CountryController(ListCountriesUseCase listCountriesUseCase) {
        this.listCountriesUseCase = listCountriesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> list() {
        var countries = listCountriesUseCase.execute().stream()
                .map(c -> new CountryResponse(c.getId(), c.getCode(), c.getName()))
                .toList();
        return ResponseEntity.ok(countries);
    }
}
