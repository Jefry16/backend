package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontPolicyView;
import com.vointika.storefront.application.dto.output.PolicyPageOutput;
import com.vointika.storefront.application.dto.output.PolicyPageOutput.PolicyDocument;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import com.vointika.storefront.application.policy.LocaleResolver;
import com.vointika.storefront.application.policy.PolicySlug;

import java.util.Optional;

/**
 * One legal document, in the locale the path asks for, or nothing. It mirrors
 * {@link GetHomePageUseCase}'s preamble — gate row → {@link LocaleResolver} →
 * content — because which tenant and which locale belong to the request rather
 * than to the page, and adds the one question this route asks: is there a policy
 * of that type.
 *
 * <p><b>Empty covers three different misses and answers all of them the same
 * way</b>, which is the point: an unknown storefront, a locale this operator
 * does not publish, and a policy type they have not written — including a slug
 * no type is named after. Distinguishing them on a public page would tell an
 * anonymous visitor what exists.
 *
 * <p>It throws nothing. A miss is ordinary traffic on a public site, and an
 * exception here would reach the API's JSON error handler on an HTML page.
 */
public class GetPolicyPageUseCase {

    private final StorefrontShopQuery storefrontShopQuery;

    public GetPolicyPageUseCase(StorefrontShopQuery storefrontShopQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
    }

    /**
     * @param pathLocale the locale prefix in the URL, or {@code null} for the
     *                   unprefixed {@code /policies/…}
     * @param slug       the type as it is addressed — {@code legal-notice}, not
     *                   {@code LEGAL_NOTICE}
     */
    public Optional<PolicyPageOutput> execute(String handle, String pathLocale, String slug) {
        return storefrontShopQuery.findGate(handle).flatMap(gate -> render(gate, pathLocale, slug));
    }

    private Optional<PolicyPageOutput> render(StorefrontGateView gate, String pathLocale, String slug) {
        return LocaleResolver.resolve(pathLocale, gate.primaryLocale(), gate.supportedLocales())
                .flatMap(locale -> storefrontShopQuery.findContent(gate.tourOperatorId(), locale)
                        .flatMap(shop -> storefrontShopQuery
                                .findPolicy(gate.tourOperatorId(), PolicySlug.toType(slug), locale)
                                .map(policy -> new PolicyPageOutput(
                                        StorefrontPageData.from(gate, shop, locale)
                                                .withPageTitle(policy.title()),
                                        document(policy)))));
    }

    private static PolicyDocument document(StorefrontPolicyView policy) {
        return new PolicyDocument(policy.type(), policy.title(), policy.body());
    }
}
