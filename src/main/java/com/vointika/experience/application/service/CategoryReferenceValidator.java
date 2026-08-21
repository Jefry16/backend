package com.vointika.experience.application.service;

import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.shared.exception.InvalidFieldException;

import java.util.UUID;

/**
 * Validates that the category an experience is filed under is one of the
 * operator's own. A null id is valid — uncategorized is a legitimate state.
 *
 * <p><b>422, not the repository's 404.</b> {@code CategoryRepository.requireExists}
 * would be the shorter call and answers the wrong question: on a PATCH the
 * experience in the URL does exist, so a 404 would say the wrong resource is
 * missing. What is wrong is a field in the body, which is what
 * {@link MediaReferenceValidator} already answers 422 for on the same two write
 * paths.
 *
 * <p>Category lives in this context, so this reaches the repository directly —
 * no shared port, which is the whole reason a category is not its own context.
 */
public class CategoryReferenceValidator {

    /** Says nothing about whether the id exists at all — a foreign id and an unknown one are one refusal. */
    public static final String NOT_THIS_OPERATORS =
            "Category not found in this operator's categories";

    private final CategoryRepository categoryRepository;

    public CategoryReferenceValidator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void validate(UUID tourOperatorId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        if (!categoryRepository.existsByIdAndTourOperatorId(categoryId, tourOperatorId)) {
            throw new InvalidFieldException(NOT_THIS_OPERATORS);
        }
    }
}
