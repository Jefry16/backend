package com.vointika.identity.application.dto.output;

import com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView;

import java.util.List;
import java.util.UUID;

public record GetProfileOutput(
        UUID id,
        String name,
        String avatarKey,
        String language,
        List<TourOperatorMembershipView> tourOperators
) {}
