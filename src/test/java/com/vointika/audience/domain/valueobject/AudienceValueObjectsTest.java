package com.vointika.audience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceValueObjectsTest {

    @Test
    void audienceNameTrimsAndAccepts() {
        assertThat(new AudienceName("  Adults  ").value()).isEqualTo("Adults");
    }

    @Test
    void audienceNameRejectsBlank() {
        assertThatThrownBy(() -> new AudienceName("   "))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void audienceNameRejectsTooLong() {
        assertThatThrownBy(() -> new AudienceName("x".repeat(81)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void audienceNameRejectsControlCharacter() {
        assertThatThrownBy(() -> new AudienceName("Adults\u0007"))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void paxPerUnitAcceptsPositive() {
        assertThat(new PaxPerUnit(6).value()).isEqualTo(6);
    }

    @Test
    void paxPerUnitDefaultsNullToOne() {
        assertThat(new PaxPerUnit((Integer) null).value()).isEqualTo(1);
    }

    @Test
    void paxPerUnitRejectsZeroOrNegative() {
        assertThatThrownBy(() -> new PaxPerUnit(0)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new PaxPerUnit(-2)).isInstanceOf(InvalidFieldException.class);
    }
}
