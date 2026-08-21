package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Category;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    /**
     * Said by four causes on purpose — the unknown id, another operator's id, the
     * translation endpoints' existence check, and an experience pointing at a
     * category that is not the operator's. Written once so they cannot drift apart.
     */
    String NOT_FOUND = "Category not found";

    /** Thrown by the pre-check and by the unique-index race, which must answer identically (PATTERNS §8d). */
    String NAME_TAKEN = "A category with this name already exists";

    Category save(Category category);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Category> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The tenant-scoped read, for the callers that go on to use the category. */
    default Category requireByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(id, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    /**
     * The same 404 for callers that only need the category to exist — the four
     * translation endpoints, which work off the overlay table afterwards.
     * Unlike {@code requireByIdAndTourOperatorId} this asks the cheap question,
     * because {@code existsBy…} is already on this interface for the experience
     * write paths (PATTERNS §9, "check which callers use the row they loaded").
     */
    default void requireExists(UUID id, UUID tourOperatorId) {
        if (!existsByIdAndTourOperatorId(id, tourOperatorId)) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
    }

    /** The operator's categories, cursor-paginated + filtered. */
    CursorPage<Category> list(ListQuery query);

    /** Whether this operator already has a category with this name (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    /** As above, ignoring one category — lets a category keep its own name on update. */
    boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId);

    /**
     * Removes the category. The experiences filed under it keep existing and
     * become uncategorized — {@code experiences.category_id} is
     * {@code ON DELETE SET NULL}, so the database does that sweep.
     */
    void delete(Category category);
}
