package com.vointika.touroperator.infrastructure.persistence;

import com.vointika.touroperator.domain.enums.PolicyType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * {@link PolicyType} against the {@code tour_operator_policies} CHECK, the third
 * pair in this context that has to agree and the first whose column is called
 * {@code type} — which is why {@link MigrationCheckConstraints} matches the
 * column name on a boundary: {@code link_type IN (…)} in the menus migration
 * reads as this constraint otherwise, and the test would pin the wrong list.
 *
 * <p>The failure this prevents is worse here than for the brand, because the
 * type is also the <em>address</em>: a value in the database the enum does not
 * have fails the read on {@code /policies/…}, a page an anonymous visitor is
 * looking at.
 */
class PolicyTypeMatchesTheCheckConstraintTest {

    @Test
    void everyPolicyTypeIsAcceptedByTheCheckConstraint() throws IOException {
        MigrationCheckConstraints.assertEnumMatches("type", PolicyType.class, "tour_operator_policies");
    }
}
