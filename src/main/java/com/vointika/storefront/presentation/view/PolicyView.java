package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.PolicyPageOutput;
import com.vointika.storefront.application.policy.PolicySlug;

/**
 * The Mustache context object for a policy page: the four global objects every
 * page gets, plus the document this one shows.
 *
 * <p><b>Must stay public, and so must {@link Document}</b> — see {@link HomeView},
 * which also records why the four components are repeated rather than nested.
 *
 * <p>Nothing is resolved here that the listing resolves — a policy has no media
 * and no handle. The page's <em>title</em> is the policy's, and that happens in
 * the use case: it is a property of the envelope, not of this record.
 */
public record PolicyView(
        Shop shop,
        Page page,
        Routes routes,
        Localization localization,
        Document policy
) {

    /**
     * @param body <b>rendered unescaped</b> — see {@code storefront/policy.mustache}
     *             for the trust boundary that makes that the feature rather than
     *             a hole. It is HTML the operator wrote about their own
     *             storefront, stored verbatim.
     */
    public record Document(String type, String title, String body) {}

    /**
     * @param pathLocale the locale prefix the request arrived under, or
     *                   {@code null} for the unprefixed policy. <b>Not the
     *                   rendered locale</b> — see {@link Routes}.
     */
    public static PolicyView from(PolicyPageOutput page, String pathLocale,
                                  MediaUrlResolver mediaUrlResolver,
                                  String origin, String path) {
        Routes routes = Routes.forPathLocale(pathLocale);
        // The switcher points at THIS policy in each language, not at the shop's
        // front door — the same rule the listing follows, one address deeper.
        String slug = PolicySlug.of(page.policy().type());
        return new PolicyView(
                Shop.from(page.envelope().shop(), mediaUrlResolver, origin, routes),
                Page.from(page.envelope().page(), mediaUrlResolver, path),
                routes,
                Localization.from(page.envelope().localization(), language -> language.policy(slug)),
                new Document(page.policy().type(), page.policy().title(), page.policy().body()));
    }
}
