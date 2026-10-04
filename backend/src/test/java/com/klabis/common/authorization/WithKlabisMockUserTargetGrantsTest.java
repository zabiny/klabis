package com.klabis.common.authorization;

import com.klabis.common.CommonInfrastructureWebMvcSetup;
import com.klabis.common.FixedAuthorizationSnapshotConfiguration;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WithKlabisMockUserTargetGrantsTest.ProbeController.class)
@CommonInfrastructureWebMvcSetup
@Import({FixedAuthorizationSnapshotConfiguration.class, AuthorizationEvaluator.class})
@DisplayName("@WithKlabisMockUser target grants installed as the request snapshot")
class WithKlabisMockUserTargetGrantsTest {

    private static final String CHILD = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String STRANGER = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final String ME = "cccccccc-0000-0000-0000-000000000003";

    @Autowired
    MockMvc mockMvc;

    @MvcComponent
    @RestController
    static class ProbeController {

        private final ObjectProvider<AuthorizationEvaluator> evaluatorProvider;

        // @MvcComponent classes are scanned by every slice test, where the evaluator is not imported
        ProbeController(ObjectProvider<AuthorizationEvaluator> evaluatorProvider) {
            this.evaluatorProvider = evaluatorProvider;
        }

        @GetMapping("/probe/everything")
        boolean everything() {
            return evaluatorProvider.getObject().has(Authority.MEMBERS_MANAGE);
        }

        @GetMapping("/probe/registrations/{memberId}")
        boolean registrations(@PathVariable UUID memberId) {
            return evaluatorProvider.getObject().isAllowed(List.of(Authority.EVENTS_REGISTRATIONS), TargetRef.member(memberId), true);
        }
    }

    @Test
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    void shouldKeepGlobalAuthoritiesWorkingWithoutTargetGrants() throws Exception {
        mockMvc.perform(get("/probe/everything")).andExpect(status().isOk()).andExpect(content().string("true"));
    }

    @Test
    @WithKlabisMockUser
    void shouldHoldNothingWithoutAuthorities() throws Exception {
        mockMvc.perform(get("/probe/everything")).andExpect(status().isOk()).andExpect(content().string("false"));
    }

    @Test
    @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS, type = TargetType.MEMBER, ids = CHILD))
    void shouldAllowGrantedTarget() throws Exception {
        mockMvc.perform(get("/probe/registrations/" + CHILD)).andExpect(content().string("true"));
    }

    @Test
    @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS, type = TargetType.MEMBER, ids = CHILD))
    void shouldDenyOtherTarget() throws Exception {
        mockMvc.perform(get("/probe/registrations/" + STRANGER)).andExpect(content().string("false"));
    }

    @Test
    @WithKlabisMockUser(authorities = Authority.EVENTS_REGISTRATIONS)
    void shouldAllowEveryTargetWhenHeldOverEverything() throws Exception {
        mockMvc.perform(get("/probe/registrations/" + STRANGER)).andExpect(content().string("true"));
    }

    @Test
    @WithKlabisMockUser(memberId = ME)
    void shouldAllowOwnerVisibleSelf() throws Exception {
        mockMvc.perform(get("/probe/registrations/" + ME)).andExpect(content().string("true"));
    }
}
