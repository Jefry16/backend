package com.vointika.shared.valueobject;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditActorTest {

    @Test
    void userActorCarriesTheId() {
        UUID id = UUID.randomUUID();
        AuditActor actor = AuditActor.user(id);
        assertThat(actor.type()).isEqualTo(AuditActorType.USER);
        assertThat(actor.id()).isEqualTo(id);
    }

    @Test
    void systemActorCarriesNoId() {
        assertThat(AuditActor.system().id()).isNull();
    }

    @Test
    void userWithoutIdAndSystemWithIdAreRejected() {
        assertThatThrownBy(() -> new AuditActor(AuditActorType.USER, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditActor(AuditActorType.SYSTEM, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
