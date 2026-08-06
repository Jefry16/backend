package com.vointika.storefront.infrastructure.config;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Mustache compiler the storefront renders through, and the compiled
 * templates it holds.
 *
 * <p>This replaces the one {@code MustacheAutoConfiguration} declares (both its
 * beans are {@code @ConditionalOnMissingBean}); the autoconfigured
 * {@code MustacheResourceTemplateLoader} is kept — it is exactly the
 * {@code classpath:/templates/*.mustache} loader this slice wants.
 *
 * <p>Every setting below has a caller today; none is speculative, and the point
 * of pinning them in a test is that a silent reversion is a security or
 * availability regression rather than a cosmetic one.
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

    /**
     * Compiled once at startup, not per request: Spring's {@code MustacheView}
     * recompiles on every render (the caching view resolver above it caches the
     * <em>View</em>, not the {@code Template}), which is why this slice writes
     * the rendered string itself instead of returning a view name.
     *
     * <p>This one, {@link #storefrontExperienceListTemplate} and
     * {@link #storefrontPolicyTemplate} all inherit from
     * {@code storefront/layout}, which needs no bean of its own: a parent
     * template is resolved through the same loader on <em>first render</em> and
     * then pinned into the compiled {@code Template} for good. So the layout is
     * compiled once per page that uses it, not once per request — and the day
     * templates come from a tenant's theme rather than the classpath, that
     * pinning is what forces the theme version into the cache key.
     */
    @Bean
    public Template storefrontHomeTemplate(Mustache.Compiler compiler) {
        return compiler.loadTemplate("storefront/home");
    }

    @Bean
    public Template storefrontExperienceListTemplate(Mustache.Compiler compiler) {
        return compiler.loadTemplate("storefront/experiences");
    }

    @Bean
    public Template storefrontPolicyTemplate(Mustache.Compiler compiler) {
        return compiler.loadTemplate("storefront/policy");
    }

    @Bean
    public Template storefrontNotFoundTemplate(Mustache.Compiler compiler) {
        return compiler.loadTemplate("storefront/not-found");
    }

    @Bean
    public Template storefrontPasswordTemplate(Mustache.Compiler compiler) {
        return compiler.loadTemplate("storefront/password");
    }
}
