package com.vointika.identity.presentation.response;

import com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String context,
        String name,
        String avatarUrl,
        String language,
        List<TourOperatorMembershipView> tourOperators
) {
    public ProfileResponse(UUID id, String name, String avatarUrl, String language,
                           List<TourOperatorMembershipView> tourOperators) {
        this(id, "users", name, avatarUrl, language, tourOperators);
    }
}
