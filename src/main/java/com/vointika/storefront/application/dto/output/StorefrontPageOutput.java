package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.StorefrontPageQuery.PageView;

/**
 * A CMS page: the globals every address carries, plus the one object this route
 * is associated with.
 *
 * <p><b>It wraps the globals rather than flattening them</b> (PATTERNS §2a). The
 * home page returns the envelope directly because it has no object of its own;
 * every other page type is this shape, and the wrapper is what keeps the globals
 * identical across all of them.
 */
public record StorefrontPageOutput(StorefrontGlobals globals, PageView page) {}
