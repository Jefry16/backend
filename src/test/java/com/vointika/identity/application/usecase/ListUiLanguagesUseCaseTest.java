package com.vointika.identity.application.usecase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListUiLanguagesUseCaseTest {

    @Test
    void returnsConfiguredLanguagesInOrder() {
        ListUiLanguagesUseCase useCase = new ListUiLanguagesUseCase(List.of("en", "es"));

        assertEquals(List.of("en", "es"), useCase.execute());
    }
}
