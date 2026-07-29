package com.vointika.contact.presentation.response;

import com.vointika.contact.domain.entity.ContactMessage;

import java.time.Instant;
import java.util.UUID;

/** {@code id} + {@code context:"contact-messages"} per the house rule. */
public record ContactMessageListItemResponse(
        UUID id,
        String context,
        String name,
        String email,
        String summary,
        boolean read,
        Instant createdAt) {

    public static ContactMessageListItemResponse from(ContactMessage message) {
        return new ContactMessageListItemResponse(
                message.getId(), "contact-messages", message.getName(),
                message.getEmail(), message.getSummary(), message.isRead(),
                message.getCreatedAt());
    }
}
