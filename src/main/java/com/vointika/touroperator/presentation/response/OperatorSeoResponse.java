package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.OperatorSeoView;

import java.util.UUID;

/** The operator's canonical SEO defaults. Settings sub-resource shape — no id/context. */
public record OperatorSeoResponse(String seoTitle, String seoDescription, UUID ogImageMediaId) {

    public static OperatorSeoResponse from(OperatorSeoView view) {
        return new OperatorSeoResponse(view.seoTitle(), view.seoDescription(), view.ogImageMediaId());
    }
}
