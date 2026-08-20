package com.vointika.touroperator.infrastructure.persistence;

import com.vointika.architecture.MigrationCheckConstraints;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * The fifth enum/CHECK pair, and the last one that had no test.
 *
 * <p>{@code InvitationStatus} is mapped {@code @Enumerated(STRING)} at
 * {@code TourOperatorInvitationJpaEntity:41} against
 * {@code tour_operator_invitations.status}, so the coupling runs both ways and both
 * directions are silent until they are not: a value only the enum has fails the insert
 * with SQLSTATE 23514, which nothing translates; a value only the CHECK has fails the
 * <em>read</em>, when someone opens the team page.
 *
 * <p>The two agree exactly today and were held there by nothing. Adding it here rather
 * than leaving it for `touroperator`'s next pass because that pass has already run —
 * a gap no future pass owns is one that stays open.
 */
class InvitationStatusMatchesTheCheckConstraintTest {

    @Test
    void everyInvitationStatusIsAcceptedByTheCheckConstraintAndViceVersa() throws IOException {
        MigrationCheckConstraints.assertEnumMatches(
                "touroperator", "status", InvitationStatus.class, "tour_operator_invitations");
    }
}
