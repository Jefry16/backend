package com.vointika.shared.port;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Where a CMS page lives, for callers holding ids. Implemented in {@code page}.
 *
 * <p>The twin of {@code StorefrontExperienceQuery.findPublishedHandles}, and it
 * exists separately for the reason every seam here does: the context that owns
 * the row is the one that answers for it.
 *
 * <p><b>Published only, and an id missing from the result is the answer</b> — an
 * unpublished or deleted page has no address, so a menu item pointing at one is
 * dropped rather than rendered as a link that 404s.
 */
public interface StorefrontPageQuery {

    Map<UUID, String> findPublishedHandles(UUID tourOperatorId, Set<UUID> pageIds, String locale);

    /**
     * The page at an address, or empty — which is the 404.
     *
     * <p><b>The handle is the one that locale publishes.</b> A page with a
     * Spanish handle is addressed by it under {@code /es}, and its canonical
     * handle is <em>not</em> a second address for the same page there — the same
     * rule that makes {@code /{primary}} a 404 when the primary already lives at
     * {@code /}. Two URLs for one page is duplicate content whether the second
     * comes from a locale prefix or a stale handle.
     *
     * <p>Empty covers every miss the storefront answers 404 for and does not
     * distinguish them: no such handle, a draft, another operator's page, or the
     * canonical handle of a page this locale addresses differently.
     */
    Optional<PageView> findByHandle(UUID tourOperatorId, String handle, String locale);

    /**
     * @param handles this page's address in every locale that renames it, plus the
     *                canonical — the language switcher's input. Without it a
     *                switcher can only repeat the current handle under another
     *                prefix, and a locale that renames the page makes the canonical
     *                a 404 there: an English prefix on a Spanish slug.
     * @param body raw HTML the operator authored. <b>It is rendered unescaped</b>
     *             when a template exists — our template rendering their content,
     *             the same trust boundary the policy page sits on, and a
     *             deliberate exception to the ban on raw output (which is about
     *             templates <em>they</em> wrote).
     */
    record PageView(UUID id,
                    String handle,
                    String title,
                    String body,
                    String seoTitle,
                    String seoDescription,
                    LocalizedHandles handles) {}
}
