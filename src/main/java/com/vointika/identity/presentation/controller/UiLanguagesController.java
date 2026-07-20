package com.vointika.identity.presentation.controller;

import com.vointika.identity.application.usecase.ListUiLanguagesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The supported admin-UI languages — the frontend renders its language picker
 * from this. Authenticated (a logged-in, post-login concern).
 */
@RestController
@RequestMapping("/api/ui-languages")
public class UiLanguagesController {

    private final ListUiLanguagesUseCase listUiLanguagesUseCase;

    public UiLanguagesController(ListUiLanguagesUseCase listUiLanguagesUseCase) {
        this.listUiLanguagesUseCase = listUiLanguagesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(listUiLanguagesUseCase.execute());
    }
}
