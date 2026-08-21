package com.vointika.shared.port;

import java.util.Map;

/**
 * The handles one resource is addressed by, per locale.
 *
 * <p><b>Sparse on purpose.</b> A locale appears in {@code byLocale} only when it
 * renames the resource; a locale that does not rename it addresses it by the
 * canonical handle, which is the same nullable-wins-canonical rule every other
 * translated column follows. The adapter cannot densify this — it has no view of
 * the operator's locale list — so the fallback travels with the data rather than
 * being reconstructed by each caller.
 *
 * <p>{@link #in(String)} is why this is a record and not a bare {@code Map}: the
 * fallback is written once. A caller reaching for {@code byLocale.get(code)} on a
 * locale that does not rename the resource gets null and builds
 * {@code /pages/null}; there is no such shape here.
 */
public record LocalizedHandles(String canonical, Map<String, String> byLocale) {

    public LocalizedHandles {
        byLocale = Map.copyOf(byLocale);
    }

    /** The handle this locale addresses the resource by. */
    public String in(String locale) {
        return byLocale.getOrDefault(locale, canonical);
    }
}
