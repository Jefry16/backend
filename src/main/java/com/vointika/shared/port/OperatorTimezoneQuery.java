package com.vointika.shared.port;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read seam: the operator's timezone, for interpreting "today" when
 * validating slot dates (a slot's wall-clock local time is judged against the
 * operator's local date). Implemented by the touroperator context. Empty when the
 * operator doesn't exist.
 */
public interface OperatorTimezoneQuery {

    Optional<ZoneId> findZoneId(UUID tourOperatorId);
}
