package com.vointika.identity.infrastructure.persistence.mapper;

import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;

public class VerificationTokenMapper {

    public static VerificationTokenJpaEntity toJpa(VerificationToken token) {
        return new VerificationTokenJpaEntity(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getStatus(),
                token.getExpiresAt(),
                token.getCreatedAt()
        );
    }

    public static VerificationToken toDomain(VerificationTokenJpaEntity jpa) {
        return VerificationToken.rehydrate(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getTokenHash(),
                jpa.getStatus(),
                jpa.getExpiresAt(),
                jpa.getCreatedAt()
        );
    }
}
