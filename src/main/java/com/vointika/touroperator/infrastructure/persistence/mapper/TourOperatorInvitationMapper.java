package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.shared.valueobject.Email;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorInvitationJpaEntity;

public class TourOperatorInvitationMapper {

    public static TourOperatorInvitationJpaEntity toJpa(TourOperatorInvitation invitation) {
        return new TourOperatorInvitationJpaEntity(
                invitation.getId(),
                invitation.getTourOperatorId(),
                invitation.getEmail().value(),
                invitation.getName().value(),
                invitation.getRole(),
                invitation.getTokenHash(),
                invitation.getStatus(),
                invitation.getInvitedByUserId(),
                invitation.getInvitedByName(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt()
        );
    }

    public static TourOperatorInvitation toDomain(TourOperatorInvitationJpaEntity jpa) {
        return new TourOperatorInvitation(
                jpa.getId(),
                jpa.getTourOperatorId(),
                new Email(jpa.getEmail()),
                new InviteeName(jpa.getName()),
                jpa.getRole(),
                jpa.getTokenHash(),
                jpa.getStatus(),
                jpa.getInvitedByUserId(),
                jpa.getInvitedByName(),
                jpa.getCreatedAt(),
                jpa.getExpiresAt(),
                jpa.getAcceptedAt()
        );
    }
}
