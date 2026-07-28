package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Slug;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A named storefront navigation menu ({@code main-menu}, {@code footer}, …).
 * The handle is the stable theme-facing identifier — unique per operator,
 * immutable; the title is the operator-facing label and the one editable
 * field. Items hang off the menu as a separate tree ({@link MenuItem}).
 */
public class Menu {

    private static final int TITLE_MAX_LENGTH = 120;

    private final UUID id;
    private final UUID tourOperatorId;
    private final Slug handle;
    private String title;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    /** Creates a brand-new menu. */
    public Menu(UUID id, UUID tourOperatorId, Slug handle, String title, UUID createdBy) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.handle = handle;
        this.title = validateTitle(title);
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Reconstitutes from persistence. */
    public Menu(UUID id,
                UUID tourOperatorId,
                Slug handle,
                String title,
                UUID createdBy,
                Instant createdAt,
                Instant updatedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.handle = handle;
        this.title = title;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Renames the menu (the handle is immutable). */
    public void rename(String newTitle) {
        this.title = validateTitle(newTitle);
        this.updatedAt = Instant.now();
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidFieldException("Menu title cannot be blank");
        }
        String trimmed = title.trim();
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Menu title must be at most " + TITLE_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    /** The audit-worthy fields (the handle identifies, the title is the editable one). */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("handle", handle.value());
        snapshot.put("title", title);
        return snapshot;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public Slug getHandle() { return handle; }
    public String getTitle() { return title; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
