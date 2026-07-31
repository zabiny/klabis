package com.klabis;

import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

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
 * authorization check. That residual case is what this test exists for.
 */
@DisplayName("PATCH request wrapper architecture")
class PatchRequestWrapperArchitectureTest {

    private static final List<Class<?>> PATCH_REQUESTS = List.of(
            com.klabis.members.infrastructure.restapi.UpdateMemberRequest.class,
            com.klabis.groups.traininggroup.infrastructure.restapi.UpdateTrainingGroupRequest.class);

    /**
     * gender is unwrapped on purpose. The generator strips property-level vendor extensions from a
     * composed schema, so the nullable {@code oneOf} the other {@code $ref} properties use would
     * cost it {@code x-klabis-authority} — an admin-only field would become writable by anyone.
     * Nothing is lost by the exception: gender has no cleared state, so it never needed the wrapper.
     * It keeps its authorization because {@code @HasAuthority} is still on the component.
     */
    private static final List<String> UNWRAPPED_BY_DESIGN = List.of("gender");

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
                    .filter(name -> !UNWRAPPED_BY_DESIGN.contains(name))
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
     * The exception above only holds while the excepted component is still annotated — an unwrapped
     * component is skipped by the advice, so {@code @HasAuthority} on it is what the spec's
     * {@code x-klabis-authority} is protecting, and the exception exists precisely to keep it.
     */
    @Test
    @DisplayName("a component unwrapped by design still declares the authority it was kept for")
    void unwrappedComponentsStillDeclareAuthority() {
        for (Class<?> patchRequest : PATCH_REQUESTS) {
            for (RecordComponent component : patchRequest.getRecordComponents()) {
                if (!UNWRAPPED_BY_DESIGN.contains(component.getName())) {
                    continue;
                }
                assertThat(component.getAccessor().getAnnotation(HasAuthority.class))
                        .as("%s.%s is exempt from the wrapper rule only because it carries "
                            + "@HasAuthority — without it the exemption is unjustified",
                                patchRequest.getSimpleName(), component.getName())
                        .isNotNull();
            }
        }
    }
}
