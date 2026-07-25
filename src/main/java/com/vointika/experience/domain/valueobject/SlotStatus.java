package com.vointika.experience.domain.valueobject;

/**
 * A departure's sellability. AVAILABLE (open for sale) and SOLD_OUT are both
 * operator-writeable — with no booking system yet to auto-flip a full slot, the
 * operator marks SOLD_OUT manually. CANCELLED is terminal (via the cancel path).
 */
public enum SlotStatus {
    AVAILABLE,
    SOLD_OUT,
    CANCELLED
}
