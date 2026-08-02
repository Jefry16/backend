package com.vointika.shared.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An operator as the public storefront sees it — the tenant's identity and
 * storefront-level configuration, everything already resolved (logo id → URL,
 * currency id → code, timezone id → IANA name) so the consumer needs no further
 * lookups.
 *
 * <p>Carries the operator id for the renderer's own use — it is the key every
 * other cross-context port takes — but the id never reaches the wire: the
 * storefront addresses tenants by slug, and a theme has no use for it. That
 * split is why this is a port view and {@code ShopResponse} is a separate
 * record. It carries no storefront password — {@code passwordEnabled} says
 * whether the gate is on, and verification happens inside {@code touroperator}
 * via {@link StorefrontOperatorQuery#verifyStorefrontPassword}, so the plaintext
 * never crosses this seam.
 *
 * @param supportedLocales the operator's published content locales, primary
 *                         first, then the rest alphabetically — a stable order
 *                         the language switcher can render directly
 * @param passwordMessage  the operator's canonical message to visitors on the
 *                         gate page; null when unset. Per-locale overrides live
 *                         in {@code translations}.
 * @param ogImageUrl       the shop-level social image, already resolved from its
 *                         stored media id (PATTERNS §5)
 * @param translations     locale → overlay, for every locale this operator has
 *                         translated. See {@link StorefrontOperatorTranslationView}
 *                         for why they ride along rather than being asked for.
 */
public record StorefrontOperatorView(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String primaryLocale,
        List<String> supportedLocales,
        String currency,
        String timezone,
        boolean passwordEnabled,
        String passwordMessage,
        String seoTitle,
        String seoDescription,
        String ogImageUrl,
        Map<String, StorefrontOperatorTranslationView> translations) {

    public StorefrontOperatorView {
        translations = translations == null ? Map.of() : Map.copyOf(translations);
    }

    /**
     * This operator's overlay for {@code locale}, or an all-null one when the
     * locale is untranslated — so a caller can read through it without a null
     * check per field.
     */
    public StorefrontOperatorTranslationView translation(String locale) {
        StorefrontOperatorTranslationView overlay = locale == null ? null : translations.get(locale);
        return overlay == null
                ? new StorefrontOperatorTranslationView(null, null, null)
                : overlay;
    }
}
