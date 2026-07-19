package com.vointika.identity.infrastructure.persistence.mapper;

import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;

public class PasswordResetTokenMapper {

    public static PasswordResetTokenJpaEntity toJpa(PasswordResetToken token) {
        return new PasswordResetTokenJpaEntity(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getStatus(),
                token.getExpiresAt(),
                token.getCreatedAt()
        );
    }

    public static PasswordResetToken toDomain(PasswordResetTokenJpaEntity jpa) {
        return PasswordResetToken.rehydrate(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getTokenHash(),
                jpa.getStatus(),
                jpa.getExpiresAt(),
                jpa.getCreatedAt()
        );
    }
}
