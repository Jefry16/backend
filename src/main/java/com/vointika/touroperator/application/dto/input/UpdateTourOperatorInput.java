package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

/**
 * The operator's own editable details. <b>PATCH semantics: null means
 * "unchanged"</b>, the shape {@code AudienceInput} set — a record cannot tell an
 * absent JSON field from an explicit null, so absence has to mean the safe
 * thing.
 *
 * <p>Clearing an optional field is therefore a <b>blank string</b>, not null:
 * {@code ""} on phone or email removes it. {@code name} and {@code address} are
 * required columns and cannot be cleared, only replaced.
 *
 * <p>{@code handle} is absent by design — it is the storefront subdomain.
 */
public record UpdateTourOperatorInput(
        String name,
        String address,
        String phone,
        String email,
        UUID timezoneId,
        UUID currencyId) {
}
