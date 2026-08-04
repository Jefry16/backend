package com.vointika.storefront.application.dto.output;

/**
 * The shop itself — what is true of the operator on every page, in <b>key</b>
 * form, because turning a storage key into a URL is presentation's job
 * (PATTERNS §5).
 *
 * <p>Every field here has a column behind it and a renderer in front of it. The
 * timezone has the first and not the second, so it is absent until something
 * renders a time.
 *
 * @param phone the shop's public contact details, <b>nullable and null on every
 *              row today</b> — V9 added the columns and nothing writes them yet,
 *              so the footer's guards are what a real operator will exercise.
 *              They are here rather than waiting because the column now exists:
 *              the rule that kept them out was "expose what the row has", and it
 *              has them.
 */
public record ShopData(
        String name,
        String address,
        String phone,
        String email,
        String logoKey,
        CurrencyData currency
) {

    /**
     * The operator's currency, joined from {@code reference}. It is here one
     * slice before the price badge that formats with {@code symbol}, because
     * adding it then would mean widening the query port a second time for a
     * field the row already has.
     */
    public record CurrencyData(String code, String symbol) {}
}
