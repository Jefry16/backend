package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.usecase.GetInvitationPreviewUseCase;

/** What the accept page needs before the form: which operator, to which email. */
public record InvitationPreviewResponse(
        String context,
        String operatorName,
        String email
) {
    public static InvitationPreviewResponse from(GetInvitationPreviewUseCase.Preview preview) {
        return new InvitationPreviewResponse(
                "invitation-previews", preview.operatorName(), preview.email());
    }
}
