package com.vointika.storefront.infrastructure.config;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Mustache compiler the storefront will render through.
 *
 * <p>This replaces the one {@code MustacheAutoConfiguration} declares (both its
 * beans are {@code @ConditionalOnMissingBean}); the autoconfigured
 * {@code MustacheResourceTemplateLoader} is kept — it is exactly the
 * {@code classpath:/templates/*.mustache} loader this slice wants.
 *
 * <p><b>Nothing compiles a template today</b>: the storefront answers JSON while
 * its pages are a placeholder, so this bean and the settings on it are here for
 * the themes rather than for a caller. That is a deliberate exception to "no
 * abstraction before its second caller" and it is cheap to justify — each
 * setting below was arrived at by reverting it and watching what broke, the
 * findings are recorded as version traps in {@code STACK.md}, and
 * {@code StorefrontMustacheConfigTest} is what keeps them true rather than
 * something to re-derive when a template returns.
 */
@Configuration
public class StorefrontMustacheConfig {

    /**
     * @param loader the autoconfigured classpath loader
     * @return a compiler that is safe against a template we did not write, and
     *         lenient about the fields an operator has not filled in
     */
    @Bean
    public Mustache.Compiler mustacheCompiler(Mustache.TemplateLoader loader) {
        return Mustache.compiler()
                .withLoader(loader)
                // jmustache's stock collector calls setAccessible(true) on declared
                // methods AND fields, so a template could read private state of
                // anything reachable from the context. false = public members only.
                .withCollector(new DefaultCollector(false))
                // The stock compiler THROWS on a null or missing variable. Shop SEO
                // and both media keys are nullable, so without this an operator who
                // has not filled in SEO gets a 500 instead of a page.
                .defaultValue("")
                // So a section guarding an optional tag omits it for a blank value,
                // not only for a null one.
                .emptyStringIsFalse(true);
    }
}
