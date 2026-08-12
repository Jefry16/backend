package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * The operator's navigation, for the storefront. Implemented in
 * {@code touroperator}, which owns menus because one storefront per operator
 * makes the operator the scope.
 *
 * <p><b>Items come back flat, with their parent ids.</b> The tree is built by the
 * caller, after it has resolved each item's target and dropped the ones that no
 * longer point anywhere — nesting first would mean unpicking a tree afterwards.
 *
 * <p><b>A target is an id here, never a URL.</b> An {@code EXPERIENCE} or
 * {@code PAGE} item stores what it points at, not where that lives; the handle
 * is that context's to give, in the rendered locale, and the URL is the
 * storefront's to build.
 */
public interface StorefrontMenuQuery {

    /**
     * @param locale the locale already chosen by the locale rule — item titles
     *               overlay nullable-wins-canonical against it
     */
    List<MenuView> findMenus(UUID tourOperatorId, String locale);

    /**
     * @param handle the address a theme reaches this menu by
     *               ({@code linklists["main-menu"]}), not a URL
     */
    record MenuView(String handle, String title, List<MenuItemView> items) {}

    /**
     * @param linkType one of {@code HOME}, {@code EXPERIENCE_LIST},
     *                 {@code EXPERIENCE}, {@code PAGE}, {@code EXTERNAL_URL} —
     *                 crossing as a string because the enum belongs to
     *                 {@code touroperator}
     * @param resourceId what an {@code EXPERIENCE} or {@code PAGE} item points
     *                   at; null for the rest
     * @param url        what an {@code EXTERNAL_URL} item points at; null for the
     *                   rest, and http/https by both a domain guard and a CHECK
     */
    record MenuItemView(UUID id,
                        UUID parentId,
                        String title,
                        String linkType,
                        UUID resourceId,
                        String url,
                        int position) {}
}
