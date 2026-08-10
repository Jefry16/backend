package com.vointika.audience.infrastructure.persistence.repository;

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
 * {@code lower(name)} unique index that audience/V2 exists to add.
 *
 * <p>Pinned here because nothing else can see it. The use-case tests stub
 * {@link com.vointika.audience.domain.repository.AudienceRepository} — the
 * domain port — so the adapter underneath never runs and they pass whether the
 * query ignores case or not. That is not a guess: swapping this delegate to the
 * case-sensitive derived query and running the <em>whole</em> suite gave 1112
 * passing tests and zero failures.
 *
 * <p>What the swap costs in production is not cosmetic. "Adults" vs "adults"
 * stops being a 409 from the pre-check and becomes a <b>500</b> from the index —
 * which is the bug {@code audience} shipped and had to correct with V2, and the
 * one {@code pickup} already guards against
 * ({@code PickupLocationRepositoryImplTest}).
 */
class AudienceRepositoryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXCLUDED = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");

    private AudienceJpaRepository jpa;
    private AudienceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        jpa = mock(AudienceJpaRepository.class);
        repository = new AudienceRepositoryImpl(jpa, mock(CriteriaListExecutor.class));
    }

    @Test
    void theCreatePreCheckIgnoresCase() {
        when(jpa.existsByTourOperatorIdAndNameIgnoreCase(OP, "adults")).thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndName(OP, "adults")).isTrue();

        verify(jpa).existsByTourOperatorIdAndNameIgnoreCase(OP, "adults");
    }

    @Test
    void theUpdatePreCheckIgnoresCaseAndExcludesTheRowBeingEdited() {
        when(jpa.existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(OP, "adults", EXCLUDED))
                .thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndNameExcluding(OP, "adults", EXCLUDED))
                .isTrue();

        verify(jpa).existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(OP, "adults", EXCLUDED);
    }
}
