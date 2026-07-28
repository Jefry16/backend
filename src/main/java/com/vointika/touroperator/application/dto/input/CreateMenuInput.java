package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

public record CreateMenuInput(
        UUID callerUserId,
        UUID tourOperatorId,
        String handle,
        String title
) {
}
