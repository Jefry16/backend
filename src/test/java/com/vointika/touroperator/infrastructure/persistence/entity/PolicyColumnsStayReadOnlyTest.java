package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.entity.PolicyTranslation;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy columns are writable <b>exactly while</b> the domain can carry the
 * value — the biconditional {@code BrandColumnsStayReadOnlyTest} uses, now that
 * policies have a write path.
 *
 * <p>This test used to assert the opposite: every column read-only, because
 * nothing wrote a policy. That was the right guard while the storefront rendered
 * documents no endpoint could author, and its own failure message said the write
 * -path slice would take the flags off. This is that slice, so the assertion
 * inverts rather than the test being deleted — the hazard it guards has not gone
 * away, it has only changed direction:
 *
 * <ul>
 *   <li><b>A column writable with no domain field</b> is nulled by every save
 *       that does not mention it, because the mapper rebuilds the row from a
 *       domain object that cannot hold the value.</li>
 *   <li><b>A column read-only while the domain does carry it</b> accepts the
 *       write at the use case, passes every test that stops there, and silently
 *       drops the column from the UPDATE.</li>
 * </ul>
 *
 * <p>Key columns are excluded: a primary key is written by definition, and
 * {@code createdAt} is deliberately {@code updatable = false} so that rewriting
 * a policy keeps the date it was first written.
 */
class PolicyColumnsStayReadOnlyTest {

    @Test
    void policyColumnsAreWritableExactlyWhileTheDomainCarriesThem() {
        assertMapping(TourOperatorPolicyJpaEntity.class, Policy.class);
        assertMapping(TourOperatorPolicyTranslationJpaEntity.class, PolicyTranslation.class);
    }

    private static void assertMapping(Class<?> entity, Class<?> domain) {
        for (Field field : persistentColumns(entity)) {
            Column column = field.getAnnotation(Column.class);
            boolean domainCarriesIt = domainHasAccessor(domain, field.getName());
            boolean writable = column.insertable() || column.updatable();

            assertThat(writable)
                    .withFailMessage(
                            "%s.%s is %s, but the domain %s %s an accessor for it. Change both "
                                    + "halves together: writable with no domain field means every save "
                                    + "nulls the column; read-only with one means the write is silently "
                                    + "dropped.",
                            entity.getSimpleName(), field.getName(),
                            writable ? "writable" : "read-only",
                            domain.getSimpleName(),
                            domainCarriesIt ? "has" : "does not have")
                    .isEqualTo(domainCarriesIt);
        }
    }

    /**
     * Mapped, non-key fields, minus {@code createdAt} — the key is written by
     * definition, and createdAt's immutability is a decision of its own rather
     * than a statement about the domain.
     */
    private static List<Field> persistentColumns(Class<?> entity) {
        List<Field> fields = Arrays.stream(entity.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> !f.getName().equals("createdAt"))
                .toList();
        assertThat(fields)
                .withFailMessage("%s has no non-key @Column fields, so this test cannot see the mapping "
                        + "it exists to track. Fix the test, do not delete it.", entity.getSimpleName())
                .isNotEmpty();
        return fields;
    }

    private static boolean domainHasAccessor(Class<?> domain, String property) {
        String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return Arrays.stream(domain.getMethods())
                .anyMatch(m -> m.getParameterCount() == 0
                        && (m.getName().equals(getter) || m.getName().equals(property)));
    }
}
