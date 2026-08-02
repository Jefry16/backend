package com.vointika.touroperator.presentation.request;

import java.util.UUID;

/**
 * The operator's canonical SEO defaults. A whole-value replace: a null field
 * clears that override rather than leaving it untouched.
 */
public record UpdateOperatorSeoRequest(String seoTitle, String seoDescription, UUID ogImageMediaId) {
}
