package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;

import java.time.Instant;
import java.util.UUID;

/**
 * One item in a menu's tree. Self-referencing via {@code parentId}:
 * {@code null} = top-level, otherwise a child of another item of the same
 * menu. The schema permits arbitrary depth; {@link #MAX_DEPTH} is enforced by
 * the {@link #create} factory so every write path inherits the cap.
 *
 * <p>Link-type / payload invariants (validated in the factory):
 * <ul>
 *   <li>{@code EXPERIENCE}/{@code PAGE} require {@code resourceId}, forbid {@code url}.</li>
 *   <li>{@code EXTERNAL_URL} requires {@code url}, forbids {@code resourceId}.</li>
 *   <li>{@code HOME}/{@code EXPERIENCE_LIST} forbid both.</li>
 * </ul>
 */
public class MenuItem {

    public static final int MAX_DEPTH = 3;

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int URL_MAX_LENGTH = 2048;

    private final UUID id;
    private final UUID menuId;
    private final UUID parentId;
    private final String title;
    private final MenuItemLinkType linkType;
    private final UUID resourceId;
    private final String url;
    private final int position;
    private final Instant createdAt;
    private final Instant updatedAt;

    /** Reconstitutes from persistence. */
    public MenuItem(UUID id,
                    UUID menuId,
                    UUID parentId,
                    String title,
                    MenuItemLinkType linkType,
                    UUID resourceId,
                    String url,
                    int position,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.menuId = menuId;
        this.parentId = parentId;
        this.title = title;
        this.linkType = linkType;
        this.resourceId = resourceId;
        this.url = url;
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new item with full invariant validation. {@code parentDepth}
     * is the depth of the would-be parent (0 = top-level item, 1 = child of a
     * top-level item, …); the new item lives at {@code parentDepth + 1}.
     * {@code parentId} and {@code parentDepth} must agree: null parent ⇔
     * depth 0.
     */
    public static MenuItem create(UUID id,
                                  UUID menuId,
                                  UUID parentId,
                                  int parentDepth,
                                  String title,
                                  MenuItemLinkType linkType,
                                  UUID resourceId,
                                  String url,
                                  int position,
                                  Instant now) {
        if (title == null || title.isBlank()) {
            throw new InvalidFieldException("Menu item title cannot be blank");
        }
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > TITLE_MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Menu item title must be at most " + TITLE_MAX_LENGTH + " characters");
        }
        if (linkType == null) {
            throw new InvalidFieldException("Menu item link type is required");
        }
        if (position < 0) {
            throw new InvalidFieldException("Menu item position must be non-negative");
        }
        if (parentId == null && parentDepth != 0) {
            throw new InvalidFieldException("Top-level menu items must have parent depth 0");
        }
        if (parentId != null && parentDepth < 1) {
            throw new InvalidFieldException("Child menu items must have a parent depth of at least 1");
        }
        if (parentDepth + 1 > MAX_DEPTH) {
            throw new InvalidFieldException(
                    "Menu items can be nested at most " + MAX_DEPTH + " levels deep");
        }
        switch (linkType) {
            case EXPERIENCE, PAGE -> {
                if (resourceId == null) {
                    throw new InvalidFieldException(linkType.name() + " link requires a resourceId");
                }
                if (url != null) {
                    throw new InvalidFieldException(linkType.name() + " link must not carry a url");
                }
            }
            case EXTERNAL_URL -> {
                if (url == null || url.isBlank()) {
                    throw new InvalidFieldException("EXTERNAL_URL link requires a url");
                }
                if (url.length() > URL_MAX_LENGTH) {
                    throw new InvalidFieldException(
                            "Menu item url must be at most " + URL_MAX_LENGTH + " characters");
                }
                if (resourceId != null) {
                    throw new InvalidFieldException("EXTERNAL_URL link must not carry a resourceId");
                }
            }
            case HOME, EXPERIENCE_LIST -> {
                if (resourceId != null || url != null) {
                    throw new InvalidFieldException(
                            linkType.name() + " link must not carry a resourceId or url");
                }
            }
        }
        return new MenuItem(id, menuId, parentId, trimmedTitle, linkType, resourceId, url,
                position, now, now);
    }

    public UUID getId() { return id; }
    public UUID getMenuId() { return menuId; }
    public UUID getParentId() { return parentId; }
    public String getTitle() { return title; }
    public MenuItemLinkType getLinkType() { return linkType; }
    public UUID getResourceId() { return resourceId; }
    public String getUrl() { return url; }
    public int getPosition() { return position; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
