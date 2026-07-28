package com.vointika.contact.infrastructure.persistence.mapper;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;

public final class ContactMessageMapper {

    private ContactMessageMapper() {
    }

    public static ContactMessageJpaEntity toJpa(ContactMessage message) {
        return new ContactMessageJpaEntity(
                message.getId(), message.getTourOperatorId(), message.getName(),
                message.getEmail(), message.getSummary(), message.getContent(),
                message.getReadAt(), message.getCreatedAt());
    }

    public static ContactMessage toDomain(ContactMessageJpaEntity entity) {
        return new ContactMessage(
                entity.getId(), entity.getTourOperatorId(), entity.getName(),
                entity.getEmail(), entity.getSummary(), entity.getContent(),
                entity.getReadAt(), entity.getCreatedAt());
    }
}
