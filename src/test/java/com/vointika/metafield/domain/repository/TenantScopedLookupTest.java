package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The three tenant-scoped lookups answer a 404 carrying the message their callers
 * used to write out by hand.
 *
 * <p><b>Why this exists.</b> {@code requireByIdAndTourOperatorId} replaced twenty
 * copies of {@code findByIdAndTourOperatorId(...).orElseThrow(() -> new
 * ResourceNotFoundException("… not found"))}, which kept each message in as many
 * places as there were call sites. Collapsing them moved the message somewhere a
 * use-case test cannot see: every use case mocks its repository, and Mockito stubs
 * a {@code default} method like any other, so the real body never runs there.
 * Without this test the three messages would be published by the API and asserted
 * nowhere — which is why {@code doCallRealMethod} is the point of it, not a detail.
 */
class TenantScopedLookupTest {

    private static final UUID ID = UUID.fromString("019f8000-0000-7000-8000-000000000001");
    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    @Test
    void aMissingMetaobjectEntryIsNotFound() {
        MetaobjectEntryRepository repository = mock(MetaobjectEntryRepository.class);
        when(repository.findByIdAndTourOperatorId(ID, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(ID, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(ID, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metaobject not found");
    }

    @Test
    void aMissingMetaobjectDefinitionIsNotFound() {
        MetaobjectDefinitionRepository repository = mock(MetaobjectDefinitionRepository.class);
        when(repository.findByIdAndTourOperatorId(ID, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(ID, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(ID, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metaobject definition not found");
    }

    @Test
    void aMissingMetafieldDefinitionIsNotFound() {
        MetafieldDefinitionRepository repository = mock(MetafieldDefinitionRepository.class);
        when(repository.findByIdAndTourOperatorId(ID, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(ID, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(ID, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metafield definition not found");
    }

    /**
     * <b>The other two lookups, and why they are here.</b> A value endpoint
     * addresses its definition by {@code namespace.key}, and rename/remove address a
     * field by key — different lookups, the same 404. Both were left executed by no
     * test at all when their call sites' assertions were rewritten into stubs of the
     * very method that now contains the throw. Caught in review by mutation:
     * replacing both bodies with {@code orElse(null)} left the suite green while
     * production would have NPE'd into a 500 where it used to answer 404.
     */
    @Test
    void aMissingIdentityIsNotFound() {
        MetafieldDefinitionRepository repository = mock(MetafieldDefinitionRepository.class);
        when(repository.findByIdentity(OPERATOR, MetafieldOwnerType.PAGE, "custom", "subtitle"))
                .thenReturn(Optional.empty());
        doCallRealMethod().when(repository)
                .requireByIdentity(OPERATOR, MetafieldOwnerType.PAGE, "custom", "subtitle");

        assertThatThrownBy(() -> repository.requireByIdentity(
                OPERATOR, MetafieldOwnerType.PAGE, "custom", "subtitle"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metafield definition not found");
    }

    @Test
    void aMissingMetaobjectFieldIsNotFound() {
        MetaobjectDefinitionRepository repository = mock(MetaobjectDefinitionRepository.class);
        when(repository.findField(ID, "heading")).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireField(ID, "heading");

        assertThatThrownBy(() -> repository.requireField(ID, "heading"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metaobject field not found");
    }

    /**
     * The found case comes back unwrapped — the whole reason twenty call sites
     * could drop a line each.
     */
    @Test
    void aPresentRowComesBackUnwrapped() {
        MetaobjectEntry found = new MetaobjectEntry(ID, OPERATOR, ID,
                new Handle("sea-swallow"), new MetaobjectEntryName("Sea Swallow"), OPERATOR);
        MetaobjectEntryRepository repository = mock(MetaobjectEntryRepository.class);
        when(repository.findByIdAndTourOperatorId(ID, OPERATOR)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(ID, OPERATOR);

        assertThat(repository.requireByIdAndTourOperatorId(ID, OPERATOR)).isSameAs(found);
    }
}
