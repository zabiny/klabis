package com.klabis;

import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the invariant that every component of a PATCH request DTO is wrapped in
 * {@link JsonNullable}.
 *
 * <p>{@code RequestBodyFieldAuthorizationAdvice} skips any component that is not wrapper-typed, so a
 * field that loses its wrapper is not merely mis-typed — it stops being authorization-checked
 * entirely, with no error anywhere. {@code UpdateMemberRequest} alone has five components carrying
 * {@code @HasAuthority(MEMBERS_MANAGE)} that would silently become writable by any caller.
 *
 * <p>These records are generated from the spec, where the wrapper follows from the OpenAPI 3.1
 * nullable spelling {@code type: [x, "null"]}. Writing the 3.0 {@code nullable: true} keyword
 * instead leaves the property non-nullable and unwrapped — silently, since the generator accepts
 * both documents. Hence a structural check rather than trusting review.
 *
 * <p>Losing the wrapper usually breaks compilation too, because the mapper unwraps the value. Not
 * always: a component the mapper passes straight through keeps compiling and silently loses its
 * authorization check. That residual case is what this test exists for — and it is not
 * hypothetical. {@code gender} shipped in exactly that state, generated as a bare {@code Gender}
 * from an {@code allOf} and passed straight through the mapper, so its
 * {@code @HasAuthority(MEMBERS_MANAGE)} was never evaluated.
 */
@DisplayName("PATCH request wrapper architecture")
class PatchRequestWrapperArchitectureTest {

    private static final List<Class<?>> PATCH_REQUESTS = List.of(
            com.klabis.members.infrastructure.restapi.UpdateMemberRequest.class,
            com.klabis.groups.traininggroup.infrastructure.restapi.UpdateTrainingGroupRequest.class,
            com.klabis.events.infrastructure.restapi.UpdateEventRequest.class);

    @Test
    @DisplayName("every component of a PATCH request DTO is a JsonNullable wrapper")
    void patchRequestComponentsAreWrapped() {
        for (Class<?> patchRequest : PATCH_REQUESTS) {
            assertThat(patchRequest.isRecord())
                    .as("%s must be a record — the advice only inspects record components",
                            patchRequest.getSimpleName())
                    .isTrue();

            List<String> unwrapped = Arrays.stream(patchRequest.getRecordComponents())
                    .filter(component -> !JsonNullable.class.isAssignableFrom(component.getType()))
                    .map(RecordComponent::getName)
                    .toList();

            assertThat(unwrapped)
                    .as("%s has components that are not JsonNullable, so "
                        + "RequestBodyFieldAuthorizationAdvice will skip them and any "
                        + "@HasAuthority/@OwnerVisible on them is silently unenforced",
                            patchRequest.getSimpleName())
                    .isEmpty();
        }
    }

    /**
     * The wrapper rule above is what makes {@code @HasAuthority} reachable; this asserts the
     * annotation is actually still there. Losing it is the other half of the same failure — the
     * advice would inspect the component and find nothing to enforce. It is easy to lose by
     * accident, because composition keywords ({@code oneOf}, {@code allOf}) silently strip
     * property-level vendor extensions such as {@code x-klabis-authority}.
     *
     * <p>Pinning the exact set, rather than merely counting, is what makes a *disappearance*
     * visible: a spec edit that drops one property's extension still leaves the others annotated,
     * so any assertion weaker than this passes straight through it.
     */
    private static final Map<Class<?>, List<String>> EXPECTED_PRIVILEGED_COMPONENTS = Map.of(
            com.klabis.members.infrastructure.restapi.UpdateMemberRequest.class,
            List.of("birthNumber", "dateOfBirth", "firstName", "gender", "lastName"),
            com.klabis.groups.traininggroup.infrastructure.restapi.UpdateTrainingGroupRequest.class,
            List.of(),
            com.klabis.events.infrastructure.restapi.UpdateEventRequest.class,
            List.of());

    @Test
    @DisplayName("privileged components still declare the authority the spec assigns them")
    void privilegedComponentsDeclareAuthority() {
        assertThat(EXPECTED_PRIVILEGED_COMPONENTS.keySet())
                .as("every PATCH DTO needs an expectation here, even an empty one — otherwise a "
                    + "DTO that grows its first privileged field is not covered by this test")
                .containsExactlyInAnyOrderElementsOf(PATCH_REQUESTS);

        EXPECTED_PRIVILEGED_COMPONENTS.forEach((patchRequest, expected) -> {
            List<String> annotated = Arrays.stream(patchRequest.getRecordComponents())
                    .filter(component -> component.getAccessor().getAnnotation(HasAuthority.class) != null)
                    .map(RecordComponent::getName)
                    .sorted()
                    .toList();

            assertThat(annotated)
                    .as("x-klabis-authority in the spec must survive into %s; a property rewritten "
                        + "to use oneOf/allOf loses it without any error",
                            patchRequest.getSimpleName())
                    .containsExactlyElementsOf(expected);
        });
    }
}
