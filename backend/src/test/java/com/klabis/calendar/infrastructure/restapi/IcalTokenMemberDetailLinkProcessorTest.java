package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.JwtParams;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IcalTokenMemberDetailLinkProcessor")
class IcalTokenMemberDetailLinkProcessorTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_MEMBER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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

    private EntityModel<MemberResource> modelForMember(UUID memberId) {
        MemberResource resource = () -> new MemberId(memberId);
        return EntityModel.of(resource);
    }

    @Nested
    @DisplayName("when authenticated user views their own member detail")
    class SelfDetail {

        @BeforeEach
        void setUp() {
            authenticateAsMember(MEMBER_UUID);
        }

        @Test
        @DisplayName("ical-token link is added")
        void icalTokenLinkIsAdded() {
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token")).isPresent();
        }

        @Test
        @DisplayName("ical-token link points to GET /api/me/ical-token")
        void icalTokenLinkPointsToCorrectEndpoint() {
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token").get().getHref()).contains("/api/me/ical-token");
        }
    }

    @Nested
    @DisplayName("when authenticated user views another member's detail")
    class OtherMemberDetail {

        @BeforeEach
        void setUp() {
            authenticateAsMember(OTHER_MEMBER_UUID);
        }

        @Test
        @DisplayName("ical-token link is NOT added")
        void icalTokenLinkIsNotAdded() {
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token")).isEmpty();
        }
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
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

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
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("ical-token")).isEmpty();
        }
    }
}
