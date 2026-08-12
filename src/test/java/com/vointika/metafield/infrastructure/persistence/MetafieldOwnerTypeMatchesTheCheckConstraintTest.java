package com.vointika.metafield.infrastructure.persistence;

import com.vointika.architecture.MigrationCheckConstraints;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * {@link MetafieldOwnerType} against the {@code metafield_definitions} CHECK.
 *
 * <p>The coupling runs both ways and neither direction fails loudly on its own:
 * a value only the enum has fails every insert with SQLSTATE 23514, which
 * nothing translates; a value only the CHECK has fails the <em>read</em>,
 * because the column is mapped {@code @Enumerated(STRING)}.
 *
 * <p>This is the pair most likely to drift, because growing the owner list is
 * meant to be routine — {@code TOUR_OPERATOR} was the third, and the enum, the
 * CHECK, the access dispatch and a controller all had to move together.
 */
class MetafieldOwnerTypeMatchesTheCheckConstraintTest {

    @Test
    void everyOwnerTypeIsAcceptedByTheCheckConstraint() throws IOException {
        MigrationCheckConstraints.assertEnumMatches("metafield", "owner_type",
                MetafieldOwnerType.class, "metafield_definitions");
    }
}
