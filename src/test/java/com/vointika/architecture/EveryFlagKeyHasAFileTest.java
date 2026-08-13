package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code flag_key} is a claim that an object exists. This is the first thing
 * that checks the claim.
 *
 * <p><b>Why it is worth a test.</b> PATTERNS §10 records the rule — "a seeded row
 * that names a storage object must ship the object" — and, in the same breath,
 * that nothing enforces it: a media row whose object is missing renders as a
 * broken image on every screen that references it. The country flags were the
 * standing example. `reference/V2` gave all three seeded countries a derived key
 * and made the column NOT NULL on the reasoning that the asset would follow; no
 * asset followed for months, and `/api/timezones` shipped a `flagUrl` pointing at
 * nothing the whole time.
 *
 * <p>It checks both directions, because each catches a different mistake: a code
 * with a key and no file is a broken image, and a file named for a country the
 * reference data does not have is a file nobody will ever fetch.
 */
class EveryFlagKeyHasAFileTest {

    private static final Path REFERENCE_MIGRATIONS = Path.of("src/main/resources/db/migration/reference");
    private static final Path FLAGS = Path.of("docker/dev-seed/flags");

    /**
     * The countries {@code V1} seeds are exactly the ones {@code V2} backfilled a
     * key for; {@code V6}'s 246 are inserted without one, deliberately.
     */
    private static Set<String> codesWithAFlagKey() throws IOException {
        Path v1 = REFERENCE_MIGRATIONS.resolve("V1__create_reference_data.sql");
        String sql = Files.readString(v1, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("'([A-Z]{2})',\\s*'[^']+'\\)").matcher(
                sql.substring(sql.indexOf("INSERT INTO reference.country")));
        Set<String> codes = new TreeSet<>();
        while (m.find()) {
            codes.add(m.group(1));
        }
        assertThat(codes)
                .withFailMessage("No country codes parsed out of %s. The seed was probably reshaped "
                        + "and this pattern no longer matches — fix the pattern, because a test that "
                        + "finds nothing checks nothing.", v1)
                .isNotEmpty();
        return codes;
    }

    private static Set<String> shippedFlagCodes() throws IOException {
        try (Stream<Path> files = Files.list(FLAGS)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".svg"))
                    .map(name -> name.substring(0, name.length() - 4).toUpperCase(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }

    @Test
    void everyCountryThatClaimsAFlagShipsOne() throws IOException {
        Set<String> missing = new TreeSet<>(codesWithAFlagKey());
        missing.removeAll(shippedFlagCodes());

        assertThat(missing)
                .withFailMessage("These countries carry a flag_key but no file in %s: %s%n"
                        + "The key resolves to a URL either way, so the symptom is a broken image "
                        + "rather than an error. Add the SVG, or leave the key null.", FLAGS, missing)
                .isEmpty();
    }

    @Test
    void everyShippedFlagBelongsToACountryThatClaimsOne() throws IOException {
        Set<String> orphans = new TreeSet<>(shippedFlagCodes());
        orphans.removeAll(codesWithAFlagKey());

        assertThat(orphans)
                .withFailMessage("These files in %s belong to no country with a flag_key: %s%n"
                        + "Nothing will ever fetch them. Either give that country a key or delete "
                        + "the file.", FLAGS, orphans)
                .isEmpty();
    }
}
