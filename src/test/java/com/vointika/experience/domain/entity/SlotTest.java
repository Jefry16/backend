package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID EXP = UUID.randomUUID();
    private static final UUID OP = UUID.randomUUID();

    private Slot slot(LocalDateTime start, LocalDateTime end) {
        return new Slot(ID, EXP, OP, start, end, "Tour", "Desc");
    }

    @Test
    void rejectsEndNotAfterStart() {
        LocalDateTime t = LocalDateTime.of(2026, 8, 1, 10, 0);
        assertThatThrownBy(() -> slot(t, t)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> slot(t, t.minusHours(1))).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void rejectsSpanOver24Hours() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);
        assertThatThrownBy(() -> slot(start, start.plusHours(25)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void allowsCrossMidnightWithin24h() {
        Slot s = slot(LocalDateTime.of(2026, 8, 1, 22, 0), LocalDateTime.of(2026, 8, 2, 1, 0));
        assertThat(s.status()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(s.endAt()).isEqualTo(LocalDateTime.of(2026, 8, 2, 1, 0));
    }

    @Test
    void derivesDaySundayFirst() {
        // 2026-08-01 is a Saturday -> 6
        Slot s = slot(LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0));
        assertThat(s.day()).isEqualTo(6);
    }

    @Test
    void cancelIsTerminalAndIdempotent409() {
        Slot s = slot(LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0));
        Slot cancelled = s.cancel();
        assertThat(cancelled.status()).isEqualTo(SlotStatus.CANCELLED);
        assertThatThrownBy(cancelled::cancel).isInstanceOf(ConflictException.class);
    }
}
