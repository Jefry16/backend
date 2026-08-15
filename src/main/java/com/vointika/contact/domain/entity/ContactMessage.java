package com.vointika.contact.domain.entity;

import com.vointika.contact.domain.valueobject.ContactContent;
import com.vointika.contact.domain.valueobject.ContactEmail;
import com.vointika.contact.domain.valueobject.ContactName;
import com.vointika.contact.domain.valueobject.ContactSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * One visitor-submitted contact-form message. Written once by the intake
 * (the storefront arc's internal endpoint; dev-seeded until then) and never
 * edited — the entity is immutable once written.
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
    private final Instant createdAt;

    public ContactMessage(UUID id,
                          UUID tourOperatorId,
                          String name,
                          String email,
                          String summary,
                          String content,
                          Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.name = name;
        this.email = email;
        this.summary = summary;
        this.content = content;
        this.createdAt = createdAt;
    }

    /**
     * A message arriving from a storefront contact form.
     *
     * <p>The value objects validate on the way in — this is the first writer the
     * inbox has ever had, which is why they exist now and did not before. It
     * arrives unread: the whole point of the inbox is that someone still has to
     * look at it.
     *
     * <p>{@code name} is required — {@link ContactName} refuses a blank one, so
     * every row names whoever wrote it.
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
                name.value(),
                email.value(),
                summary.value(),
                content.value(),
                submittedAt);
    }


    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
