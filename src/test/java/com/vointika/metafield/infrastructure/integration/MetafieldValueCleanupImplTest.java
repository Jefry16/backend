package com.vointika.metafield.infrastructure.integration;

import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * The owner-side half of the orphan fix. Narrow on purpose, and worth pinning for
 * the same reason {@code SlotAudienceSnapshotPropagatorImplTest} is: what sits
 * behind this delegation is a bulk {@code @Modifying(clearAutomatically = true,
 * flushAutomatically = true)} query, and the caller deletes the page in the same
 * transaction. A rewrite into a find-then-delete loop would pass every use-case
 * test — they stub the port — and lose the flush/clear pairing silently.
 */
class MetafieldValueCleanupImplTest {

    private static final UUID OWNER = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    @Test
    void itDeletesByOwnerAndDoesNothingElse() {
        MetafieldValueRepository repository = mock(MetafieldValueRepository.class);

        new MetafieldValueCleanupImpl(repository).deleteValuesOwnedBy(OWNER);

        verify(repository).deleteByOwnerId(OWNER);
        verifyNoMoreInteractions(repository);
    }
}
