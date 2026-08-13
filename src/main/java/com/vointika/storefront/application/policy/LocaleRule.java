package com.vointika.storefront.application.policy;

import java.util.Optional;
import java.util.Set;

/**
 * Which locale an address renders in — a pure function, so it is testable
 * without a request.
 *
 * <p><b>The primary locale lives at the bare path and nowhere else.</b>
 * {@code /} renders it, {@code /{primary}} is a <b>404</b> rather than a second
 * address for the same page, and a supported secondary renders at its prefix.
 * Two URLs for one page is duplicate content, and the canonical tag that would
 * paper over it is a worse answer than not minting the address.
 *
 * <p>The comparison is <b>exact</b>: {@code /ES} is not a second address for
 * {@code /es}. The route pattern only matches lowercase anyway, so an uppercase
 * segment never reaches here — this keeps the rule true on its own rather than
 * relying on that.
 *
 * <p><b>Every rejection is the same empty</b>, and that is deliberate: an
 * unsupported locale and a locale the operator has not published are
 * indistinguishable to a visitor, so neither tells them anything about the operator.
 */
public final class LocaleRule {

    private LocaleRule() {
    }

    /**
     * @param pathLocale the locale segment of the URL, or null for the bare path
     * @return the locale to render in, or empty when the address does not exist
     */
    public static Optional<String> resolve(String pathLocale, String primaryLocale, Set<String> supportedLocales) {
        if (pathLocale == null) {
            return Optional.of(primaryLocale);
        }
        if (pathLocale.equals(primaryLocale)) {
            return Optional.empty();
        }
        return supportedLocales.contains(pathLocale) ? Optional.of(pathLocale) : Optional.empty();
    }
}
