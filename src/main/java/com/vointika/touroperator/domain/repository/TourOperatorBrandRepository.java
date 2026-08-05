package com.vointika.touroperator.domain.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The operator's brand row, as the logo use cases see it.
 *
 * <p><b>It exposes one field, and that is the whole point.</b> V10 moved
 * {@code logo_media_id} off {@code tour_operators} onto the brand, so the admin
 * write path that already owned the logo followed its column here; the brand's
 * other fields have no write path yet and no method here either, which is what
 * keeps {@code TourOperatorBrandJpaEntity}'s read-only flags honest
 * (BrandColumnsStayReadOnlyTest reads this interface to decide what may be
 * writable).
 *
 * <p>There is deliberately no {@code Brand} domain entity behind it: nothing
 * validates or invariants a brand yet, so an aggregate would be structure ahead
 * of need (LAW §2.4).
 */
public interface TourOperatorBrandRepository {

    /**
     * The operator's current logo. Empty covers both "no logo set" and "no brand
     * row yet" — indistinguishable to every caller, and neither is an error.
     */
    Optional<UUID> findLogoMediaId(UUID tourOperatorId);

    /**
     * Points the logo at a media id, or clears it with {@code null}. Creates the
     * brand row if the operator has none: every operator alive at V10 was
     * backfilled one, but an operator created since has not been, and a logo
     * has to be settable on it.
     */
    void setLogoMediaId(UUID tourOperatorId, UUID mediaId);
}
