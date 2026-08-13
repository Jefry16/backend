package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

/**
 * The operator's own editable details. <b>PATCH semantics: null means
 * "unchanged"</b>, the shape {@code AudienceInput} set — a record cannot tell an
 * absent JSON field from an explicit null, so absence has to mean the safe
 * thing.
 *
 * <p>Clearing an optional field is therefore a <b>blank string</b>, not null:
 * {@code ""} on phone or email removes it. {@code name} and {@code address}
 * cannot be cleared, only replaced.
 *
 * <p><b>{@code address} is the exception to the blank rule, because it is an
 * object.</b> Omit it and the address is untouched; send it and it replaces the
 * stored one <em>whole</em>, optional parts included — sending only a city would
 * otherwise put a new city on an old street, which is a wrong address rather
 * than a partial one. The nesting is what makes the tri-state expressible: a
 * record cannot tell an absent field from an explicit null, but it can tell an
 * absent object.
 *
 * <p>{@code handle} is absent by design — it is the storefront subdomain.
 */
public record UpdateTourOperatorInput(
        String name,
        AddressInput address,
        String phone,
        String email,
        UUID timezoneId,
        UUID currencyId) {
}
