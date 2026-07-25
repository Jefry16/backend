package com.vointika.pickup.application.dto.input;

/**
 * The editable fields of a pickup location. Create requires both; update is
 * PARTIAL — a null field means "keep the current value".
 */
public record PickupLocationInput(String name, String time) {
}
