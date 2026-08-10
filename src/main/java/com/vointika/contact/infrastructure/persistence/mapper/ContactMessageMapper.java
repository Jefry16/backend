package com.vointika.contact.infrastructure.persistence.mapper;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;

public final class ContactMessageMapper {

    private ContactMessageMapper() {
    }

    public static ContactMessage toDomain(ContactMessageJpaEntity entity) {
        return new ContactMessage(
                entity.getId(), entity.getTourOperatorId(), entity.getName(),
                entity.getEmail(), entity.getSummary(), entity.getContent(),
                entity.getCreatedAt());
    }
}
