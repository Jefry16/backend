package com.vointika.contact.presentation.response;

import com.vointika.contact.domain.entity.ContactMessage;

import java.time.Instant;
import java.util.UUID;

/** {@code id} + {@code context:"contact-messages"} per the house rule. */
public record ContactMessageResponse(
        UUID id,
        String context,
        String name,
        String email,
        String summary,
        String content,
        Instant createdAt) {

    public static ContactMessageResponse from(ContactMessage message) {
        return new ContactMessageResponse(
                message.getId(), "contact-messages", message.getName(),
                message.getEmail(), message.getSummary(), message.getContent(),
                message.getCreatedAt());
    }
}
