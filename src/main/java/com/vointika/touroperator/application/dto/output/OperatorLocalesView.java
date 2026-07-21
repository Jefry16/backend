package com.vointika.touroperator.application.dto.output;

import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;

import java.util.List;

/**
 * An operator's content-language settings: its default/primary locale and the
 * full supported set (sorted for a stable response).
 */
public record OperatorLocalesView(String primaryLocale, List<String> supportedLocales) {

    public static OperatorLocalesView from(TourOperator operator) {
        return new OperatorLocalesView(
                operator.getPrimaryLocale().value(),
                operator.getSupportedLocales().stream().map(LocaleCode::value).sorted().toList());
    }
}
