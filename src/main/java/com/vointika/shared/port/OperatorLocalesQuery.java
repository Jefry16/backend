package com.vointika.shared.port;

import java.util.Set;
import java.util.UUID;

/**
 * Cross-context read of an operator's supported content locales — the set a
 * consumer validates a locale against (e.g. experience translations reject a
 * locale the operator hasn't enabled). Returns the codes (lowercase); empty if
 * the operator doesn't exist. Implemented in {@code touroperator}.
 */
public interface OperatorLocalesQuery {

    Set<String> findSupportedLocales(UUID tourOperatorId);
}
