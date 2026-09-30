package com.klabis.common.bootstrap;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "example-data"})
@Import(TestApplicationConfiguration.class)
@DisplayName("example-data legal guardian scenarios")
class SampleDataLegalGuardiansTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LegalGuardianGroupPort groupPort;

    @Test
    void siblingsOfNonMemberGuardianShareGroup() {
        Set<UserId> adam = guardiansOf("Adam");
        assertThat(adam).hasSize(1);
        assertThat(guardiansOf("Klára")).isEqualTo(adam);
    }

    @Test
    void minorOfMemberGuardianHasThatMember() {
        assertThat(guardiansOf("Sofie")).containsExactly(memberNamed("Eva").getId().toUserId());
    }

    @Test
    void minorWithTwoGuardiansHasMemberAndNonMember() {
        Set<UserId> guardians = guardiansOf("Matyáš");
        assertThat(guardians).hasSize(2).contains(memberNamed("Ondřej").getId().toUserId());
    }

    @Test
    void importedMinorHasNoGuardian() {
        assertThat(guardiansOf("Vojtěch")).isEmpty();
    }

    private Set<UserId> guardiansOf(String firstName) {
        return groupPort.guardiansOf(memberNamed(firstName).getId());
    }

    private Member memberNamed(String firstName) {
        return memberRepository.findAll().stream()
                .filter(m -> m.getFirstName().equals(firstName))
                .findFirst().orElseThrow();
    }
}
