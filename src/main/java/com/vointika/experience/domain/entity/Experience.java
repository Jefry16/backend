package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Handle;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An operator's sellable product. The core aggregate: identity (name, immutable
 * per-operator {@link Handle}), prose, media references (bare media-ids validated
 * at the write boundary, resolved to URLs at read), the booking cutoff, and a
 * DRAFT↔PUBLISHED lifecycle. Slots, pricing, pickup, and translations are
 * separate aggregates/overlays.
 *
 * <p>Collection caps and the thumbnail∈gallery rule are aggregate invariants,
 * re-checked on every mutation.
 */
public class Experience {

    private static final int MAX_MEDIA = 20;

    private final UUID id;
    private final UUID tourOperatorId;
    private final UUID createdBy;
    private final Handle handle;                 // immutable, unique per operator
    private final Instant createdAt;

    private ExperienceName name;
    private Description description;
    private LongDescription longDescription;
    private boolean featured;
    private List<UUID> mediaIds;
    private UUID thumbnailMediaId;
    private BookingCutoffHours bookingCutoffHours;
    private boolean published;
    /**
     * The operator's own classification, or null for uncategorized — which is a
     * legitimate state, not a missing value, and the one every experience is in
     * until someone files it. Ownership is checked at the write boundary; the
     * column is {@code ON DELETE SET NULL}, so deleting the category returns the
     * experience here rather than taking it with it.
     */
    private UUID categoryId;
    /** Optional SEO overrides; null means "no override" and the render falls back. */
    private SeoTitle seoTitle;
    private SeoDescription seoDescription;

    /**
     * The advertised "from" price — **the operator's claim, not a MIN over the
     * slots**, so nothing keeps it in step with them. Required and greater than
     * zero on every experience, drafts included, so the card always has a figure
     * and there is no "unpriced" state to render around.
     */
    private Price startingPrice;

    /** A brand-new experience — always unpublished (a draft). */
    public static Experience create(UUID id, UUID tourOperatorId, UUID createdBy, Handle handle,
                                    ExperienceName name, Description description, LongDescription longDescription,
                                    boolean featured, List<UUID> mediaIds, UUID thumbnailMediaId,
                                    BookingCutoffHours bookingCutoffHours,
                                    SeoTitle seoTitle, SeoDescription seoDescription,
                                    Price startingPrice, UUID categoryId) {
        Experience e = new Experience(id, tourOperatorId, createdBy, handle, Instant.now(),
                name, description, longDescription, featured,
                mediaIds, thumbnailMediaId, bookingCutoffHours, false,
                seoTitle, seoDescription, startingPrice, categoryId);
        e.validateInvariants();
        return e;
    }

    // Reconstitution from persistence.
    public Experience(UUID id, UUID tourOperatorId, UUID createdBy, Handle handle, Instant createdAt,
                      ExperienceName name, Description description, LongDescription longDescription,
                      boolean featured, List<UUID> mediaIds, UUID thumbnailMediaId,
                      BookingCutoffHours bookingCutoffHours,
                      boolean published, SeoTitle seoTitle, SeoDescription seoDescription,
                      Price startingPrice, UUID categoryId) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.createdBy = createdBy;
        this.handle = handle;
        this.createdAt = createdAt;
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.featured = featured;
        this.mediaIds = List.copyOf(mediaIds);
        this.thumbnailMediaId = thumbnailMediaId;
        this.bookingCutoffHours = bookingCutoffHours;
        this.published = published;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.startingPrice = startingPrice;
        this.categoryId = categoryId;
    }

    /** Replaces the editable fields (everything but id/operator/handle/status/createdAt). */
    public void update(ExperienceName name, Description description, LongDescription longDescription,
                       boolean featured, List<UUID> mediaIds, UUID thumbnailMediaId,
                       BookingCutoffHours bookingCutoffHours,
                       SeoTitle seoTitle, SeoDescription seoDescription,
                       Price startingPrice, UUID categoryId) {
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.featured = featured;
        this.mediaIds = List.copyOf(mediaIds);
        this.thumbnailMediaId = thumbnailMediaId;
        this.bookingCutoffHours = bookingCutoffHours;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.startingPrice = startingPrice;
        this.categoryId = categoryId;
        validateInvariants();
    }

    /** Publishes (unpublished → published). Publishing an already-published experience conflicts. */
    public void publish() {
        if (published) {
            throw new ConflictException("Experience is already published");
        }
        this.published = true;
    }

    /** Unpublishes (published → draft). Unpublishing a draft conflicts. */
    public void unpublish() {
        if (!published) {
            throw new ConflictException("Experience is already a draft");
        }
        this.published = false;
    }

    private void validateInvariants() {
        if (mediaIds.size() > MAX_MEDIA) {
            throw new InvalidFieldException("At most " + MAX_MEDIA + " media items are allowed");
        }
        if (thumbnailMediaId != null && !mediaIds.contains(thumbnailMediaId)) {
            throw new InvalidFieldException("The thumbnail must be one of the experience's media items");
        }
        // Not on Price itself: audience_slot uses the same type and a free tier
        // priced at 0 is legitimate there.
        if (startingPrice == null || startingPrice.value().signum() <= 0) {
            throw new InvalidFieldException("Starting price must be greater than 0");
        }
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public UUID getCreatedBy() { return createdBy; }
    /**
     * The auditable fields as JSON-native values — the exposure guard for the
     * audit log's field diffs (only what appears here can enter the trail).
     * Handle is immutable and status changes only via publish/unpublish, so
     * neither belongs in the update diff; {@code published} is diffed by the
     * publish/unpublish use cases against this same snapshot.
     */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name.value());
        snapshot.put("description", description.value());
        snapshot.put("longDescription", longDescription.value());
        snapshot.put("featured", featured);
        snapshot.put("mediaIds", mediaIds.stream().map(UUID::toString).toList());
        snapshot.put("thumbnailMediaId", thumbnailMediaId == null ? null : thumbnailMediaId.toString());
        snapshot.put("bookingCutoffHours", bookingCutoffHours.value());
        snapshot.put("startingPrice", startingPrice.value().toPlainString());
        snapshot.put("categoryId", categoryId == null ? null : categoryId.toString());
        snapshot.put("published", published);
        return snapshot;
    }

    public Handle getHandle() { return handle; }
    public Instant getCreatedAt() { return createdAt; }
    public ExperienceName getName() { return name; }
    public Description getDescription() { return description; }
    public LongDescription getLongDescription() { return longDescription; }
    public boolean isFeatured() { return featured; }
    public List<UUID> getMediaIds() { return mediaIds; }
    public UUID getThumbnailMediaId() { return thumbnailMediaId; }
    public BookingCutoffHours getBookingCutoffHours() { return bookingCutoffHours; }
    public boolean isPublished() { return published; }

    public SeoTitle getSeoTitle() { return seoTitle; }
    public SeoDescription getSeoDescription() { return seoDescription; }
    public Price getStartingPrice() { return startingPrice; }
    public UUID getCategoryId() { return categoryId; }
}