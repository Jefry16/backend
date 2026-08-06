package com.vointika.storefront.application.dto.output;

/**
 * One of the operator's legal documents as every page's footer needs it —
 * <b>no body</b>, because a footer wants four {@code <a>} tags and carrying four
 * HTML documents to build them is the wrong trade.
 *
 * @param type the {@code PolicyType} name, which a theme can switch on
 * @param slug where the policy lives, <b>not its URL</b>: application says where
 *             a thing is and presentation says what its address is, so the
 *             locale prefix is added on the other side of the seam (PATTERNS §2a)
 */
public record PolicyData(String type, String title, String slug) {}
