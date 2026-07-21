package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.Tag;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Slug;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An operator's sellable product. The core aggregate: identity (name, immutable
 * per-operator {@link Slug}), prose, content lists, media references (bare
 * media-ids validated at the write boundary, resolved to URLs at read),
 * duration + booking cutoff, and a DRAFT↔PUBLISHED lifecycle. Slots, pricing,
 * pickup, and translations are separate aggregates/overlays.
 *
 * <p>Collection caps and the thumbnail∈gallery rule are aggregate invariants,
 * re-checked on every mutation.
 */
public class Experience {

    private static final int MAX_TAGS = 20;
    private static final int MAX_HIGHLIGHTS = 20;
    private static final int MAX_INCLUSION = 30;
    private static final int MAX_MEDIA = 20;

    private final UUID id;
    private final UUID tourOperatorId;
    private final UUID createdBy;
    private final Slug slug;                 // immutable, unique per operator
    private final Instant createdAt;

    private ExperienceName name;
    private Description description;
    private LongDescription longDescription;
    private boolean featured;
    private List<Tag> tags;
    private List<InclusionItem> included;
    private List<InclusionItem> notIncluded;
    private List<Highlight> highlights;
    private List<UUID> mediaIds;
    private UUID thumbnailMediaId;
    private DurationMinutes durationMinutes;
    private BookingCutoffHours bookingCutoffHours;
    private boolean published;

    /** A brand-new experience — always unpublished (a draft). */
    public static Experience create(UUID id, UUID tourOperatorId, UUID createdBy, Slug slug,
                                    ExperienceName name, Description description, LongDescription longDescription,
                                    boolean featured, List<Tag> tags, List<InclusionItem> included,
                                    List<InclusionItem> notIncluded, List<Highlight> highlights,
                                    List<UUID> mediaIds, UUID thumbnailMediaId,
                                    DurationMinutes durationMinutes, BookingCutoffHours bookingCutoffHours) {
        Experience e = new Experience(id, tourOperatorId, createdBy, slug, Instant.now(),
                name, description, longDescription, featured, tags, included, notIncluded, highlights,
                mediaIds, thumbnailMediaId, durationMinutes, bookingCutoffHours, false);
        e.validateInvariants();
        return e;
    }

    // Reconstitution from persistence.
    public Experience(UUID id, UUID tourOperatorId, UUID createdBy, Slug slug, Instant createdAt,
                      ExperienceName name, Description description, LongDescription longDescription,
                      boolean featured, List<Tag> tags, List<InclusionItem> included,
                      List<InclusionItem> notIncluded, List<Highlight> highlights,
                      List<UUID> mediaIds, UUID thumbnailMediaId,
                      DurationMinutes durationMinutes, BookingCutoffHours bookingCutoffHours,
                      boolean published) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.createdBy = createdBy;
        this.slug = slug;
        this.createdAt = createdAt;
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.featured = featured;
        this.tags = List.copyOf(tags);
        this.included = List.copyOf(included);
        this.notIncluded = List.copyOf(notIncluded);
        this.highlights = List.copyOf(highlights);
        this.mediaIds = List.copyOf(mediaIds);
        this.thumbnailMediaId = thumbnailMediaId;
        this.durationMinutes = durationMinutes;
        this.bookingCutoffHours = bookingCutoffHours;
        this.published = published;
    }

    /** Replaces the editable fields (everything but id/operator/slug/status/createdAt). */
    public void update(ExperienceName name, Description description, LongDescription longDescription,
                       boolean featured, List<Tag> tags, List<InclusionItem> included,
                       List<InclusionItem> notIncluded, List<Highlight> highlights,
                       List<UUID> mediaIds, UUID thumbnailMediaId,
                       DurationMinutes durationMinutes, BookingCutoffHours bookingCutoffHours) {
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.featured = featured;
        this.tags = List.copyOf(tags);
        this.included = List.copyOf(included);
        this.notIncluded = List.copyOf(notIncluded);
        this.highlights = List.copyOf(highlights);
        this.mediaIds = List.copyOf(mediaIds);
        this.thumbnailMediaId = thumbnailMediaId;
        this.durationMinutes = durationMinutes;
        this.bookingCutoffHours = bookingCutoffHours;
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
        if (tags.size() > MAX_TAGS) {
            throw new InvalidFieldException("At most " + MAX_TAGS + " tags are allowed");
        }
        if (highlights.size() > MAX_HIGHLIGHTS) {
            throw new InvalidFieldException("At most " + MAX_HIGHLIGHTS + " highlights are allowed");
        }
        if (included.size() > MAX_INCLUSION || notIncluded.size() > MAX_INCLUSION) {
            throw new InvalidFieldException("At most " + MAX_INCLUSION + " inclusion items are allowed");
        }
        if (mediaIds.size() > MAX_MEDIA) {
            throw new InvalidFieldException("At most " + MAX_MEDIA + " media items are allowed");
        }
        if (thumbnailMediaId != null && !mediaIds.contains(thumbnailMediaId)) {
            throw new InvalidFieldException("The thumbnail must be one of the experience's media items");
        }
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public UUID getCreatedBy() { return createdBy; }
    public Slug getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
    public ExperienceName getName() { return name; }
    public Description getDescription() { return description; }
    public LongDescription getLongDescription() { return longDescription; }
    public boolean isFeatured() { return featured; }
    public List<Tag> getTags() { return tags; }
    public List<InclusionItem> getIncluded() { return included; }
    public List<InclusionItem> getNotIncluded() { return notIncluded; }
    public List<Highlight> getHighlights() { return highlights; }
    public List<UUID> getMediaIds() { return mediaIds; }
    public UUID getThumbnailMediaId() { return thumbnailMediaId; }
    public DurationMinutes getDurationMinutes() { return durationMinutes; }
    public BookingCutoffHours getBookingCutoffHours() { return bookingCutoffHours; }
    public boolean isPublished() { return published; }
}
