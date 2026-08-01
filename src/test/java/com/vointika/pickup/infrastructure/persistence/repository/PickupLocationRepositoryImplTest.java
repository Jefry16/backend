package com.vointika.pickup.infrastructure.persistence.repository;

import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The duplicate-name pre-check must be case-INSENSITIVE, matching the
 * {@code lower(name)} unique index in pickup/V1.
 *
 * <p>Pinned here because the use-case tests cannot see it: they stub
 * {@code existsByTourOperatorIdAndName…} directly, so they pass whether the
 * query underneath ignores case or not. Drop the {@code IgnoreCase} and
 * "Beach Pier" vs "beach pier" stops being a 409 from the pre-check and becomes
 * a 500 from the index — which is the bug {@code audience} shipped and had to
 * fix with a V2 migration.
 */
class PickupLocationRepositoryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXCLUDED = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");

    private PickupLocationJpaRepository jpa;
    private PickupLocationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        jpa = mock(PickupLocationJpaRepository.class);
        repository = new PickupLocationRepositoryImpl(jpa, mock(CriteriaListExecutor.class));
    }

    @Test
    void theCreatePreCheckIgnoresCase() {
        when(jpa.existsByTourOperatorIdAndNameIgnoreCase(OP, "beach pier")).thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndName(OP, "beach pier")).isTrue();

        verify(jpa).existsByTourOperatorIdAndNameIgnoreCase(OP, "beach pier");
    }

    @Test
    void theUpdatePreCheckIgnoresCaseAndExcludesTheRowBeingEdited() {
        when(jpa.existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(OP, "beach pier", EXCLUDED))
                .thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndNameExcluding(OP, "beach pier", EXCLUDED))
                .isTrue();

        verify(jpa).existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(OP, "beach pier", EXCLUDED);
    }
}
