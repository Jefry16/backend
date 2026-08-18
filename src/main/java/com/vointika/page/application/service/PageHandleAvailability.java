package com.vointika.page.application.service;

import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.exception.ResourceAlreadyExistsException;

import java.util.UUID;

/**
 * Is this handle free across <b>both</b> namespaces? (PATTERNS §4d.)
 *
 * <p>The storefront resolves a handle against localized handles first and canonical
 * ones second, so the two are one namespace on the read side and have to be checked
 * as one on the write side — a canonical handle equal to another page's localized
 * handle silently shadows it in that locale.
 *
 * <p><b>It lives here because it was written twice</b>, in create and rename,
 * differing only in which page excludes itself. A third handle-setting path is the
 * case this guards: copying eight lines is easy, and remembering the second namespace
 * while doing it is the part that gets dropped — which is the shadowing bug §4d was
 * written for.
 *
 * <p>Both refusals are published examples ({@code pages/create-conflict},
 * {@code pages/rename-conflict}), so the sentences are contract and must not drift.
 */
public class PageHandleAvailability {

    /** Also thrown from the create/rename race handlers, so the pre-check and the index agree. */
    public static final String CANONICAL_TAKEN = "A page with this handle already exists";

    private static final String LOCALIZED_TAKEN = "A page already uses this handle as a localized handle";

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;

    public PageHandleAvailability(PageRepository pageRepository,
                                  PageTranslationRepository translationRepository) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
    }

    /**
     * @param excludePageId the page allowed to already hold this handle <em>as a
     *                      localized one</em>; null on create, where no page exists
     *                      yet. The canonical check needs no exclusion: create has no
     *                      page, and rename returns early when the handle is unchanged,
     *                      so neither can collide with itself.
     */
    public void requireFree(UUID tourOperatorId, String handle, UUID excludePageId) {
        if (pageRepository.existsByTourOperatorIdAndHandle(tourOperatorId, handle)) {
            throw new ResourceAlreadyExistsException(CANONICAL_TAKEN);
        }
        if (translationRepository.existsByHandleInAnyLocale(tourOperatorId, handle, excludePageId)) {
            throw new ResourceAlreadyExistsException(LOCALIZED_TAKEN);
        }
    }
}
