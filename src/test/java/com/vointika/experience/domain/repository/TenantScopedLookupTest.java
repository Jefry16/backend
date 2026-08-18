package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.ExperienceOwnershipQuery;
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
 * {@code requireByIdAndTourOperatorId} and {@code requireExists} actually run.
 *
 * <p>`PATTERNS.md` §9: Mockito stubs a {@code default} method like any other, and
 * every repository here is mocked in every use-case test — so collapsing the twelve
 * inline {@code orElseThrow}s into these two defaults moved the throw somewhere a
 * mocked repository reaches only if a test asks it to.
 *
 * <p><b>This file is the second guard, not the only one.</b> The six use-case
 * {@code setUp}s call {@code doCallRealMethod()} on the defaults and go on stubbing
 * the <em>abstract</em> finder, so their {@code unknownExperienceIs404} tests still
 * run the real branch — which is why inverting both defaults fails five of them as
 * well as the four here. Stubbing the default at those call sites instead would have
 * been the easier repair and would have made all five assert only that Mockito
 * rethrows what it was told to throw. That is the arrangement `touroperator` and
 * `metafield` still have; PATTERNS §9 records why this one is stronger.
 *
 * <p>What this file adds is coverage that does not depend on a caller existing: it
 * pins both defaults directly, including {@code requireExists}'s true branch, which
 * no 404 test can reach.
 *
 * <p><b>Mutate the bodies and watch these fail</b>, or they prove nothing. The
 * mutation that matters for {@code requireExists} is inverting its condition: the
 * four translation endpoints would then 404 on every experience that does exist.
 */
class TenantScopedLookupTest {

    private static final UUID EXPERIENCE = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c11");
    private static final UUID OPERATOR = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c12");

    @Test
    void aMissingExperienceIsTheTenantScoped404() {
        ExperienceRepository repository = mock(ExperienceRepository.class);
        when(repository.findByIdAndTourOperatorId(EXPERIENCE, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(EXPERIENCE, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(EXPERIENCE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ExperienceOwnershipQuery.NOT_FOUND);
    }

    @Test
    void aPresentExperienceComesBackUnwrapped() {
        Experience found = mock(Experience.class);
        ExperienceRepository repository = mock(ExperienceRepository.class);
        when(repository.findByIdAndTourOperatorId(EXPERIENCE, OPERATOR)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(EXPERIENCE, OPERATOR);

        assertThat(repository.requireByIdAndTourOperatorId(EXPERIENCE, OPERATOR)).isSameAs(found);
    }

    @Test
    void requireExistsRefusesWhatTheOperatorDoesNotOwn() {
        ExperienceRepository repository = mock(ExperienceRepository.class);
        when(repository.existsByIdAndTourOperatorId(EXPERIENCE, OPERATOR)).thenReturn(false);
        doCallRealMethod().when(repository).requireExists(EXPERIENCE, OPERATOR);

        assertThatThrownBy(() -> repository.requireExists(EXPERIENCE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ExperienceOwnershipQuery.NOT_FOUND);
    }

    @Test
    void requireExistsPassesWhatItDoes() {
        ExperienceRepository repository = mock(ExperienceRepository.class);
        when(repository.existsByIdAndTourOperatorId(EXPERIENCE, OPERATOR)).thenReturn(true);
        doCallRealMethod().when(repository).requireExists(EXPERIENCE, OPERATOR);

        assertThatCode(() -> repository.requireExists(EXPERIENCE, OPERATOR)).doesNotThrowAnyException();
    }
}
