package com.vointika.rendering.application.dto.output;

import java.util.List;

/**
 * A menu item with its target resolved to a handle — still not a URL, because
 * URL shape belongs to the BFF that renders it.
 *
 * @param handle      the experience handle or page handle for this locale; null
 *                    for HOME, EXPERIENCE_LIST and EXTERNAL_URL
 * @param externalUrl set only for EXTERNAL_URL
 */
public record NavigationItem(
        String title,
        String linkType,
        String handle,
        String externalUrl,
        List<NavigationItem> children) {}
