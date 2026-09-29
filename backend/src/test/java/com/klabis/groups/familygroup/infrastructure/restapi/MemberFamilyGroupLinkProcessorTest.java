package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemberFamilyGroupLinkProcessor}.
 * <p>
 * The processor reaches the groups module from a member detail, where only a member id is
 * available. Family groups are keyed on {@code UserId}, so the member id has to be converted
 * before the lookup. The stubbed repository below answers the way the JDBC adapter does — by
 * parents and members of the group — so a wrong id in the filter yields no group.
 */
@DisplayName("MemberFamilyGroupLinkProcessor")
@ExtendWith(MockitoExtension.class)
class MemberFamilyGroupLinkProcessorTest {

    private static final MemberId CHILD = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId OTHER_CHILD = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final MemberId PARENT = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId NON_MEMBER_PARENT = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    private MemberFamilyGroupLinkProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MemberFamilyGroupLinkProcessor(familyGroupRepository);
    }

    private void givenGroups(FamilyGroup... groups) {
        when(familyGroupRepository.findOne(any(FamilyGroupFilter.class))).thenAnswer(invocation -> {
            UserId userId = invocation.<FamilyGroupFilter>getArgument(0).memberOrParentIs();
            if (userId == null) {
                return Optional.empty();
            }
            return Arrays.stream(groups)
                    .filter(g -> g.getParents().contains(userId) || g.hasMember(userId))
                    .findFirst();
        });
    }

    private static EntityModel<MemberDetailsResponse> detailOf(MemberId memberId) {
        return EntityModel.of(MemberDetailsResponseBuilder.builder().id(memberId.uuid()).build());
    }

    private FamilyGroupFilter capturedFilter() {
        ArgumentCaptor<FamilyGroupFilter> captor = ArgumentCaptor.forClass(FamilyGroupFilter.class);
        verify(familyGroupRepository).findOne(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("member is a child of the family group")
    class MemberIsChild {

        private FamilyGroup group;

        @BeforeEach
        void setUpGroup() {
            group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT.toUserId()));
            group.addChild(CHILD);
            group.addChild(OTHER_CHILD);
            givenGroups(group);
        }

        @Test
        @DisplayName("adds familyGroup link pointing at the group the child belongs to")
        void addsFamilyGroupLink() {
            EntityModel<MemberDetailsResponse> model = detailOf(CHILD);

            processor.process(model);

            Optional<Link> link = model.getLink("familyGroup");
            assertThat(link).isPresent();
            assertThat(link.get().getHref()).endsWith("/api/family-groups/" + group.getId().uuid());
        }

        @Test
        @DisplayName("looks the group up by the child's own user id")
        void looksUpByChildUserId() {
            EntityModel<MemberDetailsResponse> model = detailOf(CHILD);

            processor.process(model);

            assertThat(model.getLink("familyGroup")).isPresent();
            assertThat(capturedFilter().memberOrParentIs()).isEqualTo(CHILD.toUserId());
        }
    }

    @Nested
    @DisplayName("member is a parent of the family group")
    class MemberIsParent {

        @Test
        @DisplayName("adds familyGroup link — a parent is a member of the group too")
        void addsFamilyGroupLink() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT.toUserId()));
            givenGroups(group);
            EntityModel<MemberDetailsResponse> model = detailOf(PARENT);

            processor.process(model);

            Optional<Link> link = model.getLink("familyGroup");
            assertThat(link).isPresent();
            assertThat(link.get().getHref()).endsWith("/api/family-groups/" + group.getId().uuid());
            assertThat(capturedFilter().memberOrParentIs()).isEqualTo(PARENT.toUserId());
        }

        @Test
        @DisplayName("adds familyGroup link also when a co-parent has no member profile")
        void addsFamilyGroupLinkWithNonMemberCoParent() {
            FamilyGroup group = FamilyGroup.create(
                    new FamilyGroup.CreateFamilyGroup("Novákovi", NON_MEMBER_PARENT));
            group.addParent(PARENT.toUserId());
            givenGroups(group);
            EntityModel<MemberDetailsResponse> model = detailOf(PARENT);

            processor.process(model);

            assertThat(model.getLink("familyGroup")).isPresent();
            assertThat(model.getLink("familyGroup").get().getHref())
                    .endsWith("/api/family-groups/" + group.getId().uuid());
        }
    }

    @Test
    @DisplayName("adds no familyGroup link when the member belongs to no family group")
    void addsNoLinkWhenMemberHasNoFamilyGroup() {
        givenGroups(FamilyGroup.create(
                new FamilyGroup.CreateFamilyGroup("Novákovi", NON_MEMBER_PARENT)));

        EntityModel<MemberDetailsResponse> model = detailOf(OTHER_CHILD);

        processor.process(model);

        assertThat(model.getLink("familyGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds no familyGroup link for a member whose only group is a memberless parent's")
    void addsNoLinkWhenOnlyGroupHasMemberlessParent() {
        // A user without a member profile never has a member detail to process, so a group
        // parented solely by such a user is unreachable from every member id.
        FamilyGroup group = FamilyGroup.create(
                new FamilyGroup.CreateFamilyGroup("Novákovi", NON_MEMBER_PARENT));
        group.addChild(OTHER_CHILD);
        givenGroups(group);

        EntityModel<MemberDetailsResponse> model = detailOf(PARENT);

        processor.process(model);

        assertThat(model.getLink("familyGroup")).isEmpty();
    }
}
