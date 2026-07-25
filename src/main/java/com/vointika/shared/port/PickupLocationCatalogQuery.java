package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * Cross-context read seam: an operator's full pickup-location catalog, for the
 * experience context to snapshot onto slots at creation. Implemented by the
 * pickup context. Empty list = the operator offers no pickup (not an error).
 */
public interface PickupLocationCatalogQuery {

    List<PickupLocationView> findAllForTourOperator(UUID tourOperatorId);
}
