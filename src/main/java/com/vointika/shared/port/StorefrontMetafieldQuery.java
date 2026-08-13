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
     */
    List<MetafieldView> findForOperator(UUID tourOperatorId);

    /**
     * @param type      the type <b>code</b> — our vocabulary, not Shopify's
     *                  ({@code single_line_text}, never
     *                  {@code single_line_text_field}), decided when the context
     *                  shipped. It crosses as a string because the enum belongs
     *                  to {@code metafield}.
     * @param value     the stored text. For {@code metaobject_reference} it is
     *                  the entry id, and {@code metaobject} carries what it
     *                  points at; presentation serves one or the other, never
     *                  both.
     * @param metaobject the resolved target, <b>null on every other type</b>
     */
    record MetafieldView(String namespace,
                         String key,
                         String type,
                         String value,
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

    record MetaobjectFieldView(String key, String type, String value) {}
}
