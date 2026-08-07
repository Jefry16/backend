package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyJpaEntity;

public class TourOperatorPolicyMapper {

    public static TourOperatorPolicyJpaEntity toJpa(Policy p) {
        return new TourOperatorPolicyJpaEntity(
                p.id(), p.tourOperatorId(), p.type(),
                p.title().value(), p.body().value(),
                p.createdAt(), p.updatedAt());
    }

    public static Policy toDomain(TourOperatorPolicyJpaEntity jpa) {
        return new Policy(
                jpa.getId(), jpa.getTourOperatorId(), jpa.getType(),
                new PolicyTitle(jpa.getTitle()), new PolicyBody(jpa.getBody()),
                jpa.getCreatedAt(), jpa.getUpdatedAt());
    }

    private TourOperatorPolicyMapper() {}
}
