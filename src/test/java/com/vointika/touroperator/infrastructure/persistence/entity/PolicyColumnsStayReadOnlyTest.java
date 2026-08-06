package com.vointika.touroperator.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The policies are a read path, and this pins the reason rather than the
 * setting. It is {@code BrandColumnsStayReadOnlyTest}'s invariant applied to
 * V12's tables: <b>a column is mapped read-only exactly while nothing can write
 * it.</b>
 *
 * <p>Nothing can, today — there is no domain {@code Policy}, no repository that
 * saves one and no admin endpoint. That makes both directions silent:
 *
 * <ul>
 *   <li><b>Writable while nothing carries the value</b> — every write here would
 *       go through a mapper rebuilding the entity from something that does not
 *       hold these columns, so an unrelated save writes the null it had to
 *       invent. That is exactly how a translation upsert would blank a
 *       policy.</li>
 *   <li><b>Read-only once a writer exists</b> — the write path lands, every test
 *       that stops at the use case passes, and the UPDATE silently omits the
 *       column.</li>
 * </ul>
 *
 * <p>So the slice that adds the write path changes both halves together, and
 * this test is what says so at the moment it changes one.
 */
class PolicyColumnsStayReadOnlyTest {

    @Test
    void everyPolicyColumnIsReadOnlyWhileNothingWritesOne() {
        for (Class<?> entity : List.of(TourOperatorPolicyJpaEntity.class,
                TourOperatorPolicyTranslationJpaEntity.class)) {
            for (Field field : persistentColumns(entity)) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.insertable() || column.updatable())
                        .withFailMessage(
                                "%s.%s is writable, but nothing writes a policy: there is no repository "
                                        + "that saves one and no domain object behind this table. A "
                                        + "writable column here is a column an unrelated save can null. "
                                        + "The slice that adds the write path takes these flags off in "
                                        + "the same change.",
                                entity.getSimpleName(), field.getName())
                        .isFalse();
            }
        }
    }

    /** Mapped, non-key fields — the primary key is written by definition and says nothing here. */
    private static List<Field> persistentColumns(Class<?> entity) {
        List<Field> fields = Arrays.stream(entity.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .toList();
        assertThat(fields)
                .withFailMessage("%s has no non-key @Column fields, so this test cannot see the mapping "
                        + "it exists to track. Fix the test, do not delete it.", entity.getSimpleName())
                .isNotEmpty();
        return fields;
    }
}
