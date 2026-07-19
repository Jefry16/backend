package com.vointika.identity.infrastructure.persistence.mapper;

import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;

public class RefreshTokenMapper {

    public static RefreshTokenJpaEntity toJpa(RefreshToken token) {
        return new RefreshTokenJpaEntity(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getFamilyId(),
                token.getReplacedById(),
                token.isRevoked(),
                token.getExpiresAt(),
                token.getCreatedAt()
        );
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        return RefreshToken.rehydrate(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getTokenHash(),
                jpa.getFamilyId(),
                jpa.getReplacedById(),
                jpa.isRevoked(),
                jpa.getExpiresAt(),
                jpa.getCreatedAt()
        );
    }
}
