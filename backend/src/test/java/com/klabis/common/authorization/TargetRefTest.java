package com.klabis.common.authorization;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetRefTest {

    @Test
    void shouldCreateTypedRefs() {
        UUID id = UUID.randomUUID();

        assertThat(TargetRef.member(id)).isEqualTo(new TargetRef(TargetType.MEMBER, id));
        assertThat(TargetRef.event(id)).isEqualTo(new TargetRef(TargetType.EVENT, id));
        assertThat(TargetRef.member(id)).isNotEqualTo(TargetRef.event(id));
    }

    @Test
    void shouldRejectTargetTypeNone() {
        assertThatThrownBy(() -> new TargetRef(TargetType.NONE, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingParts() {
        assertThatThrownBy(() -> new TargetRef(null, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TargetRef(TargetType.MEMBER, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
