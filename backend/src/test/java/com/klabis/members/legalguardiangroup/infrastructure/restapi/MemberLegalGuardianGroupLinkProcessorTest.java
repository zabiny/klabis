package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.Member;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberLegalGuardianGroupLinkProcessor")
class MemberLegalGuardianGroupLinkProcessorTest {

    private final MemberLegalGuardianGroupLinkProcessor processor = new MemberLegalGuardianGroupLinkProcessor();

    @AfterEach
    void clearState() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("adds no link when the controller published no group")
    void addsNoLinkWithoutContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        UUID memberId = UUID.randomUUID();
        Member adult = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.now().minusYears(30)).build();
        EntityModel<MemberDetailsResponse> model = EntityModel.of(
                MemberDetailsResponseBuilder.builder().id(memberId).build(),
                Link.of("/api/members/" + memberId).withSelfRel());

        processor.process(model, adult);

        assertThat(model.getLink("legalGuardianGroup")).isEmpty();
        assertThat(model.getLink("legalGuardians")).isEmpty();
    }

    @Test
    @DisplayName("adds no link outside a request scope")
    void addsNoLinkWithoutRequestScope() {
        UUID memberId = UUID.randomUUID();
        Member adult = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.now().minusYears(30)).build();
        EntityModel<MemberDetailsResponse> model = EntityModel.of(
                MemberDetailsResponseBuilder.builder().id(memberId).build());

        processor.process(model, adult);

        assertThat(model.getLinks()).isEmpty();
    }
}
