package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The brand is a read path with one exception, and this pins the reason rather
 * than the setting — because both ways of getting it wrong are silent. It is
 * {@code ContactColumnsStayReadOnlyTest}'s invariant applied to V10's columns:
 * <b>a column is mapped read-only exactly when nothing can write it.</b>
 *
 * <p>Every write here goes mapper-or-repository → {@code save}, rebuilding the
 * entity from something that does not carry these values. So:
 *
 * <ul>
 *   <li><b>Flags removed while nothing carries the value</b> — the null that had
 *       to be invented is written, and a request that never mentioned the brand
 *       wipes it. Setting a logo would blank the slogan.</li>
 *   <li><b>A writer appears while the flags stay on</b> — the write path lands,
 *       every test that stops at the use case passes, and the UPDATE silently
 *       omits the column.</li>
 * </ul>
 *
 * <p>So the slice that adds the brand write path changes both halves together,
 * and this test is what says so at the moment it changes one.
 */
class BrandColumnsStayReadOnlyTest {

    /**
     * {@code logo_media_id} is the exception, and the only one: V10 moved it off
     * {@code tour_operators}, so the admin endpoint that already owned the logo
     * followed it here through {@link TourOperatorBrandRepository}. Everything
     * else on the brand has no writer at all.
     *
     * <p>The repository's own surface is the oracle rather than a list retyped
     * here — add {@code setSlogan} and this test expects the flags to come off
     * that column, which is exactly the moment they should.
     */
    @Test
    void aBrandColumnIsWritableExactlyWhileTheRepositoryCanWriteIt() {
        for (Field field : persistentColumns(TourOperatorBrandJpaEntity.class)) {
            boolean repositoryWritesIt = hasSetterFor(field.getName());
            Column column = field.getAnnotation(Column.class);

            assertThat(column.insertable() || column.updatable())
                    .withFailMessage(
                            "TourOperatorBrandJpaEntity.%s is %s, but TourOperatorBrandRepository %s "
                                    + "a set%s method.%n"
                                    + "  writable + nobody writes it -> the repository invents a null and "
                                    + "any unrelated brand save wipes the column%n"
                                    + "  read-only + a writer exists -> the write path silently no-ops; "
                                    + "the UPDATE omits the column%n"
                                    + "Change both halves together.",
                            field.getName(),
                            column.insertable() || column.updatable() ? "writable" : "read-only",
                            repositoryWritesIt ? "has" : "does not have",
                            capitalize(field.getName()))
                    .isEqualTo(repositoryWritesIt);
        }
    }

    /**
     * The palette and the social links have no write surface at all — no
     * repository, no domain object, nothing that saves them. Every column is
     * therefore read-only, and the first writer flips this test with the flags.
     */
    @Test
    void theBrandChildTablesAreMappedEntirelyReadOnly() {
        for (Class<?> entity : List.of(TourOperatorBrandColorJpaEntity.class,
                TourOperatorBrandSocialLinkJpaEntity.class)) {
            for (Field field : persistentColumns(entity)) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.insertable() || column.updatable())
                        .withFailMessage(
                                "%s.%s is writable, but nothing writes the brand's %s: there is no "
                                        + "repository and no domain object behind this table. A writable "
                                        + "column here is a column an unrelated save can null.",
                                entity.getSimpleName(), field.getName(), entity.getSimpleName())
                        .isFalse();
            }
        }
    }

    /**
     * V10 put {@code slogan} and {@code short_description} on the operator's
     * existing translation table, which <b>does</b> have a live write path
     * ({@code PUT .../translations/{locale}}). That is what makes these two the
     * dangerous ones: an SEO edit that never mentions the brand rebuilds the row.
     */
    @Test
    void theTranslatedBrandColumnsAreReadOnlyWhileTheDomainCannotCarryThem() {
        for (String property : new String[]{"slogan", "shortDescription"}) {
            boolean domainCarriesIt = domainHasAccessor(property);
            Column column = mapping(TourOperatorTranslationJpaEntity.class, property);

            assertThat(column.insertable() || column.updatable())
                    .withFailMessage(
                            "TourOperatorTranslationJpaEntity.%s is %s, but the domain "
                                    + "TourOperatorTranslation %s an accessor for it. Change both "
                                    + "halves together — writable with no domain field means every "
                                    + "translation upsert wipes the column.",
                            property,
                            column.insertable() || column.updatable() ? "writable" : "read-only",
                            domainCarriesIt ? "has" : "does not have")
                    .isEqualTo(domainCarriesIt);
        }
    }

    /** Mapped, non-key fields — the primary key is written by definition and says nothing here. */
    private static List<Field> persistentColumns(Class<?> entity) {
        List<Field> fields = Arrays.stream(entity.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> !f.getName().equals("createdAt") && !f.getName().equals("updatedAt"))
                .toList();
        assertThat(fields)
                .withFailMessage("%s has no non-key @Column fields, so this test cannot see the mapping "
                        + "it exists to track. Fix the test, do not delete it.", entity.getSimpleName())
                .isNotEmpty();
        return fields;
    }

    private static Column mapping(Class<?> entity, String property) {
        try {
            Column column = entity.getDeclaredField(property).getAnnotation(Column.class);
            assertThat(column)
                    .withFailMessage("%s.%s has no @Column, so this test cannot see the mapping it "
                            + "exists to track. Fix the test, do not delete it.",
                            entity.getSimpleName(), property)
                    .isNotNull();
            return column;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(
                    entity.getSimpleName() + " has no '" + property + "' field. If it was renamed, rename "
                            + "it here too; if it was removed, remove this expectation deliberately.", e);
        }
    }

    private static boolean hasSetterFor(String property) {
        String setter = "set" + capitalize(property);
        return Arrays.stream(TourOperatorBrandRepository.class.getMethods())
                .anyMatch(m -> m.getName().equals(setter));
    }

    /** Any zero-arg accessor naming the property — {@code getSlogan()} or {@code slogan()}. */
    private static boolean domainHasAccessor(String property) {
        String getter = "get" + capitalize(property);
        return Arrays.stream(TourOperatorTranslation.class.getMethods())
                .anyMatch(m -> m.getParameterCount() == 0
                        && (m.getName().equals(getter) || m.getName().equals(property)));
    }

    private static String capitalize(String property) {
        return Character.toUpperCase(property.charAt(0)) + property.substring(1);
    }
}
