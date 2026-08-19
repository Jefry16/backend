package com.vointika.page.domain.repository;

import com.vointika.page.domain.entity.Page;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.PageOwnershipQuery;
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
 * `page`'s copy of the `experience` guard — the two defaults run.
 *
 * <p>`PATTERNS.md` §9: Mockito stubs a {@code default} like any other method, so
 * folding eleven `orElseThrow`s into these two moved the throw somewhere a mocked
 * repository reaches only when asked. The call sites keep the stronger arrangement
 * (`doCallRealMethod` in their `setUp`, stubbing the abstract lookups), so breaking
 * both defaults already fails three of their tests.
 *
 * <p>This file adds what a call site cannot guarantee: coverage that survives a
 * caller being deleted, and both branches pinned in one place. <b>Mutate the bodies
 * and watch these fail.</b>
 */
class TenantScopedLookupTest {

    private static final UUID PAGE = UUID.fromString("019f8d55-6e30-7a14-b2c9-70f5e1a49b03");
    private static final UUID OPERATOR = UUID.fromString("019f8d55-6e30-7a14-b2c9-70f5e1a49b04");

    @Test
    void aMissingPageIsTheTenantScoped404() {
        PageRepository repository = mock(PageRepository.class);
        when(repository.findByIdAndTourOperatorId(PAGE, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(PAGE, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(PAGE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(PageOwnershipQuery.NOT_FOUND);
    }

    @Test
    void aPresentPageComesBackUnwrapped() {
        Page found = mock(Page.class);
        PageRepository repository = mock(PageRepository.class);
        when(repository.findByIdAndTourOperatorId(PAGE, OPERATOR)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(PAGE, OPERATOR);

        assertThat(repository.requireByIdAndTourOperatorId(PAGE, OPERATOR)).isSameAs(found);
    }

    @Test
    void requireExistsRefusesWhatTheOperatorDoesNotOwn() {
        PageRepository repository = mock(PageRepository.class);
        when(repository.existsByIdAndTourOperatorId(PAGE, OPERATOR)).thenReturn(false);
        doCallRealMethod().when(repository).requireExists(PAGE, OPERATOR);

        assertThatThrownBy(() -> repository.requireExists(PAGE, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(PageOwnershipQuery.NOT_FOUND);
    }

    @Test
    void requireExistsPassesWhatItDoes() {
        PageRepository repository = mock(PageRepository.class);
        when(repository.existsByIdAndTourOperatorId(PAGE, OPERATOR)).thenReturn(true);
        doCallRealMethod().when(repository).requireExists(PAGE, OPERATOR);

        assertThatCode(() -> repository.requireExists(PAGE, OPERATOR)).doesNotThrowAnyException();
    }
}
