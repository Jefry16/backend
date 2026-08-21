package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.CategoryName;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An operator's own classification for its experiences ("Boat tours", "Walking
 * tours"). Operator-scoped, referenced by an experience through a nullable
 * {@code category_id} — an uncategorized experience is a legitimate state, so
 * deleting a category leaves its experiences behind rather than taking them.
 *
 * <p>It carries a name and nothing else. There is deliberately no handle: a
 * handle is a permanent URL, and nothing routes to a category yet (LAW §2.4).
 * The day a storefront category page lands, that is the slice that adds one —
 * along with the handle-history question every other permanent URL here has.
 */
public class Category {

    private final UUID id;
    private final UUID tourOperatorId;
    private CategoryName name;
    private final Instant createdAt;

    /** A brand-new category. */
    public Category(UUID id, UUID tourOperatorId, CategoryName name) {
        this(id, tourOperatorId, name, Instant.now());
    }

    /** Reconstitution from persistence. */
    public Category(UUID id, UUID tourOperatorId, CategoryName name, Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public void rename(CategoryName newName) {
        this.name = newName;
    }

    /**
     * The auditable fields as JSON-native values — the exposure guard for the
     * audit log's field diffs (only what appears here can enter the trail).
     */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name.value());
        return snapshot;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public CategoryName getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
