package com.vointika.shared.port;

/**
 * Does a handle address a real tenant? The storefront's whole read surface,
 * reduced to the one question a placeholder still has to answer.
 *
 * <p>Implemented by {@code touroperator}, which owns the row. It replaced
 * {@code StorefrontShopQuery} and {@code StorefrontExperienceQuery} when the
 * storefront was cut back to a placeholder: those carried the shop, its brand,
 * its policies and its published experiences, and nothing renders any of that
 * today. They are recoverable from git when the real pages return; keeping them
 * alive against no reader is how a seam rots.
 *
 * <p>It answers a boolean rather than a view <b>because that is the entire
 * requirement</b> — the placeholder either serves or 404s. A tenant id nothing
 * reads would be one more thing to keep true (LAW §2.4).
 */
public interface StorefrontTenantQuery {

    boolean exists(String handle);
}
