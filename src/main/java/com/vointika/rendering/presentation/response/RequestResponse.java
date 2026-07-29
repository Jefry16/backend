package com.vointika.rendering.presentation.response;

/**
 * The {@code request} block — what the server decided about this request, as
 * opposed to what was asked for. Currently just the resolved locale, which the
 * theme must trust over the URL prefix it came from.
 */
public record RequestResponse(String locale) {}
