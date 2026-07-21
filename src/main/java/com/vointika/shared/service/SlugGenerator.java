package com.vointika.shared.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Slug;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.function.Predicate;

/**
 * Derives a unique {@link Slug} from a free-form name. The caller supplies the
 * uniqueness scope via {@code existsCheck} — global (tour operators), per-operator
 * (experience canonical slugs), or per-operator-per-locale (localized slugs).
 * A shared {@code @Component} (stateless, no dependencies); still directly
 * newable in unit tests.
 *
 * <p>Slugify: NFD-normalize, drop combining marks (so "Café" → "cafe"),
 * lowercase, collapse any run of non-{@code [a-z0-9]} to a single dash, trim
 * dashes. On collision, append {@code -2}, {@code -3}, … The base is capped so
 * even the longest suffix stays within the {@link Slug} length limit.
 */
@Component
public class SlugGenerator {

    private static final int BASE_MAX_LENGTH = 145;   // + "-1000" stays under Slug's 170 cap
    private static final int MAX_ATTEMPTS = 1000;

    public Slug generateUnique(String sourceName, Predicate<String> existsCheck) {
        String base = slugify(sourceName);
        if (!existsCheck.test(base)) {
            return new Slug(base);
        }
        for (int i = 2; i <= MAX_ATTEMPTS; i++) {
            String candidate = base + "-" + i;
            if (!existsCheck.test(candidate)) {
                return new Slug(candidate);
            }
        }
        throw new InvalidFieldException(
                "Could not generate unique slug after " + MAX_ATTEMPTS + " attempts");
    }

    private static String slugify(String input) {
        if (input == null) {
            throw new InvalidFieldException("Name must contain at least one letter or digit");
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(normalized.length());
        boolean lastWasDash = false;
        for (int i = 0; i < normalized.length() && sb.length() < BASE_MAX_LENGTH; i++) {
            char c = Character.toLowerCase(normalized.charAt(i));
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue; // drop the accents NFD left behind
            }
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastWasDash = false;
            } else if (!lastWasDash && sb.length() > 0) {
                sb.append('-');
                lastWasDash = true;
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.deleteCharAt(sb.length() - 1);
        }
        String slug = sb.toString();
        if (slug.isBlank()) {
            throw new InvalidFieldException("Name must contain at least one letter or digit");
        }
        return slug;
    }
}
