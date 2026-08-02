package com.vointika.touroperator.application.dto.output;

import java.util.UUID;

/**
 * The operator's canonical (untranslated) SEO defaults. {@code ogImageMediaId} is
 * the stored reference; the URL is resolved at read time by whoever renders it.
 */
public record OperatorSeoView(String seoTitle, String seoDescription, UUID ogImageMediaId) {
}
