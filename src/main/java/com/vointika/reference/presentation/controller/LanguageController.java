package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListLanguagesUseCase;
import com.vointika.reference.presentation.response.LanguageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The platform's supported content languages — the master list operators enable
 * a subset of, and the frontend renders language pickers from. Authenticated.
 */
@RestController
@RequestMapping("/api/languages")
public class LanguageController {

    private final ListLanguagesUseCase listLanguagesUseCase;

    public LanguageController(ListLanguagesUseCase listLanguagesUseCase) {
        this.listLanguagesUseCase = listLanguagesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<LanguageResponse>> list() {
        var languages = listLanguagesUseCase.execute().stream()
                .map(l -> new LanguageResponse(l.getId(), l.getCode(), l.getName()))
                .toList();
        return ResponseEntity.ok(languages);
    }
}
