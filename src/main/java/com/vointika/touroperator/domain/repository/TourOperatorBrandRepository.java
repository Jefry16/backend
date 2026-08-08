package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.Brand;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorBrandRepository {

    /**
     * The operator's brand row, or empty when they have none. Empty is ordinary:
     * every operator alive at V10 was backfilled a row, but one created since has
     * not been, and nothing creates one at operator creation.
     */
    Optional<Brand> findByTourOperatorId(UUID tourOperatorId);

    /**
     * Create-or-replace the whole brand, <b>including both collections</b>, which
     * are deleted and reinserted rather than diffed. Creates the row when the
     * operator has none.
     */
    Brand save(Brand brand);
}
