package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Category;
import com.vointika.shared.exception.ResourceNotFoundException;
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
 * {@code CategoryRepository}'s two {@code default}s actually run — the sibling of
 * {@link TenantScopedLookupTest}, for the same reason (`PATTERNS.md` §9: Mockito
 * stubs a default like any other method, so a default's body is reached in a
 * mocked-repository test only when a test asks for it).
 *
 * <p><b>Mutate the bodies and watch these fail</b>, or they prove only that the
 * mock returns what it was told. The mutation that matters for
 * {@code requireExists} is inverting its condition: the four translation
 * endpoints would then 404 on every category that does exist.
 */
class CategoryTenantScopedLookupTest {

    private static final UUID CATEGORY = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c21");
    private static final UUID OPERATOR = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c22");

    @Test
    void aMissingCategoryIsTheTenantScoped404() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(CATEGORY, OPERATOR);

        assertThatThrownBy(() -> repository.requireByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CategoryRepository.NOT_FOUND);
    }

    @Test
    void aPresentCategoryComesBackUnwrapped() {
        Category found = mock(Category.class);
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(CATEGORY, OPERATOR);

        assertThat(repository.requireByIdAndTourOperatorId(CATEGORY, OPERATOR)).isSameAs(found);
    }

    @Test
    void requireExistsRefusesWhatTheOperatorDoesNotOwn() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(false);
        doCallRealMethod().when(repository).requireExists(CATEGORY, OPERATOR);

        assertThatThrownBy(() -> repository.requireExists(CATEGORY, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CategoryRepository.NOT_FOUND);
    }

    @Test
    void requireExistsPassesWhatItDoes() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(true);
        doCallRealMethod().when(repository).requireExists(CATEGORY, OPERATOR);

        assertThatCode(() -> repository.requireExists(CATEGORY, OPERATOR)).doesNotThrowAnyException();
    }
}
