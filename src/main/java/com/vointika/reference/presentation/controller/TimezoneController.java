package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListTimezonesUseCase;
import com.vointika.reference.presentation.response.CountryResponse;
import com.vointika.reference.presentation.response.TimezoneResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timezones")
public class TimezoneController {

    private final ListTimezonesUseCase listTimezonesUseCase;

    public TimezoneController(ListTimezonesUseCase listTimezonesUseCase) {
        this.listTimezonesUseCase = listTimezonesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<TimezoneResponse>> list() {
        var timezones = listTimezonesUseCase.execute().stream()
                .map(t -> new TimezoneResponse(
                        t.getId(), t.getName(), t.getCityName(),
                        new CountryResponse(t.getCountry().getId(), t.getCountry().getCode(), t.getCountry().getName())))
                .toList();
        return ResponseEntity.ok(timezones);
    }
}
