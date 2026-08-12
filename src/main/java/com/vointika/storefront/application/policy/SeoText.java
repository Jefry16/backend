package com.vointika.storefront.application.policy;

/**
 * What goes in the title tag and the meta description, wherever the request
 * landed.
 *
 * <p><b>One chain, in one place.</b> Every page type falls back the same way —
 * its own override, then what it is called, then the shop's — and the reason the
 * shop sits at the end is written into the migration that added those columns
 * (touroperator/V8): the home page has no content object to carry SEO, and every
 * other page type falls back to the shop when its own overrides are empty.
 *
 * <p>It was inlined in the globals use case while the home page was the only
 * caller. The CMS page is the second, which is when it moved here rather than
 * being copied.
 */
public final class SeoText {

    private SeoText() {
    }

    /**
     * @param ownSeoTitle the page's own override, or null
     * @param ownTitle    what the page is called, or null for a page type that
     *                    has no title of its own (the home page)
     */
    public static String title(String ownSeoTitle, String ownTitle, String shopSeoTitle, String shopName) {
        return firstPresent(ownSeoTitle, ownTitle, shopSeoTitle, shopName);
    }

    /**
     * <b>No fallback to a title.</b> A description that repeats the title is
     * worse than none — search engines write their own snippet from the page
     * either way, and a duplicated line is what they discard.
     */
    public static String description(String ownSeoDescription, String shopSeoDescription) {
        return firstPresent(ownSeoDescription, shopSeoDescription);
    }

    private static String firstPresent(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
