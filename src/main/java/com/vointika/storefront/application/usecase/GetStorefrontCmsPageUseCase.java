package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.port.StorefrontPageQuery.PageView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontPageOutput;
import com.vointika.storefront.application.policy.SeoText;

import java.util.Optional;

/**
 * One CMS page at {@code /pages/&#123;handle&#125;} — the globals plus the page.
 *
 * <p><b>It composes the globals use case rather than reassembling them.</b> Both
 * are plain POJOs in the same layer, and the alternative — every page route
 * building its own globals — is exactly how {@code shop.brand} would come to
 * mean one thing on the home page and another here.
 *
 * <p><b>The globals are read first, and that ordering is not cosmetic.</b> They
 * carry the locale decision: an address in a locale the shop does not publish is
 * a 404 before any page is looked up, so a draft page and an unpublished locale
 * cannot be told apart by trying both.
 */
public class GetStorefrontCmsPageUseCase {

    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final StorefrontPageQuery pageQuery;

    public GetStorefrontCmsPageUseCase(GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                       StorefrontPageQuery pageQuery) {
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.pageQuery = pageQuery;
    }

    /**
     * @param pathLocale the locale segment of the URL, or null for the bare path
     * @param handle     the page handle as it appears in the URL
     */
    public Optional<StorefrontPageOutput> execute(String shopHandle, String pathLocale, String handle) {
        return getStorefrontGlobals.execute(shopHandle, pathLocale).flatMap(globals ->
                pageQuery.findByHandle(globals.shop().id(), handle, globals.localization().current())
                        .map(page -> new StorefrontPageOutput(withPageSeo(globals, page), page)));
    }

    private static StorefrontGlobals withPageSeo(StorefrontGlobals globals, PageView page) {
        return globals.withSeo(
                SeoText.title(page.seoTitle(), page.title(),
                        globals.shop().seoTitle(), globals.shop().name()),
                SeoText.description(page.seoDescription(), globals.shop().seoDescription()));
    }
}
