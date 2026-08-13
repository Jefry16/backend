package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * The operator's own metafield values, for the storefront. Implemented in
 * {@code metafield}.
 *
 * <p>Separate from {@link StorefrontTourOperatorQuery} because the values live in
 * another context: {@code touroperator} owns the operator row and implements that
 * port, and it may not read {@code metafield}'s tables. So the globals are
 * assembled from two seams rather than one — the alternative would be a context
 * reaching across a boundary to make one query cheaper.
 *
 * <p><b>Values only, never the definition catalogue.</b> A definition an
 * operator created but never filled in has nothing to render, and shipping the
 * empty shape would tell a visitor what the operator plans to say.
 */
public interface StorefrontMetafieldQuery {

    /**
     * Ordered by namespace then key, so the rendered payload does not reshuffle
     * between requests.
     *
     * <p><b>A reference whose target cannot be shown is omitted entirely</b> —
     * unpublished, deleted, or belonging to another operator. The same rule the
     * menus follow: a link to nothing is pruned rather than served broken. The
     * alternative is a bare id in the payload, which is what this read shipped
     * with until it was fixed, and a theme could do nothing with it.
     *
     * <p><b>Values are typed here, not by the caller.</b> {@code boolean} arrives
     * as a {@link Boolean} and {@code json} as the parsed value; every other type
     * stays a {@code String}. The typing belongs to this side of the boundary
     * because only {@code metafield} can see {@code MetafieldType} — deciding it
     * in {@code storefront} would mean matching on the literals
     * {@code "boolean"} and {@code "json"}, a second copy of an enum it is fenced
     * from and nothing to keep the copies equal. Same reason the brand palette's
     * role split happens in {@code touroperator} (PATTERNS §2a).
     *
     * <p><b>Numbers stay strings on purpose.</b> {@code number_integer}
     * normalizes through {@code Long}, so it reaches 9.2e18 — past JavaScript's
     * exact-integer ceiling of 2^53, where a JSON number quietly loses digits —
     * and {@code number_decimal} allows 38 digits of precision. The call
     * {@code startingPrice} already made, for the same reason. {@code date} stays
     * a string too: JSON has no date type and the stored form is ISO-8601
     * already.
     *
     * <p><b>Values are overlaid for {@code locale} before they are typed.</b> A
     * value with no translation in that locale falls back to the canonical one,
     * per key rather than per resource — so an operator who has translated two of
     * five metafields gets two translated and three canonical, not all five in
     * one language or the other. Only text types can carry a translation at all.
     *
     * <p>Only JDK types cross ({@code Map}, {@code List}, {@code String},
     * {@code Number}, {@code Boolean}, null). <b>No parser type may appear in
     * this record</b> — {@code shared} is imported by every context, and a
     * Jackson node here would put a JSON library on all of them.
     */
    List<MetafieldView> findForOperator(UUID tourOperatorId, String locale);

    /**
     * @param type      the type <b>code</b> — our vocabulary, not Shopify's
     *                  ({@code single_line_text}, never
     *                  {@code single_line_text_field}), decided when the context
     *                  shipped. It crosses as a string because the enum belongs
     *                  to {@code metafield}.
     * @param value     the stored value, <b>typed by the owning context</b> —
     *                  see {@link #findForOperator}. For
     *                  {@code metaobject_reference} it is the entry id, and
     *                  {@code metaobject} carries what it points at;
     *                  presentation serves one or the other, never both.
     * @param metaobject the resolved target, <b>null on every other type</b>
     */
    record MetafieldView(String namespace,
                         String key,
                         String type,
                         Object value,
                         MetaobjectView metaobject) {}

    /**
     * One published metaobject entry, flattened to primitives.
     *
     * <p><b>Fields are nested rather than top-level, which is where we depart
     * from Shopify.</b> Theirs are accessors on the drop itself
     * ({@code boat.capacity.value}), which is why they need a {@code system}
     * object — their own docs say it exists "to avoid collisions between system
     * property names and user-defined metaobject fields". Nesting under
     * {@code fields} makes a collision impossible, so {@code system} buys
     * nothing and is not copied.
     *
     * @param fields only the fields that <b>have</b> a value, in the definition's
     *               field order. An unset field has no row, and inventing an
     *               empty one would tell a visitor what the operator has not
     *               written yet.
     */
    record MetaobjectView(UUID id,
                          String type,
                          String handle,
                          String name,
                          List<MetaobjectFieldView> fields) {}

    /** Typed exactly as {@link MetafieldView#value} is — the same catalogue. */
    record MetaobjectFieldView(String key, String type, Object value) {}
}
