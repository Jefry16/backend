package com.vointika.pickup.presentation.request;

/**
 * Create/update body for a pickup location. {@code time} is a local time string
 * (HH:mm). On update, a null field keeps the current value.
 */
public record PickupLocationRequest(String name, String time) {
}
