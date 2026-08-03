package com.vointika.storefront.application.dto.output;

/**
 * The shop itself — what is true of the operator on every page, in <b>key</b>
 * form, because turning a storage key into a URL is presentation's job
 * (PATTERNS §5).
 *
 * <p>Every field here has a column behind it and a renderer in front of it. The
 * timezone has the first and not the second, so it is absent until something
 * renders a time.
 */
public record ShopData(
        String name,
        String address,
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
