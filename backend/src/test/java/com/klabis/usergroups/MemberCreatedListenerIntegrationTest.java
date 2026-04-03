package com.klabis.usergroups;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberCreatedListener integration test — auto-assignment to training group")
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES, verifyAutomatically = false)
@ActiveProfiles("test")
@CleanupTestData
@Import(TestApplicationConfiguration.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ZBM8500', 'Owner', 'Group', '1985-01-01', 'CZ', 'MALE', 'owner@test.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ZBM1001', 'New', 'Member', '2010-06-15', 'CZ', 'MALE', 'newmember@test.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO user_groups (id, type, name, age_range_min, age_range_max, created_at, created_by, modified_at, modified_by, version) VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'TRAINING', 'Juniors 10-18', 10, 18, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO user_group_owners (user_group_id, member_id) VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')",
        "INSERT INTO user_group_members (user_group_id, member_id, joined_at) VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', CURRENT_TIMESTAMP)"
})
class MemberCreatedListenerIntegrationTest {

    private static final MemberId NEW_MEMBER_ID = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final MemberId OWNER_MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @TestConfiguration
    static class SyncTaskExecutorConfiguration {
        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Test
    @DisplayName("should auto-assign new member to training group when dateOfBirth falls within age range")
    void shouldAutoAssignMemberToMatchingTrainingGroup() {
        MemberCreatedEvent event = new MemberCreatedEvent(
                NEW_MEMBER_ID,
                RegistrationNumber.of("ZBM1001"),
                "New",
                "Member",
                LocalDate.of(2010, 6, 15),
                "CZ",
                Gender.MALE,
                new Address("Street 2", "City", "11000", "CZ"),
                EmailAddress.of("newmember@test.com"),
                PhoneNumber.of("+420222222222"),
                null
        );

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        List<UserGroup> groups = userGroupRepository.findAll(GroupFilter.byMember(NEW_MEMBER_ID));
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).isInstanceOf(TrainingGroup.class);
        assertThat(groups.get(0).getName()).isEqualTo("Juniors 10-18");
    }

    @Test
    @DisplayName("should not assign member when no training group age range matches")
    void shouldNotAssignMemberWhenNoMatchingGroup() {
        MemberId adultMemberId = new MemberId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
        MemberCreatedEvent event = new MemberCreatedEvent(
                adultMemberId,
                RegistrationNumber.of("ZBM8501"),
                "Adult",
                "Member",
                LocalDate.of(1985, 1, 1),
                "CZ",
                Gender.MALE,
                new Address("Street 3", "City", "11000", "CZ"),
                EmailAddress.of("adult@test.com"),
                PhoneNumber.of("+420333333333"),
                null
        );

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        List<UserGroup> groups = userGroupRepository.findAll(GroupFilter.byMember(adultMemberId));
        assertThat(groups).isEmpty();
    }
}
