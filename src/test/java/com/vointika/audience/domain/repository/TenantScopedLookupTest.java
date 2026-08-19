package com.vointika.audience.domain.repository;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AudienceOwnershipQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * `audience`'s copy of the guard — the two defaults run (`PATTERNS.md` §9).
 *
 * <p><b>{@code requireExists} delegates to {@code requireByIdAndTourOperatorId} here
 * rather than issuing its own query</b>, unlike `experience` and `page`, whose
 * aggregates are expensive to load. That makes the delegation itself worth pinning:
 * if it ever grows a separate {@code exists} call, {@link #requireExistsRefusesWhatTheOperatorDoesNotOwn}
 * keeps holding, so read this file as covering the message and the branch, not the
 * query plan.
 *
 * <p>Both defaults need {@code doCallRealMethod} together — stubbing only the outer
 * one leaves the inner returning null, and nothing throws.
 */
class TenantScopedLookupTest {

    private static final UUID AUDIENCE = UUID.fromString("019f9012-77b4-7c58-9de1-4a2b06f7c310");
    private static final UUID OPERATOR = UUID.fromString("019f9012-77b4-7c58-9de1-4a2b06f7c311");

    private static AudienceRepository repositoryReturning(Optional<Audience> result) {
        AudienceRepository repository = mock(AudienceRepository.class);
        when(repository.findByIdAndTourOperatorId(AUDIENCE, OPERATOR)).thenReturn(result);
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(AUDIENCE, OPERATOR);
        doCallRealMethod().when(repository).requireExists(AUDIENCE, OPERATOR);
        return repository;
    }

    @Test
    void aMissingAudienceIsTheTenantScoped404() {
        AudienceRepository repository = repositoryReturning(Optional.empty());

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(AUDIENCE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(AudienceOwnershipQuery.NOT_FOUND);
    }

    @Test
    void aPresentAudienceComesBackUnwrapped() {
        Audience found = mock(Audience.class);
        AudienceRepository repository = repositoryReturning(Optional.of(found));

        assertThat(repository.requireByIdAndTourOperatorId(AUDIENCE, OPERATOR)).isSameAs(found);
    }

    @Test
    void requireExistsRefusesWhatTheOperatorDoesNotOwn() {
        AudienceRepository repository = repositoryReturning(Optional.empty());

        assertThatThrownBy(() -> repository.requireExists(AUDIENCE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(AudienceOwnershipQuery.NOT_FOUND);
    }

    @Test
    void requireExistsPassesWhatItDoes() {
        AudienceRepository repository = repositoryReturning(Optional.of(mock(Audience.class)));

        assertThatCode(() -> repository.requireExists(AUDIENCE, OPERATOR)).doesNotThrowAnyException();
    }
}
