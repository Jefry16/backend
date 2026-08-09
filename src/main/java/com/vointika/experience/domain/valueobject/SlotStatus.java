package com.vointika.experience.domain.valueobject;

/**
 * A departure's sellability. CANCELLED is terminal, set through the cancel
 * endpoint.
 *
 * <p><b>Nothing writes SOLD_OUT yet, and that is not an oversight.</b> It is
 * derived, not chosen: a departure is full when the bookings say so, counted at
 * checkout success. Until checkout exists there is no writer, so the only
 * SOLD_OUT rows are the dev seed's — which is enough for the read paths that
 * render the state. It was briefly operator-writeable through the slot PATCH;
 * that was removed, because a hand-set flag and a booking-derived one disagree
 * the first time a cancellation frees a seat.
 */
public enum SlotStatus {
    AVAILABLE,
    SOLD_OUT,
    CANCELLED
}
