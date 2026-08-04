package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.entity.TourOperator;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code phone} and {@code email} are mapped read-only, and this pins the reason
 * rather than the setting — because both ways of getting it wrong are silent.
 *
 * <p>The invariant is a biconditional: <b>the column is mapped read-only exactly
 * when the domain cannot carry its value.</b> Every operator write goes
 * {@code TourOperatorMapper.toJpa} → {@code save}, rebuilding this entity from a
 * domain {@code TourOperator}. So:
 *
 * <ul>
 *   <li><b>Flags removed while the domain still has no field</b> — every unrelated
 *       operator save (a logo, a locale set, an SEO edit) writes the null the
 *       mapper had to invent, and the columns are wiped by a request that never
 *       mentioned them. Verified against the running database: with the flags off,
 *       one {@code PUT /seo} turned a seeded {@code +34 910 000 000} into
 *       {@code null}.</li>
 *   <li><b>The domain gains a field while the flags stay on</b> — the write half
 *       lands, every test that stops at the use case passes, and the UPDATE
 *       silently omits the column. The operator saves a phone number and it never
 *       reaches Postgres.</li>
 * </ul>
 *
 * <p>So the slice that adds the write path changes both halves together, and this
 * test is what says so at the moment it changes one.
 */
class ContactColumnsStayReadOnlyTest {

    @Test
    void contactColumnsAreReadOnlyForExactlyAsLongAsTheDomainCannotCarryThem() {
        for (String property : new String[]{"phone", "email"}) {
            boolean domainCarriesIt = hasAccessor(property);
            Column column = mapping(property);

            assertThat(column.insertable() || column.updatable())
                    .withFailMessage(
                            "TourOperatorJpaEntity.%s is %s, but the domain TourOperator %s "
                                    + "an accessor for it.%n"
                                    + "  writable + domain has no field  -> TourOperatorMapper.toJpa "
                                    + "invents a null and every unrelated operator save wipes the "
                                    + "column%n"
                                    + "  read-only + domain has a field  -> the write path silently "
                                    + "no-ops; the UPDATE omits the column%n"
                                    + "Change both halves together.",
                            property,
                            column.insertable() || column.updatable() ? "writable" : "read-only",
                            domainCarriesIt ? "has" : "does not have")
                    .isEqualTo(domainCarriesIt);
        }
    }

    private static Column mapping(String property) {
        try {
            Field field = TourOperatorJpaEntity.class.getDeclaredField(property);
            Column column = field.getAnnotation(Column.class);
            assertThat(column)
                    .withFailMessage("TourOperatorJpaEntity.%s has no @Column, so this test cannot "
                            + "see the mapping it exists to track. Fix the test, do not delete it.",
                            property)
                    .isNotNull();
            return column;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(
                    "TourOperatorJpaEntity has no '" + property + "' field. If it was renamed, rename "
                            + "it here too; if it was removed, remove this expectation deliberately.", e);
        }
    }

    /** Any zero-arg accessor naming the property — {@code getPhone()} or {@code phone()}. */
    private static boolean hasAccessor(String property) {
        String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return Arrays.stream(TourOperator.class.getMethods())
                .anyMatch(m -> m.getParameterCount() == 0
                        && (m.getName().equals(getter) || m.getName().equals(property)));
    }
}
