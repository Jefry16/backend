package com.vointika.rendering.application.dto.output;

/**
 * The page's resolved SEO block — every field already fallen through its chain,
 * so a theme renders it directly and never re-implements the precedence.
 *
 * <p>Any field may still be null: an operator who has set nothing and whose
 * content carries nothing has no description to give, and inventing one would be
 * worse than omitting it.
 */
public record Seo(String title, String description, String imageUrl) {}
