package com.vointika.touroperator.presentation.request;

import java.util.List;

/** The operator's content languages: the default/primary locale + the supported set. */
public record UpdateOperatorLocalesRequest(String primaryLocale, List<String> supportedLocales) {
}
