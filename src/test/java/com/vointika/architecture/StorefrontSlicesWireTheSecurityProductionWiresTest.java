package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A storefront {@code @WebMvcTest} that loads {@code SecurityConfig} also loads
 * {@code StorefrontUnauthenticatedRequests}.
 *
 * <p><b>Without it the slice describes a world production does not live in.</b>
 * That policy turns an unmatched path on a tenant host into the storefront's 404;
 * a slice missing it answers 401 instead, and any assertion about a path the
 * routes do not match then pins the wrong status — passing, permanently, against
 * behaviour the running stack contradicts.
 *
 * <p><b>Written as an invariant because the same mistake landed twice in two
 * slices.</b> {@code StorefrontCmsPageControllerTest.aSegmentThatIsNotHandleShaped
 * IsNotAPageRoute} asserted {@code isUnauthorized()} — the bug it should have
 * caught — and one slice later
 * {@code StorefrontContactControllerTest.thePageDoesNotAcceptASubmission} was
 * written the same way and passed. Both were found by curling the rebuilt stack,
 * not by the suite (PATTERNS §9: fixing the same thing twice means the subject is
 * a set).
 *
 * <p>It reads source rather than reflecting over annotations because a slice's
 * {@code @Import} is a compile-time literal, and the failure has to name the file
 * to edit.
 *
 * <p><b>It looks inside the {@code @Import} list, not the whole file</b>, and that
 * distinction is this guard's own near-miss. The {@code import} statement for the
 * class contains the same word, so a whole-file search reports a slice as wired
 * when it merely imports the type. Measured: the first version of this test passed
 * on a slice with the entry deleted from {@code @Import} and the import line left
 * behind — which is exactly what deleting it looks like.
 */
class StorefrontSlicesWireTheSecurityProductionWiresTest {

    private static final Path CONTROLLER_TESTS =
            Path.of("src/test/java/com/vointika/storefront/presentation/controller");

    private static final String SECURITY = "@Import({SecurityConfig.class";
    private static final String POLICY = "StorefrontUnauthenticatedRequests";

    @Test
    void everyStorefrontSliceThatLoadsSecurityAlsoLoadsTheNotFoundPolicy() throws IOException {
        List<String> missing = new ArrayList<>();
        int examined = 0;

        try (var files = Files.walk(CONTROLLER_TESTS)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Test.java")).toList()) {
                String source = Files.readString(file);
                int annotation = source.indexOf(SECURITY);
                if (annotation < 0) {
                    continue;
                }
                examined++;
                String importList = source.substring(annotation, source.indexOf(')', annotation) + 1);
                if (!importList.contains(POLICY)) {
                    missing.add(file.getFileName().toString());
                }
            }
        }

        // A walk that finds no security-loading slices passes the assertion below
        // without checking anything.
        assertThat(examined)
                .withFailMessage("Found no storefront controller slices importing SecurityConfig, so this "
                        + "guard checked nothing. Fix the scan rather than deleting the test.")
                .isGreaterThan(3);

        assertThat(missing)
                .withFailMessage("""
                        These storefront slices load SecurityConfig without \
                        StorefrontUnauthenticatedRequests:
                          %s
                        Production always wires both. Without the policy the slice answers 401 where the \
                        running stack answers the storefront's 404, so any assertion about an unmatched \
                        path pins behaviour that does not exist. Add it to @Import.""",
                        String.join("\\n  - ", missing))
                .isEmpty();
    }
}
