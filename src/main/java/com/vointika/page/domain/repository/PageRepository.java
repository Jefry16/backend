package com.vointika.page.domain.repository;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface PageRepository {

    Page save(Page page);

    boolean existsByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId);

    Optional<Page> findByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    /** As above, ignoring one page — a page's own canonical handle never clashes with itself. */
    boolean existsByTourOperatorIdAndHandleExcluding(UUID tourOperatorId, String handle, UUID excludePageId);

    CursorPage<PageListItem> list(ListQuery query);

    void delete(UUID pageId);
}
