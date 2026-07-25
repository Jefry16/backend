package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A bookable departure of an experience. Times are operator-LOCAL wall-clock
 * ({@link LocalDateTime}, no timezone) — a slot means "10:00 in the operator's
 * local time"; the tz is applied later (cutoff, display). Storing both endpoints
 * makes cross-midnight self-describing ({@code endAt}'s date is simply the next
 * day) and duration derivable ({@code endAt − startAt}), so nothing is inferred
 * from a bare time. Snapshots the experience name/description at creation so the
 * departure stays stable if the parent is later edited. Per-audience price +
 * capacity live on {@code audience_slot} rows. Immutable — transitions are
 * functional; a slot's timing is never edited (cancel + recreate instead).
 */
public class Slot {

    /** A slot may cross one midnight but not span multiple days. */
    private static final Duration MAX_SPAN = Duration.ofHours(24);

    private final UUID id;
    private final UUID experienceId;
    private final UUID tourOperatorId;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String experienceName;
    private final String experienceDescription;
    private final SlotStatus status;
    private final Instant createdAt;

    /** New departure (AVAILABLE). */
    public Slot(UUID id,
                UUID experienceId,
                UUID tourOperatorId,
                LocalDateTime startAt,
                LocalDateTime endAt,
                String experienceName,
                String experienceDescription) {
        this(id, experienceId, tourOperatorId, startAt, endAt,
                experienceName, experienceDescription, SlotStatus.AVAILABLE, Instant.now());
    }

    /** Reconstitution from persistence. */
    public Slot(UUID id,
                UUID experienceId,
                UUID tourOperatorId,
                LocalDateTime startAt,
                LocalDateTime endAt,
                String experienceName,
                String experienceDescription,
                SlotStatus status,
                Instant createdAt) {
        if (startAt == null || endAt == null) {
            throw new InvalidFieldException("Slot start and end are required");
        }
        if (!endAt.isAfter(startAt)) {
            throw new InvalidFieldException("Slot end must be after its start");
        }
        if (Duration.between(startAt, endAt).compareTo(MAX_SPAN) > 0) {
            throw new InvalidFieldException("A slot may last at most 24 hours");
        }
        this.id = id;
        this.experienceId = experienceId;
        this.tourOperatorId = tourOperatorId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.experienceName = experienceName;
        this.experienceDescription = experienceDescription;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Sets AVAILABLE or SOLD_OUT (functional — returns a copy). Rejects CANCELLED
     * (use {@link #cancel()}) and a change on an already-cancelled slot.
     */
    public Slot changeStatus(SlotStatus newStatus) {
        if (newStatus == SlotStatus.CANCELLED) {
            throw new InvalidFieldException("Cancel a slot via the cancel action");
        }
        if (this.status == SlotStatus.CANCELLED) {
            throw new ConflictException("A cancelled slot cannot change status");
        }
        return copyWithStatus(newStatus);
    }

    /** Cancels the departure (terminal). Already CANCELLED → 409. */
    public Slot cancel() {
        if (status == SlotStatus.CANCELLED) {
            throw new ConflictException("Slot is already cancelled");
        }
        return copyWithStatus(SlotStatus.CANCELLED);
    }

    private Slot copyWithStatus(SlotStatus newStatus) {
        return new Slot(id, experienceId, tourOperatorId, startAt, endAt,
                experienceName, experienceDescription, newStatus, createdAt);
    }

    public UUID id() { return id; }
    public UUID experienceId() { return experienceId; }
    public UUID tourOperatorId() { return tourOperatorId; }
    public LocalDateTime startAt() { return startAt; }
    public LocalDateTime endAt() { return endAt; }

    /** Day-of-week of the start, 0–6 Sunday-first — persisted for indexed weekday filtering. */
    public int day() {
        return startAt.getDayOfWeek().getValue() % 7;
    }

    public String experienceName() { return experienceName; }
    public String experienceDescription() { return experienceDescription; }
    public SlotStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
