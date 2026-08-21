package com.vointika.experience.application.dto.input;

/**
 * The editable fields of a category — shared by create and update, and bound
 * directly by the controller (PATTERNS §4c: an identical presentation copy is
 * not a seam).
 *
 * <p><b>Nothing may annotate this record.</b> The application layer's allowlist
 * is {@code com.vointika..} plus {@code java..}, so a {@code @JsonProperty} or a
 * Jakarta constraint compiles and then fails ArchUnit.
 */
public record CategoryInput(String name) {
}
