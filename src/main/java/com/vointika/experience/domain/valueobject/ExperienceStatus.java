package com.vointika.experience.domain.valueobject;

/**
 * An experience's lifecycle. A new experience starts DRAFT; PUBLISHED is
 * shopper-visible. (No ARCHIVED — unpublish is the off-switch.)
 */
public enum ExperienceStatus {
    DRAFT,
    PUBLISHED
}
