package com.vointika.contact.domain.entity;

import com.vointika.contact.domain.valueobject.ContactContent;
import com.vointika.contact.domain.valueobject.ContactEmail;
import com.vointika.contact.domain.valueobject.ContactName;
import com.vointika.contact.domain.valueobject.ContactSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * One shopper-submitted contact-form message. Written once by the intake
 * (the storefront arc's internal endpoint; dev-seeded until then) and never
 * edited — the only mutable bit is the inbox read-state ({@code readAt},
 * null = unread), which is workflow bookkeeping, not content.
 *
 * <p>Validation value objects (email/summary/content caps) arrive with the
 * intake use case — this admin-only slice reconstitutes and reads.
 */
public class ContactMessage {

    private final UUID id;
    private final UUID tourOperatorId;
    private final String name;
    private final String email;
    private final String summary;
    private final String content;
    private Instant readAt;
    private final Instant createdAt;

    public ContactMessage(UUID id,
                          UUID tourOperatorId,
                          String name,
                          String email,
                          String summary,
                          String content,
                          Instant readAt,
                          Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.name = name;
        this.email = email;
        this.summary = summary;
        this.content = content;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    /**
     * A message arriving from a storefront contact form.
     *
     * <p>The value objects validate on the way in — this is the first writer the
     * inbox has ever had, which is why they exist now and did not before. It
     * arrives unread: the whole point of the inbox is that someone still has to
     * look at it.
     */
    public static ContactMessage submit(UUID id,
                                        UUID tourOperatorId,
                                        ContactName name,
                                        ContactEmail email,
                                        ContactSummary summary,
                                        ContactContent content,
                                        Instant submittedAt) {
        return new ContactMessage(
                id,
                tourOperatorId,
                name == null ? null : name.value(),
                email.value(),
                summary.value(),
                content.value(),
                null,
                submittedAt);
    }

    /** Marks read (idempotent — re-reading keeps the first read timestamp). */
    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    /** Marks unread (idempotent). */
    public void markUnread() {
        readAt = null;
    }

    public boolean isRead() { return readAt != null; }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
