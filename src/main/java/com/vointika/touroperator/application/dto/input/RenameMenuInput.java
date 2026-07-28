package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

public record RenameMenuInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID menuId,
        String title
) {
}
