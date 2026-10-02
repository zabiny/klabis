package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.JwtParams;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Own-profile and other-member branches are covered at controller level in MemberControllerApiTest;
 * this class holds only branches that cannot be reached from a response.
 */
@DisplayName("IcalTokenMemberDetailLinkProcessor")
class IcalTokenMemberDetailLinkProcessorTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final IcalTokenMemberDetailLinkProcessor processor = new IcalTokenMemberDetailLinkProcessor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsMember(UUID memberUuid) {
        var token = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.member(memberUuid)
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void authenticateAsUserWithoutMemberProfile(UUID userUuid) {
        var token = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("ZBM0001", userUuid)
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private EntityModel<MemberDetailsResponse> modelForMember(UUID memberId) {
        MemberDetailsResponse resource = MemberDetailsResponseBuilder.builder()
                .id(memberId)
                .build();
        return EntityModel.of(resource);
    }

    @Nested
    @DisplayName("when authenticated user has no member profile (admin-only account)")
    class NoMemberProfile {

        @BeforeEach
        void setUp() {
            authenticateAsUserWithoutMemberProfile(UUID.randomUUID());
        }

        @Test
        @DisplayName("ical-token link is NOT added")
        void icalTokenLinkIsNotAdded() {
            EntityModel<MemberDetailsResponse> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token")).isEmpty();
        }
    }

    @Nested
    @DisplayName("when security context is empty (unauthenticated)")
    class Unauthenticated {

        @Test
        @DisplayName("ical-token link is NOT added")
        void icalTokenLinkIsNotAdded() {
            SecurityContextHolder.clearContext();
            EntityModel<MemberDetailsResponse> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token")).isEmpty();
        }
    }

    @Nested
    @DisplayName("when the resource has no id")
    class NoResourceId {

        @Test
        @DisplayName("ical-token link is NOT added")
        void icalTokenLinkIsNotAdded() {
            authenticateAsMember(MEMBER_UUID);
            EntityModel<MemberDetailsResponse> model = EntityModel.of(MemberDetailsResponseBuilder.builder().build());

            processor.process(model);

            assertThat(model.getLink("ical-token")).isEmpty();
        }
    }
}
