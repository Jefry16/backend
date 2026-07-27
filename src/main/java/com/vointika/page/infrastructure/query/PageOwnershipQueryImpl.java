package com.vointika.page.infrastructure.query;

import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.port.PageOwnershipQuery;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** page's adapter for the shared ownership seam (metafield value writes). */
@Component
public class PageOwnershipQueryImpl implements PageOwnershipQuery {

    private final PageRepository pageRepository;

    public PageOwnershipQueryImpl(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public boolean existsForTourOperator(UUID pageId, UUID tourOperatorId) {
        return pageRepository.existsByIdAndTourOperatorId(pageId, tourOperatorId);
    }
}
