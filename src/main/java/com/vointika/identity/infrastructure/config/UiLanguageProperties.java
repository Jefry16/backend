package com.vointika.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Admin-UI languages a user may set as their profile preference — lowercase
 * Paraglide locale codes, matching the frontend's message catalogs.
 *
 * <p>Growing a language needs no schema change, but it is <b>not</b> yml-only:
 * transactional email ships one template pair per (type, locale) on the
 * classpath, and a language with no templates does not fail — the send falls
 * back to English, so the user silently gets the wrong language. Add the code
 * here, ship the frontend catalog, <b>and</b> add the templates plus the code to
 * {@code ClasspathTemplateCatalog.LOCALES}.
 * {@code TemplateLocalesTrackUiLanguagesTest} fails the build if they diverge.
 */
@ConfigurationProperties(prefix = "app.identity")
public record UiLanguageProperties(List<String> uiLanguages) {}
