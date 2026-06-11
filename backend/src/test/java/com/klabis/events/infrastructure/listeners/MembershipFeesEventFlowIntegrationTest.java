package com.klabis.events.infrastructure.listeners;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.events.domain.MemberRegistrationBlockRepository;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.FinanceAccountLinkSupport;
import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-module event flow integration tests for the membership-fees → events integration.
 *
 * <p>Verifies end-to-end event processing via Spring Modulith's transactional outbox:
 * <ul>
 *   <li>{@link MemberMissedFeeSelectionEvent} → member registration gets blocked</li>
 *   <li>{@link MemberFeeSelectionResolvedEvent} → member registration block gets lifted</li>
 * </ul>
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE, extraIncludes = "membershipfees")
@ActiveProfiles("test")
@CleanupTestData
@Import(TestApplicationConfiguration.class)
@DisplayName("membership-fees → events cross-module event flow")
class MembershipFeesEventFlowIntegrationTest {

    private static final MemberId SANCTION_MEMBER = new MemberId(UUID.fromString("ee000001-0000-0000-0000-000000000000"));
    private static final MemberId UNBLOCK_MEMBER = new MemberId(UUID.fromString("ee000002-0000-0000-0000-000000000000"));

    @MockitoBean
    @SuppressWarnings("unused")
    private Members members;

    @MockitoBean
    @SuppressWarnings("unused")
    private ActiveMembersByAgeProvider activeMembersByAgeProvider;

    @MockitoBean
    @SuppressWarnings("unused")
    private FinanceAccountLinkSupport financeAccountLinkSupport;

    @MockitoBean
    @SuppressWarnings("unused")
    private AllMembersPort allMembersPort;

    @MockitoBean
    @SuppressWarnings("unused")
    private ChargePort chargePort;

    @Autowired
    private MemberRegistrationSanctionPort sanctionPort;

    @Autowired
    private MemberRegistrationBlockRepository blockRepository;

    @AfterEach
    void cleanupBlocks() {
        blockRepository.unblock(SANCTION_MEMBER);
        blockRepository.unblock(UNBLOCK_MEMBER);
    }

    @Nested
    @DisplayName("MemberMissedFeeSelectionEvent")
    class MissedFeeSelectionEvents {

        @Test
        @DisplayName("should block member from event registrations when MemberMissedFeeSelectionEvent is received")
        void shouldBlockMemberWhenMissedFeeSelectionEventReceived(Scenario scenario) {
            assertThat(blockRepository.isBlocked(SANCTION_MEMBER))
                    .as("member must not be blocked initially")
                    .isFalse();

            scenario.publish(new MemberMissedFeeSelectionEvent(SANCTION_MEMBER, 2026))
                    .andWaitForStateChange(
                            () -> blockRepository.isBlocked(SANCTION_MEMBER),
                            blocked -> blocked)
                    .andVerify(isBlocked ->
                            assertThat(isBlocked)
                                    .as("member must be blocked after missed fee selection event")
                                    .isTrue()
                    );
        }
    }

    @Nested
    @DisplayName("MemberFeeSelectionResolvedEvent")
    class ResolvedFeeSelectionEvents {

        @BeforeEach
        void blockMember() {
            blockRepository.block(UNBLOCK_MEMBER);
        }

        @Test
        @DisplayName("should unblock member when MemberFeeSelectionResolvedEvent is received after being blocked")
        void shouldUnblockMemberWhenFeeSelectionResolvedEventReceived(Scenario scenario) {
            assertThat(blockRepository.isBlocked(UNBLOCK_MEMBER))
                    .as("member must be blocked before the test")
                    .isTrue();

            scenario.publish(new MemberFeeSelectionResolvedEvent(UNBLOCK_MEMBER, 2026))
                    .andWaitForStateChange(
                            () -> !blockRepository.isBlocked(UNBLOCK_MEMBER),
                            unblocked -> unblocked)
                    .andVerify(isUnblocked ->
                            assertThat(isUnblocked)
                                    .as("member must be unblocked after fee selection resolved event")
                                    .isTrue()
                    );
        }
    }
}
