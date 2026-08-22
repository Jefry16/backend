package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontContactOutput;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontContactUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * The contact page — where a visitor writes to the operator's inbox.
 *
 * <p><b>It serves the form's shape and not a form</b>, and it accepts nothing.
 * Intake was deleted with the rest of the public write surface, so the page
 * describes what a message is — four fields and their limits, read off the
 * {@code contact} domain — and stops there. A {@code POST} arrives with the cart,
 * which is where the first public writes are being designed as a whole
 * ({@code OPEN-WORK.md}).
 *
 * <p><b>A route of its own is a deliberate divergence from Shopify</b>, where the
 * contact page is an ordinary CMS page carrying the {@code page.contact} template
 * and only the submission goes to {@code /contact}. The consequence to know: an
 * operator can still author a CMS page at {@code /pages/contact} — several have —
 * and the two pages do not know about each other.
 *
 * <p><b>The form has nothing to miss on</b>, so unlike every other page route this
 * one has a single source of 404: the host or the locale, both decided by the
 * globals. There is no second lookup that can come back empty.
 */
@RestController
public class StorefrontContactController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontContactUseCase getStorefrontContact;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontContactController(TenantHandleResolver tenantHandleResolver,
                                       GetStorefrontContactUseCase getStorefrontContact,
                                       MediaAssetBatchQuery mediaAssetBatchQuery,
                                       MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontContact = getStorefrontContact;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping(path = StorefrontRoutes.CONTACT, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse contact(HttpServletRequest request) {
        return render(request, null);
    }

    @GetMapping(path = StorefrontRoutes.LOCALIZED_CONTACT, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedContact(@PathVariable String locale,
                                                      HttpServletRequest request) {
        return render(request, locale);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale) {
        String operatorHandle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);

        StorefrontContactOutput output = getStorefrontContact.execute(operatorHandle, pathLocale)
                .orElseThrow(StorefrontControllers::notFound);

        Map<UUID, MediaAsset> assets = StorefrontControllers.assets(output.globals(), mediaAssetBatchQuery);

        return StorefrontGlobalsResponse.contact(output.globals(), output.form(),
                StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }
}
