-- Audience name is unique per operator case-INSENSITIVELY ("Adults" == "adults")
-- — the app checks this way, and the DB must enforce it so a race can't slip a
-- case-variant duplicate past the pre-check. Swap the exact-name unique index for
-- a functional one on lower(name).

DROP INDEX audience.audiences_operator_name_unique;

CREATE UNIQUE INDEX audiences_operator_name_unique
    ON audience.audiences (tour_operator_id, lower(name));
