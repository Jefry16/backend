package com.vointika.storefront.application.dto.output;

import java.util.UUID;

/**
 * The shop itself — what is true of the operator on every page, in <b>key</b>
 * form, because turning a storage key into a URL is presentation's job
 * (PATTERNS §5).
 *
 * <p><b>The admission rule changed, deliberately.</b> This used to say every
 * field had a column behind it <em>and a renderer in front of it</em>, which is
 * why the timezone was left out. That rule was right for a page and wrong for a
 * contract: {@code shop} is API the day an operator authors a theme, and a field
 * added later is a breaking change while a field added now costs one component
 * of a record we already load. So the rule is now the first half only — <b>expose
 * what the row has, invent nothing</b> — and a field is omitted only when there
 * is no column behind it (social links, a slogan) or when it belongs somewhere
 * else (theme settings, {@code localization}).
 *
 * <p><b>There is no {@code logoKey} any more.</b> It moved into
 * {@link BrandData}, where Shopify keeps it — their shop object has no logo, and
 * a second address for one field is a rename waiting to happen once operators
 * author themes.
 *
 * @param phone the shop's public contact details, <b>nullable and null on every
 *              row today</b> — V9 added the columns and nothing writes them yet,
 *              so the footer's guards are what a real operator will exercise.
 *              They are here rather than waiting because the column now exists:
 *              the rule that kept them out was "expose what the row has", and it
 *              has them.
 */
public record ShopData(
        UUID id,
        String name,
        String address,
        String phone,
        String email,
        String description,
        String passwordMessage,
        BrandData brand,
        CurrencyData currency,
        TimezoneData timezone
) {

    /**
     * The operator's currency, joined from {@code reference}. It is here one
     * slice before the price badge that formats with {@code symbol}, because
     * adding it then would mean widening the query port a second time for a
     * field the row already has.
     */
    public record CurrencyData(String code, String symbol) {}

    /**
     * The operator's IANA zone ({@code Europe/Madrid}) and the city it is named
     * for, joined from {@code reference}. <b>Shopify's {@code shop} has no
     * equivalent</b> — a catalogue of products does not need one. A departure
     * time does: every slot a theme renders is an instant that means nothing
     * without the zone it was scheduled in.
     */
    public record TimezoneData(String name, String city) {}
}
