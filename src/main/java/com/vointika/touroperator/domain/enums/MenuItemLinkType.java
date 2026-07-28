package com.vointika.touroperator.domain.enums;

/**
 * Typed link targets a menu item can point at. Designed to grow additively
 * (CART once the cart exists, METAOBJECT once the storefront renders one)
 * without migration beyond widening the DB CHECK — the eventual render side
 * treats unknown values as unresolvable, never as an error.
 */
public enum MenuItemLinkType {
    /** The storefront home page. No payload. */
    HOME,
    /** The experiences catalogue page. No payload. */
    EXPERIENCE_LIST,
    /** One experience; requires {@code resourceId}. */
    EXPERIENCE,
    /** A CMS content page; requires {@code resourceId}. */
    PAGE,
    /** A verbatim external URL; requires {@code url}. */
    EXTERNAL_URL
}
