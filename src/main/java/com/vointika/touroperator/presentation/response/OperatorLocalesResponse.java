package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.OperatorLocalesView;

import java.util.List;

/** An operator's content-language settings. */
public record OperatorLocalesResponse(String primaryLocale, List<String> supportedLocales) {

    public static OperatorLocalesResponse from(OperatorLocalesView view) {
        return new OperatorLocalesResponse(view.primaryLocale(), view.supportedLocales());
    }
}
