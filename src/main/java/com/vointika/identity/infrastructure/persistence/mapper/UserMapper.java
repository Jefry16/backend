package com.vointika.identity.infrastructure.persistence.mapper;

import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;

public class UserMapper {

    public static UserJpaEntity toJpa(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getEmail().value(),
                user.getName().value(),
                user.getHashedPassword(),
                user.getStatus(),
                user.getAvatarKey(),
                user.getLanguage(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static User toDomain(UserJpaEntity jpa) {
        return new User(
                jpa.getId(),
                new Email(jpa.getEmail()),
                new UserName(jpa.getName()),
                jpa.getHashedPassword(),
                jpa.getStatus(),
                jpa.getAvatarKey(),
                jpa.getLanguage(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }
}
