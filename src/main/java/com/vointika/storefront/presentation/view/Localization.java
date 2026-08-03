package com.vointika.storefront.presentation.view;

import com.vointika.storefront.application.dto.output.LocalizationData;

import java.util.List;
import java.util.function.Function;

/**
 * {@code {{localization}}} — the language this page is in, and a link to the same
 * page in each of the others.
 *
 * <p>{@code url} is the switcher's whole reason for existing and is why this
 * takes a function rather than a route: "the same page in another language" is
 * {@code /en} from the home page and {@code /en/experiences} from the listing,
 * so each page names its own member of {@link Routes} and the prefix rule stays
 * in one place.
 *
 * <p>Public, and so is {@link Language}, for the reason {@link Shop} documents.
 */
public record Localization(String locale, List<Language> languages) {

    /**
     * @param current true for the language being rendered — exactly one entry
     *                carries it, which is what lets a theme mark the switcher's
     *                active item without comparing strings
     */
    public record Language(String code, boolean current, String url) {}

    public static Localization from(LocalizationData localization, Function<Routes, String> thisPage) {
        return new Localization(
                localization.locale(),
                localization.languages().stream()
                        .map(language -> new Language(
                                language.code(),
                                language.current(),
                                thisPage.apply(Routes.forPathLocale(language.pathLocale()))))
                        .toList());
    }
}
