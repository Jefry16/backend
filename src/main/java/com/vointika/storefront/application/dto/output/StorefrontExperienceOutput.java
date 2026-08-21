package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceDetailView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;

import java.util.List;

/**
 * The globals plus the one experience this route is about — the same pairing
 * {@link StorefrontPageOutput} makes for a CMS page.
 *
 * <p>Metafields ride beside the experience rather than on it: they come from a
 * different context through its own port, and the view crossing
 * {@code StorefrontExperienceQuery} carries only what the {@code experience}
 * schema owns.
 */
public record StorefrontExperienceOutput(StorefrontGlobals globals,
                                         ExperienceDetailView experience,
                                         List<MetafieldView> metafields) {}
