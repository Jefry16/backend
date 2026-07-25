package com.vointika.pickup.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupLocationValueObjectsTest {

    @Test
    void nameTrimsAndAccepts() {
        assertThat(new PickupLocationName("  Old Port  ").value()).isEqualTo("Old Port");
    }

    @Test
    void nameRejectsBlankAndTooLong() {
        assertThatThrownBy(() -> new PickupLocationName("  ")).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new PickupLocationName("x".repeat(201))).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void nameRejectsControlCharacter() {
        assertThatThrownBy(() -> new PickupLocationName("Port\u0007")).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void timeParsesHourMinute() {
        assertThat(new PickupLocationTime("09:30").value()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void timeRejectsMissingOrMalformed() {
        assertThatThrownBy(() -> new PickupLocationTime((String) null)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new PickupLocationTime("  ")).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new PickupLocationTime("25:00")).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new PickupLocationTime((LocalTime) null)).isInstanceOf(InvalidFieldException.class);
    }
}
