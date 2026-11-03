## Context

Three PATCH endpoints use the prefill + overlay pattern:

1. The controller loads a fully pre-filled update command (the baseline).
2. A hand-written mapper writes onto it, through the `@RecordBuilder` copy-builder, only the fields the request carries.

Each mapper declares the overlay helpers privately:

| Mapper | `overlay` calls | `overlayValue` calls | Other |
|---|---|---|---|
| `UpdateMemberRequestMapper` | 12 | 5 | `catch IllegalArgumentException` → `InvalidUpdateException` |
| `UpdateEventRequestMapper` | 10 | 3 | `overlaySiPolicy` (two request fields → one `SiPolicy`), `mergeCategories` |
| `UpdateTrainingGroupRequestMapper` | 0 | 3 | |

The two helpers do the following:
- `overlay(field, apply)` runs `apply` for a present field, including a present null. It is the same as `field.ifPresent(apply)`, because `JsonNullable.ifPresent` passes a present null on.
- `overlayValue(field, apply)` runs `apply` only for a present value that is not null.

The `backend-patterns` skill (`references/rest-controller.md`) shows both helpers as private methods of the mapper, so every new PATCH mapper copies them. The javadoc of `UpdateTrainingGroupRequestMapper` describes a present-null as surfacing a 400. `TrainingGroupControllerTest` ("an explicit null for a non-clearable field retains the baseline value", "an explicit null ageRange retains the baseline value") pins the opposite, which is what the code does.

Request-body field authorization runs in `RequestBodyFieldAuthorizationAdvice` on the request DTO, before the controller body and the mapper. `PatchRequestWrapperArchitectureTest` guards that every component of the three request DTOs is a `JsonNullable`.

## Goals / Non-Goals

**Goals:**
- The two PATCH rules live in one module, documented once.
- New PATCH mappers use that module instead of copying helpers, and the skill says so.
- Each field keeps exactly the rule it has today.

**Non-Goals:**
- `overlaySiPolicy` and `mergeCategories`. Both are rules specific to their value objects.
- Error handling of value-object construction. The members `IllegalArgumentException` wrapping stays. Error types are decided by `show-error-messages-in-czech`.
- The other seven PATCH endpoints, whose request DTOs have no `JsonNullable` fields.
- Prefill (`prefilledUpdateCommand`) and the application side of PATCH.

## Decisions

1. **`PatchOverlay` is a final utility class in `com.klabis.common.patch`** with a private constructor and static methods. The package already holds `JsonNullableConfiguration`, and `common` is an OPEN Modulith module, so every module can use it with no named interface.

2. **Two methods, named by rule:**

   ```java
   public static <T> void overlay(JsonNullable<T> field, Consumer<T> apply)         // clearable
   public static <T> void overlayNonNull(JsonNullable<T> field, Consumer<T> apply)  // no cleared state
   ```

   `overlay` delegates to `field.ifPresent(apply)`. The class javadoc states the three states and both rules. A mapper's javadoc only lists its fields that have no cleared state.
   - *Alternative:* a single `overlayNonNull`, with clearable fields calling `JsonNullable.ifPresent` directly. It passes the deletion test better, because `overlay` is a pass-through. Rejected in the grilling session: two methods side by side make every call site name its rule in the same vocabulary.

3. **`overlayValue` is renamed to `overlayNonNull`.** The new name says what the method checks. The old one did not distinguish it from `overlay`.

4. **`overlaySiPolicy` keeps its inline resolution.** It needs "the value, or the baseline" for two fields to build one `SiPolicy`, and a consumer method would force mutable holders. A third method `valueOr(field, baseline)` would widen the interface for one caller. Its javadoc refers to `overlayNonNull` as the rule it follows.

5. **Tests: one seam test, existing endpoint tests kept.**
   - `PatchOverlayTest` covers both methods against an undefined field, a present null and a present value (6 cases).
   - The controller tests already pin the per-field behaviour end to end, and they stay unchanged.
   - No mapper unit tests are added, because they would layer a second test over the same rule.
   - The bodies move unchanged, so there is no failing-first step. The controller tests are the regression guard.

## Risks / Trade-offs

- **[Risk] A field switches rule during the replacement**, for example `overlayValue` replaced by `overlay`. → Replace mechanically: `overlay` stays `overlay`, and `overlayValue` becomes `overlayNonNull`. The existing explicit-null controller tests catch a mix-up for some fields:
  - training group `trainers` and `ageRange` (`TrainingGroupControllerTest`);
  - member `chipNumber`, `bankAccountNumber`, `birthNumber` and `dietaryRestrictions` (`UpdateMemberApiTest`);
  - event `siUsage` (`EventControllerTest`).

  For every other field, the mapper diff must show only the rename. Code review checks that.
- **[Trade-off] `overlay` is a one-line pass-through to `JsonNullable.ifPresent`.** → Accepted for the uniform vocabulary (decision 2).
