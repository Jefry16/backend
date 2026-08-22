package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontContactQuery;
import com.vointika.storefront.application.dto.output.StorefrontContactOutput;

import java.util.Optional;

/**
 * The contact page: the globals, plus what the inbox accepts.
 *
 * <p><b>The globals still come first</b>, and the ordering matters for the same
 * reason it does on every other page — they carry the locale decision, so
 * {@code /de/contact} on an operator that does not publish German is a 404 before
 * anything else is read. Empty here means the operator or the locale, never the
 * form.
 *
 * <p><b>SEO is not overridden.</b> A contact page has no entity and no title of
 * its own, so {@code pageTitle} and {@code pageDescription} fall through to the
 * operator's — deliberately not papered over with a hardcoded "Contact", which
 * would be an untranslatable English string on a storefront that serves several
 * languages. It is the second page type in that position; {@code OPEN-WORK.md}
 * carries the question.
 */
public class GetStorefrontContactUseCase {

    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final StorefrontContactQuery contactQuery;

    public GetStorefrontContactUseCase(GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                       StorefrontContactQuery contactQuery) {
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.contactQuery = contactQuery;
    }

    public Optional<StorefrontContactOutput> execute(String operatorHandle, String pathLocale) {
        return getStorefrontGlobals.execute(operatorHandle, pathLocale)
                .map(globals -> new StorefrontContactOutput(globals, contactQuery.form()));
    }
}
