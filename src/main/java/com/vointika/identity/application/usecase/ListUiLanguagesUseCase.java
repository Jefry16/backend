package com.vointika.identity.application.usecase;

import java.util.List;

/**
 * Returns the admin-UI languages a user may pick (lowercase locale codes), in
 * configured order — the first is the default. Mirrors the allowlist that
 * {@code ChangeLanguageUseCase} validates against, so the frontend can render
 * the picker from a single source of truth (labels via Intl.DisplayNames).
 */
public class ListUiLanguagesUseCase {

    private final List<String> uiLanguages;

    public ListUiLanguagesUseCase(List<String> uiLanguages) {
        this.uiLanguages = uiLanguages;
    }

    public List<String> execute() {
        return uiLanguages;
    }
}
