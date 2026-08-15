package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The name is required, and blank counts as absent.
 *
 * <p>It was optional until 2026-08-15. The column is now {@code NOT NULL}
 * (contact/V3) for a reason the type has to keep true: a nullable column here is
 * invisible to a negative filter, because {@code NOT (NULL LIKE 'x')} is UNKNOWN
 * and {@code WHERE} keeps only true rows. Let a blank through and it is stored,
 * which puts the same hole back one layer down.
 */
class ContactNameTest {

    @Test
    void trimsTheName() {
        assertThat(new ContactName("  Ana Ruiz  ").value()).isEqualTo("Ana Ruiz");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new ContactName(null))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Name is required");
    }

    /** Whitespace is not a name. Storing it would be the null wearing a disguise. */
    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> new ContactName("   "))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Name is required");
    }

    @Test
    void rejectsAnOverlongName() {
        assertThatThrownBy(() -> new ContactName("a".repeat(121)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("at most 120");
    }

    /** The boundary itself is allowed — 120 is the column width, not one past it. */
    @Test
    void acceptsExactlyTheMaximum() {
        assertThat(new ContactName("a".repeat(120)).value()).hasSize(120);
    }

    /** Trimming happens before the length check, so trailing spaces cannot fail it. */
    @Test
    void trimsBeforeMeasuring() {
        assertThat(new ContactName("a".repeat(120) + "   ").value()).hasSize(120);
    }
}
