package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyDetailView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontPolicyOutput;
import com.vointika.storefront.application.policy.PolicySlug;
import com.vointika.storefront.application.policy.SeoText;

import java.util.Optional;

/**
 * One policy's page: the globals plus the document.
 *
 * <p><b>Globals first, and the order is load-bearing</b> — they carry the locale
 * decision, so an address in a locale the operator does not publish is a 404
 * before any policy is looked up. That keeps an unpublished locale and an
 * unwritten policy indistinguishable, which is the same property the CMS page
 * and the experience page rely on.
 *
 * <p><b>The slug is translated to a type name here and validated nowhere.</b>
 * {@code storefront} cannot see {@code PolicyType}, and a copy of that enum would
 * be a second list nothing keeps equal. An unrecognised slug becomes a type name
 * no row has, so it answers empty — the same empty an operator who never wrote
 * that policy produces.
 *
 * <p><b>SEO comes from the policy's title.</b> A policy has no SEO columns of its
 * own, so the chain is title-then-operator, which is {@link SeoText}'s existing
 * shape; the description falls straight through to the operator's, because a
 * description that repeats the title is worse than none.
 */
public class GetStorefrontPolicyUseCase {

    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final StorefrontTourOperatorQuery tourOperatorQuery;

    public GetStorefrontPolicyUseCase(GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                      StorefrontTourOperatorQuery tourOperatorQuery) {
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.tourOperatorQuery = tourOperatorQuery;
    }

    public Optional<StorefrontPolicyOutput> execute(String operatorHandle, String pathLocale, String slug) {
        return getStorefrontGlobals.execute(operatorHandle, pathLocale).flatMap(globals ->
                tourOperatorQuery.findPolicy(globals.tourOperator().id(), PolicySlug.toTypeName(slug),
                                globals.localization().current())
                        .map(policy -> new StorefrontPolicyOutput(withPolicySeo(globals, policy), policy)));
    }

    private static StorefrontGlobals withPolicySeo(StorefrontGlobals globals, PolicyDetailView policy) {
        return globals.withSeo(
                SeoText.title(null, policy.title(),
                        globals.tourOperator().seoTitle(), globals.tourOperator().name()),
                SeoText.description(null, globals.tourOperator().seoDescription()));
    }
}
