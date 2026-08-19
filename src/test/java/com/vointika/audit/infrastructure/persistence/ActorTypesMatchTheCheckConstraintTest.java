package com.vointika.audit.infrastructure.persistence;

import com.vointika.architecture.MigrationCheckConstraints;
import com.vointika.shared.valueobject.AuditActorType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * The {@link AuditActorType} enum and the {@code audit_log_actor_type_check}
 * constraint are two lists that must agree, and nothing used to make them.
 *
 * <p>The enum's own javadoc promises that adding {@code STOREFRONT} is "a one-line
 * CHECK-constraint migration" — a promise enforced by nothing. Add the value and
 * forget the migration and the constraint rejects the insert with SQLSTATE 23514,
 * which {@code SpringTransactionRunner} does not translate (it translates only
 * 23505). So it surfaces as a 500 — and because {@code AuditTrailPortImpl} appends
 * inside the caller's transaction on purpose, <b>the business action it was
 * recording rolls back with it</b>. The first shopper checkout would fail, not the
 * build.
 *
 * <p>Verified against the running database before this test was written: inserting
 * {@code actor_type = 'STOREFRONT'} today fails with
 * {@code violates check constraint "audit_log_actor_type_check"}.
 *
 * <p><b>This carried its own copy of the migration parser until 2026-08-19</b> —
 * the version-ordered read, the last-constraint-wins rule, the {@code V10}-sorts-before-
 * {@code V1} trap and its refusal to guess at an unknown filename, all duplicated from
 * {@link MigrationCheckConstraints}, which three other contexts already used. Worse than
 * the duplication: the copy asserted <em>one way</em> (every enum value is in the CHECK)
 * where the shared helper asserts both. A value in the CHECK and not in the enum passed
 * here, and it is the direction that fails a <em>read</em> — the column is
 * {@code @Enumerated(STRING)}, so such a row breaks on the way out, not on the way in.
 */
class ActorTypesMatchTheCheckConstraintTest {

    @Test
    void everyActorTypeIsAcceptedByTheCheckConstraintAndViceVersa() throws IOException {
        MigrationCheckConstraints.assertEnumMatches(
                "audit", "actor_type", AuditActorType.class, "audit_log");
    }
}
