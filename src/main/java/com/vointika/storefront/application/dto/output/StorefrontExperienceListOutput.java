package com.vointika.storefront.application.dto.output;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;

/**
 * The globals plus the listing's own object — the same pairing
 * {@link StorefrontPageOutput} makes for a CMS page.
 *
 * <p>The page crosses as a {@link CursorPage} and not a bare list, because
 * {@code nextCursor} is the listing's, not a card's: presentation turns the pair
 * into {@code experiences: {data, nextCursor}}.
 */
public record StorefrontExperienceListOutput(StorefrontGlobals globals,
                                             CursorPage<ExperienceCardView> experiences) {}
