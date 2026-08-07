package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One of the operator's four store policies — cancellation, privacy, terms or
 * legal notice.
 *
 * <p><b>The type is the address</b>, which is what makes this entity so small.
 * A policy has no handle (the type names the URL), no publish state (the row's
 * absence <em>is</em> unpublished, so deleting one takes it off the storefront)
 * and no SEO override (its title is the title tag). Everything {@code Page}
 * needed a column and an endpoint for, this gets from its primary key.
 *
 * <p>The <b>identity</b> is still {@code (tourOperatorId, type)} — that pair is
 * UNIQUE and neither part is mutable, because retyping a policy would move it to
 * another URL, which is a delete and a create. The {@code id} is a surrogate the
 * list framework needs, not a second way to address a policy: no endpoint takes
 * one, since the type is the address.
 */
public record Policy(
        UUID id,
        UUID tourOperatorId,
        PolicyType type,
        PolicyTitle title,
        PolicyBody body,
        Instant createdAt,
        Instant updatedAt) {

    public Policy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
    }

    /** A policy written for the first time. */
    public static Policy write(UUID id, UUID tourOperatorId, PolicyType type,
                               PolicyTitle title, PolicyBody body, Instant now) {
        return new Policy(id, tourOperatorId, type, title, body, now, now);
    }

    /** The same policy with new text — its id and {@code createdAt} survive a rewrite. */
    public Policy rewrite(PolicyTitle newTitle, PolicyBody newBody, Instant now) {
        return new Policy(id, tourOperatorId, type, newTitle, newBody, createdAt, now);
    }
}
