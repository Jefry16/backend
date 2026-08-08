package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.entity.BrandColor;
import com.vointika.touroperator.domain.entity.BrandSocialLink;
import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Brand columns are writable <b>exactly while</b> a domain object can carry the
 * value — in both directions, on all three tables plus the two translated
 * columns that live on the operator's overlay.
 *
 * <p>This test has now inverted twice, and the hazard is why. It began as
 * "everything but the logo is read-only, because nothing writes it": true while
 * the logo endpoint was the brand's only writer and the repository rebuilt the
 * row from that one value. The brand write path made the domain carry all of it,
 * so the flags came off — and the guard flipped rather than being deleted,
 * because the failure it prevents merely changed direction:
 *
 * <ul>
 *   <li><b>Writable with no domain field</b> — the mapper has nothing to put in
 *       the column, so every save writes a null over it. This is exactly what
 *       setting a logo used to do to the slogan.</li>
 *   <li><b>Read-only with a domain field</b> — the write lands in the use case,
 *       every test that stops there passes, and Hibernate quietly omits the
 *       column from the UPDATE.</li>
 * </ul>
 *
 * <p>Key columns are excluded (a primary key is written by definition), as is
 * {@code createdAt}, whose immutability is a decision of its own rather than a
 * statement about the domain.
 */
class BrandColumnsStayReadOnlyTest {

    /** Each JPA entity against the domain type that has to be able to carry its columns. */
    private static final Map<Class<?>, Class<?>> DOMAIN_BEHIND = Map.of(
            TourOperatorBrandJpaEntity.class, Brand.class,
            TourOperatorBrandColorJpaEntity.class, BrandColor.class,
            TourOperatorBrandSocialLinkJpaEntity.class, BrandSocialLink.class);

    @Test
    void everyBrandColumnIsWritableExactlyWhileTheDomainCarriesIt() {
        DOMAIN_BEHIND.forEach(BrandColumnsStayReadOnlyTest::assertMapping);
    }

    /**
     * The brand's two translatable columns live on {@code tour_operator_translations},
     * which has its own write path — so they are governed by
     * {@link TourOperatorTranslation}, not by {@link Brand}. They were the
     * dangerous pair while the domain could not carry them: an SEO edit that
     * never mentioned the brand rebuilt the row and blanked both.
     */
    @Test
    void theTranslatedBrandColumnsFollowTheTranslationDomain() {
        for (String property : new String[]{"slogan", "shortDescription"}) {
            assertColumn(TourOperatorTranslationJpaEntity.class, property,
                    TourOperatorTranslation.class);
        }
    }

    private static void assertMapping(Class<?> entity, Class<?> domain) {
        for (Field field : persistentColumns(entity)) {
            assertColumn(entity, field.getName(), domain);
        }
    }

    private static void assertColumn(Class<?> entity, String property, Class<?> domain) {
        Column column = columnOf(entity, property);
        boolean domainCarriesIt = domainHasAccessor(domain, property);
        boolean writable = column.insertable() || column.updatable();

        assertThat(writable)
                .withFailMessage(
                        "%s.%s is %s, but the domain %s %s an accessor for it.%n"
                                + "  writable + no domain field -> the mapper writes a null and any "
                                + "unrelated save wipes the column%n"
                                + "  read-only + a domain field -> the write silently no-ops; the "
                                + "UPDATE omits the column%n"
                                + "Change both halves together.",
                        entity.getSimpleName(), property,
                        writable ? "writable" : "read-only",
                        domain.getSimpleName(),
                        domainCarriesIt ? "has" : "does not have")
                .isEqualTo(domainCarriesIt);
    }

    private static Column columnOf(Class<?> entity, String property) {
        return Arrays.stream(entity.getDeclaredFields())
                .filter(f -> f.getName().equals(property))
                .map(f -> f.getAnnotation(Column.class))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        entity.getSimpleName() + " has no mapped column " + property
                                + " — fix the test, do not delete it"));
    }

    private static List<Field> persistentColumns(Class<?> entity) {
        List<Field> fields = Arrays.stream(entity.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> !f.getName().equals("createdAt"))
                .filter(f -> !f.getName().equals("updatedAt"))
                .filter(f -> !f.getName().equals("tourOperatorId"))
                .toList();
        assertThat(fields)
                .withFailMessage("%s has no non-key @Column fields, so this test cannot see the "
                        + "mapping it exists to track. Fix the test, do not delete it.",
                        entity.getSimpleName())
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
