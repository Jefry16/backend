package com.vointika.storefront.infrastructure.config;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the compiler settings against silent reversion, which is the whole point
 * of writing them down: each one is the difference between a working public page
 * and either a 500 or a data leak, and none of them fails loudly on its own.
 *
 * <p>This class is {@code public} because its context objects are nested in it:
 * a {@code public} member of a package-private class is still unreachable
 * reflectively once access coercion is off, so nesting them here would make
 * every assertion below pass for the wrong reason.
 */
public class StorefrontMustacheConfigTest {

    private final Mustache.Compiler compiler = new StorefrontMustacheConfig().mustacheCompiler(
            name -> { throw new UnsupportedOperationException("this test compiles inline, never a partial"); });

    public static class Shop {
        private final String storefrontPassword = "hunter2";

        public String name() {
            return "Acme Tours";
        }

        public String getShopName() {
            return "Acme Tours";
        }
    }

    public record Seo(String title) {}

    @Test
    void aPrivateFieldIsUnreachableFromATemplate() {
        String rendered = compiler.compile("{{name}}|{{storefrontPassword}}").execute(new Shop());

        assertThat(rendered)
                .withFailMessage("DefaultCollector(false) is what stops a template reading private "
                        + "state of anything reachable from its context. Rendered: %s", rendered)
                .isEqualTo("Acme Tours|");
    }

    /**
     * The other half of turning access coercion off, and it is not in the
     * decision record: with it off, {@code DefaultCollector.getMethod} degrades to
     * a plain {@code clazz.getMethod(name)} — the {@code get<Name>()}/{@code is<Name>()}
     * fallback only exists in the coercion branch. So a view model must expose
     * <b>exactly-named public accessors</b>. Records do; a JavaBean does not, and
     * it fails by rendering an empty page rather than by throwing.
     */
    @Test
    void aJavaBeanGetterIsNotAnAccessor() {
        assertThat(compiler.compile("[{{shopName}}]").execute(new Shop())).isEqualTo("[]");
    }

    @Test
    void aNullPropertyRendersEmptyInsteadOfThrowing() {
        assertThat(compiler.compile("[{{title}}]").execute(new Seo(null))).isEqualTo("[]");
    }

    @Test
    void aSectionOverAnEmptyStringIsOmitted() {
        assertThat(compiler.compile("[{{#title}}x{{/title}}]").execute(new Seo(""))).isEqualTo("[]");
    }
}
