package com.vointika.metafield.application.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * A locale an operator actually publishes in, or a 422.
 *
 * <p><b>Only the upserts ask.</b> A read of an unpublished locale answers an empty
 * overlay and a delete answers 204, so this is deliberately not on the path every
 * translation endpoint takes — see PATTERNS §9a.
 *
 * <p>It exists because the two upserts held the same three lines and the same
 * message, and they are the two that must never disagree: a locale the metafield
 * upsert accepts and the metaobject-field upsert refuses would be a difference no
 * client could explain.
 */
public class OperatorLocaleCheck {

    private final OperatorLocalesQuery operatorLocalesQuery;

    public OperatorLocaleCheck(OperatorLocalesQuery operatorLocalesQuery) {
        this.operatorLocalesQuery = operatorLocalesQuery;
    }

    public LocaleCode require(UUID tourOperatorId, String rawLocale) {
        LocaleCode locale = new LocaleCode(rawLocale);
        if (!operatorLocalesQuery.findSupportedLocales(tourOperatorId).contains(locale.value())) {
            throw new InvalidFieldException(
                    "Locale '" + locale.value() + "' is not supported by this operator");
        }
        return locale;
    }
}
