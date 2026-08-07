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
 * <p>Identity is {@code (tourOperatorId, type)} and neither is mutable: retyping
 * a policy would move it to another URL, which is a delete and a create.
 */
public record Policy(
        UUID tourOperatorId,
        PolicyType type,
        PolicyTitle title,
        PolicyBody body,
        Instant createdAt,
        Instant updatedAt) {

    public Policy {
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
    }

    /** A policy written for the first time. */
    public static Policy write(UUID tourOperatorId, PolicyType type,
                               PolicyTitle title, PolicyBody body, Instant now) {
        return new Policy(tourOperatorId, type, title, body, now, now);
    }

    /** The same policy with new text — {@code createdAt} survives a rewrite. */
    public Policy rewrite(PolicyTitle newTitle, PolicyBody newBody, Instant now) {
        return new Policy(tourOperatorId, type, newTitle, newBody, createdAt, now);
    }
}
