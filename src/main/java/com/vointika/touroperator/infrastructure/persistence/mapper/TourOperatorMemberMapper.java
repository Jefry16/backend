package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;

public class TourOperatorMemberMapper {

    public static TourOperatorMemberJpaEntity toJpa(TourOperatorMember member) {
        return new TourOperatorMemberJpaEntity(
                member.getId(),
                member.getTourOperatorId(),
                member.getUserId(),
                member.getRole(),
                member.isDefault(),
                member.getJoinedAt(),
                member.getName(),
                member.getEmail()
        );
    }

    public static TourOperatorMember toDomain(TourOperatorMemberJpaEntity jpa) {
        return new TourOperatorMember(
                jpa.getId(),
                jpa.getTourOperatorId(),
                jpa.getUserId(),
                jpa.getRole(),
                jpa.isDefault(),
                jpa.getJoinedAt(),
                jpa.getName(),
                jpa.getEmail()
        );
    }
}
