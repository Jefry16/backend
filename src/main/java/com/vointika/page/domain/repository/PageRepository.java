package com.vointika.page.domain.repository;

import com.vointika.page.domain.entity.Page;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface PageRepository {

    Page save(Page page);

    boolean existsByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId);

    Optional<Page> findByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId);

    /** The tenant-scoped read, for the callers that go on to use the page. */
    default Page requireByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(pageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(PageOwnershipQuery.NOT_FOUND));
    }

    /**
     * The same 404, for callers that only need the page to exist — the translation
     * endpoints, which then work entirely off the overlay table. Reading the whole
     * page, body included, to answer a boolean is what they did before.
     */
    default void requireExists(UUID pageId, UUID tourOperatorId) {
        if (!existsByIdAndTourOperatorId(pageId, tourOperatorId)) {
            throw new ResourceNotFoundException(PageOwnershipQuery.NOT_FOUND);
        }
    }

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    /** As above, ignoring one page — a page's own canonical handle never clashes with itself. */
    boolean existsByTourOperatorIdAndHandleExcluding(UUID tourOperatorId, String handle, UUID excludePageId);

    CursorPage<PageListItem> list(ListQuery query);

    void delete(UUID pageId);
}
