-- The slot<->pickup relationship is deliberately UNWIRED while its model is
-- redesigned (materialized synced snapshots vs default-in + exclusions). V5's
-- table was never populated by a released feature; dropped rather than edited
-- (an applied migration is immutable). Pickup locations remain a standalone
-- catalog; the relationship returns as a new migration once the model lands.

DROP TABLE experience.pickup_location_slot;
