package com.vointika.experience.application.service;

import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryReferenceValidatorTest {

    private static final UUID OPERATOR = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c51");
    private static final UUID CATEGORY = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c52");

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryReferenceValidator validator = new CategoryReferenceValidator(repository);

    /** Uncategorized is a state, not a missing value — and it costs no query. */
    @Test
    void aNullCategoryIsValidAndAsksNothing() {
        assertThatCode(() -> validator.validate(OPERATOR, null)).doesNotThrowAnyException();
        verify(repository, never()).existsByIdAndTourOperatorId(any(), any());
    }

    @Test
    void theOperatorsOwnCategoryPasses() {
        when(repository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(true);

        assertThatCode(() -> validator.validate(OPERATOR, CATEGORY)).doesNotThrowAnyException();
    }

    /**
     * 422 rather than the repository's 404: on an update the experience in the URL
     * does exist, so a 404 would name the wrong missing resource. It is a bad field
     * in the body, which is what the media validator beside it already answers 422 for.
     */
    @Test
    void anotherOperatorsCategoryIs422() {
        when(repository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(OPERATOR, CATEGORY))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage(CategoryReferenceValidator.NOT_THIS_OPERATORS);
    }

    /**
     * A foreign id and an id that exists nowhere raise the same refusal, so the
     * message cannot be used to probe another operator's categories.
     */
    @Test
    void anUnknownIdIsRefusedTheSameWay() {
        when(repository.existsByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(OPERATOR, CATEGORY))
                .hasMessage(CategoryReferenceValidator.NOT_THIS_OPERATORS);
    }
}
