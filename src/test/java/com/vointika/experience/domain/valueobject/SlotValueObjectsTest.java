package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotValueObjectsTest {

    @Test
    void priceRoundsToTwoDecimals() {
        assertThat(new Price(new BigDecimal("10.005")).value()).isEqualByComparingTo("10.01");
    }

    @Test
    void priceRejectsNullNegativeAndTooLarge() {
        assertThatThrownBy(() -> new Price(null)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new Price(new BigDecimal("-1"))).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new Price(new BigDecimal("10000000000"))).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void capacityAcceptsInRange() {
        assertThat(new Capacity(50).value()).isEqualTo(50);
    }

    @Test
    void capacityRejectsOutOfRangeAndNull() {
        assertThatThrownBy(() -> new Capacity(0)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new Capacity(100_001)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> new Capacity((Integer) null)).isInstanceOf(InvalidFieldException.class);
    }
}
