package com.vointika.shared.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.valueobject.LocaleCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A locale the operator actually publishes in, or a 422.
 *
 * <p><b>Seven upserts across five contexts ask this, and they must never
 * disagree.</b> A locale one translation endpoint accepts and another refuses is a
 * difference no client could explain, and until this existed nothing stopped it:
 * the same three lines and the same message were written out in
 * {@code touroperator} (operator and policy translations), {@code page},
 * {@code experience}, {@code audience} and {@code metafield} (metafield and
 * metaobject-field translations).
 *
 * <p><b>The message is a published contract</b> — it is the 422 body under every
 * translation upsert in the API guide — so the copies were seven statements of one
 * promise, none of which could be changed alone.
 *
 * <p><b>Only the upserts ask.</b> A read of an unpublished locale answers an empty
 * overlay and a delete answers 204. That asymmetry is deliberate and documented per
 * verb in the guide; see {@code PATTERNS.md} §9a.
 *
 * <p>It lives in {@code shared} rather than in a context because the rule belongs
 * to the operator's published locales, which is {@link OperatorLocalesQuery}'s
 * subject, and no one context owns translations.
 */
@Component
public class OperatorLocaleCheck {

    private final OperatorLocalesQuery operatorLocalesQuery;

    public OperatorLocaleCheck(OperatorLocalesQuery operatorLocalesQuery) {
        this.operatorLocalesQuery = operatorLocalesQuery;
    }

    /** The validated code, so a caller can use the return value instead of re-parsing. */
    public LocaleCode require(UUID tourOperatorId, String rawLocale) {
        LocaleCode locale = new LocaleCode(rawLocale);
        if (!operatorLocalesQuery.findSupportedLocales(tourOperatorId).contains(locale.value())) {
            throw new InvalidFieldException(refusal(locale.value()));
        }
        return locale;
    }

    /**
     * The refusal, public because it is <b>published</b>: four documentation tests
     * across four contexts stub this 422 and publish its body into the API guide.
     * Centralising the rule alone was not enough — a mutation proved it. Changing the
     * message here moved nothing in the guide, because each test spelled the sentence
     * itself.
     */
    public static String refusal(String locale) {
        return "Locale '" + locale + "' is not supported by this operator";
    }
}
