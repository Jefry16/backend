package com.vointika.touroperator.domain.repository;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code requireById} and the invitation supplier actually run.
 *
 * <p><b>This is `PATTERNS.md` §9's rule applied to this context, one PR after that
 * rule was written down and not applied here.</b> Every repository is mocked in
 * every use-case test, and Mockito stubs a {@code default} method like any other —
 * so collapsing a dozen {@code orElseThrow}s into {@code requireById} moved the
 * throw somewhere no existing test could reach. The call sites' assertions had been
 * rewritten from stubbing the abstract method to stubbing the default one, which
 * proves only that Mockito rethrows what it was told to throw.
 *
 * <p>Caught in review by the probe §9 prescribes: replacing the body with
 * {@code orElse(null)} left the suite green, while production would have turned
 * every tenant-scoped read on a missing operator into a 500 where the isolation 404
 * belongs. <b>Mutate the bodies below and watch these fail</b>, or they are proving
 * nothing.
 */
class TenantScopedLookupTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    @Test
    void aMissingOperatorIsTheTenantIsolation404() {
        TourOperatorRepository repository = mock(TourOperatorRepository.class);
        when(repository.findById(OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireById(OPERATOR);

        assertThatThrownBy(() -> repository.requireById(OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(TourOperatorMembershipCheck.TENANT_NOT_FOUND);
    }

    @Test
    void aPresentOperatorComesBackUnwrapped() {
        TourOperator found = mock(TourOperator.class);
        TourOperatorRepository repository = mock(TourOperatorRepository.class);
        when(repository.findById(OPERATOR)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireById(OPERATOR);

        assertThat(repository.requireById(OPERATOR)).isSameAs(found);
    }

    /**
     * The invitation refusal is a {@code Supplier} constant rather than a default
     * method, so no mock intercepts it — but it is the same class of thing and the
     * same one-word edit away from changing four published bodies, so it is pinned
     * here beside its neighbour.
     */
    @Test
    void theInvitationRefusalIsThisSentence() {
        assertThat(TourOperatorInvitationRepository.NOT_FOUND.get())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invitation not found");
    }
}
