package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvocationRules")
class InvocationRulesTest {

    static class Operations {

        @HasAuthority({Authority.MEMBERS_READ, Authority.EVENTS_EDIT})
        public void memberAndEvent(@TargetId(TargetType.MEMBER) UUID memberId, @TargetId(TargetType.EVENT) UUID eventId) {
        }

        public void twoMemberTargets(@TargetId(TargetType.MEMBER) UUID first, @TargetId(TargetType.MEMBER) UUID second) {
        }

        public void untargeted(UUID id) {
        }
    }

    @Test
    void shouldKeepOneTargetParameterPerKind() throws NoSuchMethodException {
        var method = Operations.class.getMethod("memberAndEvent", UUID.class, UUID.class);

        var rules = InvocationRules.of(method, Operations.class);

        assertThat(rules.targets()).containsOnlyKeys(TargetType.MEMBER, TargetType.EVENT);
        assertThat(rules.targetOfKind(TargetType.MEMBER)).isEqualTo(new TargetParameter(0, TargetType.MEMBER));
        assertThat(rules.targetOfKind(TargetType.EVENT)).isEqualTo(new TargetParameter(1, TargetType.EVENT));
        assertThat(rules.targetOfKind(TargetType.GROUP)).isNull();
    }

    @Test
    void shouldHaveNoTargetsWithoutTargetIdParameters() throws NoSuchMethodException {
        var rules = InvocationRules.of(Operations.class.getMethod("untargeted", UUID.class), Operations.class);

        assertThat(rules.targets()).isEmpty();
    }

    @Test
    void shouldRejectTwoTargetParametersOfTheSameKind() throws NoSuchMethodException {
        var method = Operations.class.getMethod("twoMemberTargets", UUID.class, UUID.class);

        assertThatThrownBy(() -> InvocationRules.of(method, Operations.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEMBER");
    }
}
