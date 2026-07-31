package com.klabis;

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
 * <p>These records are generated from the spec, where the wrapper comes from a per-property
 * {@code x-klabis-patch-field} extension. A single mistyped or forgotten key is enough, and it fails
 * open — hence a structural check rather than trusting review.
 *
 * <p>Dropping the extension usually breaks compilation too, because the mapper unwraps the value.
 * Not always: a component the mapper passes straight through keeps compiling and silently loses its
 * authorization check. That residual case is what this test exists for.
 */
@DisplayName("PATCH request wrapper architecture")
class PatchRequestWrapperArchitectureTest {

    private static final List<Class<?>> PATCH_REQUESTS = List.of(
            com.klabis.members.infrastructure.restapi.UpdateMemberRequest.class,
            com.klabis.groups.traininggroup.infrastructure.restapi.UpdateTrainingGroupRequest.class);

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
}
