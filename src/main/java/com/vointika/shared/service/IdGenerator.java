package com.vointika.shared.service;

import java.util.UUID;

/**
 * Mints identifiers for new aggregates. The default implementation produces
 * UUID v7 (time-ordered epoch) so primary keys sort chronologically and are
 * index-friendly on write-heavy tables.
 *
 * <p>Lives in {@code shared} because every bounded context needs the same ID
 * generation strategy.
 */
public interface IdGenerator {

    UUID newId();
}
