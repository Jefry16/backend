package com.vointika.storefront.application.dto.output;

/**
 * An image in <b>key</b> form: everything a rendered {@code <img>} needs except
 * the URL, which presentation resolves (PATTERNS §5).
 *
 * <p><b>{@code alt}, {@code width} and {@code height} are null on every row
 * today.</b> The media columns exist and nothing populates them yet — the alt
 * write path and the dimension measurement are both their own work. The shape is
 * the contract; the data follows.
 *
 * <p>There is no aspect ratio here: it is derived from the pair rather than
 * stored, so it belongs where the pair is turned into something a template
 * reads.
 */
public record ImageData(String storageKey, String alt, Integer width, Integer height) {}
