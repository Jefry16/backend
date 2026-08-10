package com.vointika.experience.infrastructure.integration;

import com.vointika.experience.infrastructure.persistence.repository.SlotAudiencePricingJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * The #47 propagator: an audience rename must reach the snapshots already frozen
 * onto existing departures, or a slot keeps advertising the old tier name.
 *
 * <p>What this pins is narrow and deliberate — that the adapter forwards to
 * {@code updateSnapshotByAudienceId} and does nothing else. It is worth pinning
 * because of what sits on the other side: that query carries
 * {@code @Modifying(clearAutomatically = true, flushAutomatically = true)}, and
 * PATTERNS §11 records why both halves are load-bearing. The caller saves the
 * audience in the same transaction, so without {@code flushAutomatically} the
 * bulk update would not see it and {@code clearAutomatically} would then discard
 * the pending save outright. A rewrite that "simplified" this adapter into a
 * findAll-and-save loop would pass every use-case test — they stub the port —
 * and lose that protection silently.
 */
class SlotAudienceSnapshotPropagatorImplTest {

    private static final UUID AUDIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private SlotAudiencePricingJpaRepository jpa;
    private SlotAudienceSnapshotPropagatorImpl propagator;

    @BeforeEach
    void setUp() {
        jpa = mock(SlotAudiencePricingJpaRepository.class);
        propagator = new SlotAudienceSnapshotPropagatorImpl(jpa);
    }

    @Test
    void aRenamePropagatesAsASingleBulkUpdate() {
        propagator.propagate(AUDIENCE, "Adultos", 1);

        verify(jpa).updateSnapshotByAudienceId(AUDIENCE, "Adultos", 1);
        // No read-then-write: the flush/clear semantics live on that one query.
        verifyNoMoreInteractions(jpa);
    }

    @Test
    void paxPerUnitTravelsWithTheName() {
        propagator.propagate(AUDIENCE, "Family pack", 4);

        verify(jpa).updateSnapshotByAudienceId(AUDIENCE, "Family pack", 4);
    }
}
