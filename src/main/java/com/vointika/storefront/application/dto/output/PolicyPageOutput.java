package com.vointika.storefront.application.dto.output;

/**
 * The envelope every page gets, plus the document this page exists to show.
 *
 * <p>The envelope's page title is the policy's, not the shop's — see
 * {@link StorefrontPageData#withPageTitle(String)}.
 *
 * @param policy the same rows the footer's {@code shop.policies} lists, read one
 *               level deeper: this is the only route that asks for a body
 */
public record PolicyPageOutput(StorefrontPageData envelope, PolicyDocument policy) {

    /**
     * @param body <b>raw HTML the operator wrote</b>, carried verbatim and
     *             rendered unescaped. See {@code storefront/policy.mustache} for
     *             why that is the feature rather than a hole.
     */
    public record PolicyDocument(String type, String title, String body) {}
}
