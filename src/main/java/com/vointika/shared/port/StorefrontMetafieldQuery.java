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
     */
    List<MetafieldView> findForOperator(UUID tourOperatorId);

    /**
     * @param type the type <b>code</b> — our vocabulary, not Shopify's
     *             ({@code single_line_text}, never {@code single_line_text_field}),
     *             decided when the context shipped. It crosses as a string
     *             because the enum belongs to {@code metafield}.
     */
    record MetafieldView(String namespace, String key, String type, String value) {}
}
