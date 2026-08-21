package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@code ListSchema} declares {@code .tenantScoped()}.
 *
 * <p><b>Omitting it is not an error, it is {@code false}.</b>
 * {@code CriteriaListExecutor} adds the tenant predicate only
 * {@code if (schema.tenantScoped())}, and the builder's field is a plain boolean
 * with no default to override — so a schema that never calls it compiles, runs,
 * and returns every operator's rows. The whole suite stays green: deleting the
 * line from the storefront listing left <b>1415 tests, 0 failures</b>, and so did
 * deleting it from {@code ListPagesUseCase}. Measured, both.
 *
 * <p><b>Why a guard now rather than when the convention was established.</b> The
 * sixteen admin lists sit behind a JWT and the membership interceptor, so the
 * same omission there is a cross-tenant leak between authenticated members —
 * serious, and bounded by who can get a token. The seventeenth is the storefront
 * experiences listing, which is unauthenticated: the identical one-line omission
 * serves every operator's catalogue to anyone with the URL. The convention did not
 * get weaker, the blast radius did.
 *
 * <p><b>Asserting the tenant id on the executed query does not catch this</b>, and
 * that is the trap worth knowing. {@code StorefrontExperienceQueryImplTest} does
 * exactly that in {@code theTenantIsTheHostsOperatorAndNotTheCallers} — and passes
 * with the line deleted, because the executor there is a mock. The
 * {@code ListQuery} carries the right tenant either way; whether anything *uses*
 * it is the schema flag, and no mocked executor exercises a flag it never reads.
 * The fact lives in the builder chain, so this reads the builder chain.
 *
 * <p>There is deliberately no exemption list. Nothing in the repository is
 * legitimately un-scoped today, and an empty one invites the first person with an
 * awkward case to add themselves to it rather than argue for it. A genuine
 * platform-wide list should fail this test and be discussed.
 */
class EveryListSchemaIsTenantScopedTest {

    @Test
    void noListCanBeBuiltWithoutTenantScoping() throws IOException {
        ListSchemaScanner.Scan scan = ListSchemaScanner.scan();

        // A walk that found nothing would pass every assertion below. The count is
        // not pinned — a new list must not have to edit this test — but zero, or a
        // handful where there are seventeen, means the scan broke rather than that
        // the repository shrank.
        assertThat(scan.declaresASchema())
                .withFailMessage("""
                        Found no ListSchema declarations at all, so this guard checked nothing. \
                        The scan is broken — fix it rather than deleting the test.""")
                .isNotEmpty();

        Set<String> unscoped = new TreeSet<>();
        for (String declaring : scan.declaresASchema()) {
            String chain = ListSchemaScanner.schemaBlock(scan.sources().get(declaring));
            if (!chain.contains(".tenantScoped()")) {
                unscoped.add(declaring);
            }
        }

        assertThat(unscoped)
                .withFailMessage("""
                        %d ListSchema(s) do not declare .tenantScoped():
                        %s
                        CriteriaListExecutor adds the tenant predicate only when the schema says to, \
                        and the builder's field is a plain boolean — so this omission is not an \
                        error, it is every operator's rows in one response. On an admin list that \
                        is a cross-tenant leak between members; on a storefront route it is public.

                        If a list genuinely has no tenant — a platform-wide one — that is a \
                        decision to argue for, not an exemption to add quietly. Say so here and in \
                        `OPEN-WORK.md`.""",
                        unscoped.size(), String.join("\\n  - ", unscoped))
                .isEmpty();
    }
}
