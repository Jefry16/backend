package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListCountriesUseCase;
import com.vointika.reference.presentation.response.CountryResponse;
import com.vointika.shared.media.MediaUrlResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final ListCountriesUseCase listCountriesUseCase;
    private final MediaUrlResolver mediaUrlResolver;

    public CountryController(ListCountriesUseCase listCountriesUseCase, MediaUrlResolver mediaUrlResolver) {
        this.listCountriesUseCase = listCountriesUseCase;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> list() {
        var countries = listCountriesUseCase.execute().stream()
                .map(c -> new CountryResponse(c.getId(), c.getCode(), c.getName(),
                        mediaUrlResolver.toUrl(c.getFlagKey())))
                .toList();
        return ResponseEntity.ok(countries);
    }
}
