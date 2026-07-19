package com.vointika.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Admin-UI languages a user may set as their profile preference — lowercase
 * Paraglide locale codes, matching the frontend's message catalogs. Growing a
 * language is configuration only: add the code to {@code
 * app.identity.ui-languages} and ship the frontend catalog; no schema or code
 * change.
 */
@ConfigurationProperties(prefix = "app.identity")
public record UiLanguageProperties(List<String> uiLanguages) {}
