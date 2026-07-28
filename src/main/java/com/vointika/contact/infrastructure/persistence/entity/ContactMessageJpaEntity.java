package com.vointika.contact.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "contact", name = "contact_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(updatable = false, length = 120)
    private String name;

    @Column(nullable = false, updatable = false, length = 320)
    private String email;

    @Column(nullable = false, updatable = false, length = 200)
    private String summary;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    /** Inbox read-state; null = unread. The row's only mutable column. */
    @Column
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
