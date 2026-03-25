package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AffordanceAuthorizationTest.AffordanceTestController.class)
@Import(HalFormsSupport.class)
@DisplayName("klabisAfford authorization filtering")
class AffordanceAuthorizationTest {

    private static final String OWNER_ID_STRING = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER_ID_STRING = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final UUID OWNER_ID = UUID.fromString(OWNER_ID_STRING);
    private static final UUID OTHER_ID = UUID.fromString(OTHER_ID_STRING);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserDetailsService userDetailsService;

    record AffordanceTestResponse(String value) {}

    record AffordanceTestRequest(String value) {}

    @MvcComponent
    @RestController
    static class AffordanceTestController {

        @GetMapping(value = "/api/afford-test/no-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<AffordanceTestResponse> getNoAuth() {
            return EntityModel.of(new AffordanceTestResponse("data"))
                    .add(klabisLinkTo(methodOn(AffordanceTestController.class).getNoAuth()).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(AffordanceTestController.class).updateNoAuth(null))));
        }

        @PatchMapping("/api/afford-test/no-auth")
        ResponseEntity<Void> updateNoAuth(@RequestBody AffordanceTestRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/api/afford-test/has-authority/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<AffordanceTestResponse> getHasAuthority(@PathVariable UUID id) {
            return EntityModel.of(new AffordanceTestResponse("data"))
                    .add(klabisLinkTo(methodOn(AffordanceTestController.class).getHasAuthority(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(AffordanceTestController.class).updateHasAuthority(id, null))));
        }

        @PatchMapping("/api/afford-test/has-authority/{id}")
        @HasAuthority(Authority.MEMBERS_MANAGE)
        ResponseEntity<Void> updateHasAuthority(@PathVariable UUID id, @RequestBody AffordanceTestRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/api/afford-test/owner-visible/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<AffordanceTestResponse> getOwnerVisible(@PathVariable UUID id) {
            return EntityModel.of(new AffordanceTestResponse("data"))
                    .add(klabisLinkTo(methodOn(AffordanceTestController.class).getOwnerVisible(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(AffordanceTestController.class).updateOwnerVisible(id, null))));
        }

        @PatchMapping("/api/afford-test/owner-visible/{id}")
        @OwnerVisible
        ResponseEntity<Void> updateOwnerVisible(@PathVariable @OwnerId UUID id, @RequestBody AffordanceTestRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/api/afford-test/owner-or-admin/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<AffordanceTestResponse> getOwnerOrAdmin(@PathVariable UUID id) {
            return EntityModel.of(new AffordanceTestResponse("data"))
                    .add(klabisLinkTo(methodOn(AffordanceTestController.class).getOwnerOrAdmin(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(AffordanceTestController.class).updateOwnerOrAdmin(id, null))));
        }

        @PatchMapping("/api/afford-test/owner-or-admin/{id}")
        @HasAuthority(Authority.MEMBERS_MANAGE)
        @OwnerVisible
        ResponseEntity<Void> updateOwnerOrAdmin(@PathVariable @OwnerId UUID id, @RequestBody AffordanceTestRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/api/afford-test/owner-null/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<AffordanceTestResponse> getOwnerNullId(@PathVariable UUID id) {
            return EntityModel.of(new AffordanceTestResponse("data"))
                    .add(klabisLinkTo(methodOn(AffordanceTestController.class).getOwnerNullId(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(AffordanceTestController.class).updateOwnerNullId(null, null))));
        }

        @PatchMapping("/api/afford-test/owner-null/{id}")
        @OwnerVisible
        ResponseEntity<Void> updateOwnerNullId(@PathVariable @OwnerId UUID id, @RequestBody AffordanceTestRequest body) {
            return ResponseEntity.noContent().build();
        }
    }

    @Nested
    @DisplayName("method with no security annotations")
    class NoSecurityAnnotations {

        @Test
        @WithKlabisMockUser(username = "anyUser")
        @DisplayName("affordance is always present regardless of user authorities")
        void affordanceAlwaysPresentWithNoAnnotations() throws Exception {
            mockMvc.perform(get("/api/afford-test/no-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists());
        }
    }

    @Nested
    @DisplayName("method with @HasAuthority")
    class HasAuthorityMethod {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("affordance present when user has required authority")
        void affordancePresentWhenAuthorized() throws Exception {
            mockMvc.perform(get("/api/afford-test/has-authority/{id}", OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists());
        }

        @Test
        @WithKlabisMockUser(username = "noAuthUser")
        @DisplayName("affordance absent when user lacks required authority")
        void affordanceAbsentWhenNotAuthorized() throws Exception {
            mockMvc.perform(get("/api/afford-test/has-authority/{id}", OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }
    }

    @Nested
    @DisplayName("method with @OwnerVisible + @OwnerId")
    class OwnerVisibleMethod {

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("affordance present when user is the owner")
        void affordancePresentForOwner() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-visible/{id}", OWNER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("affordance absent when user is not the owner")
        void affordanceAbsentForNonOwner() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-visible/{id}", OWNER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("affordance absent when @OwnerId argument is null — conservative approach")
        void affordanceAbsentWhenOwnerIdArgumentIsNull() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-null/{id}", OWNER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }
    }

    @Nested
    @DisplayName("method with @HasAuthority + @OwnerVisible (OR semantics)")
    class HasAuthorityOrOwnerVisible {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("affordance present when user has required authority (not owner)")
        void affordancePresentForAdmin() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-or-admin/{id}", OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("affordance present when user is the owner (lacks authority)")
        void affordancePresentForOwnerWithoutAuthority() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-or-admin/{id}", OWNER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("affordance absent when user is not owner and lacks authority")
        void affordanceAbsentForUnauthorizedNonOwner() throws Exception {
            mockMvc.perform(get("/api/afford-test/owner-or-admin/{id}", OWNER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }
    }
}
