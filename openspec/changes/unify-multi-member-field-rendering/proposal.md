## Why

The `refactor-halforms-field-factory` change (commit `9e54c027`) fixed multi-select `MemberId` fields
to render as a member picker per row instead of a checkbox group, by making the frontend trust the
HAL-FORMS `type` metadata. `UpdateTrainingGroupRequest.trainers` never got this fix at the source:
it still declares `PatchField<List<String>>` even though the domain layer (`TrainingGroup.replaceTrainers`,
`UpdateTrainingGroupCommand`) has used `Set<MemberId>` for a long time. As a result the HAL-FORMS
template advertises `trainers` as `{"multi": true, "type": "text"}`, so the frontend renders it as a
plain multi-text collection field. Verified live via Playwright on `/training-groups/{id}` edit form:
each row shows the literal string `[object Object]` in a text input, because the actual field values
are member reference objects, not plain text.

Separately, `KlabisFieldsFactory.tsx` still has a `case "List"` branch (added in `4ebbd633`, before the
field-factory refactor) based on an assumption that some backend field emits `type: "List"`. No current
DTO does — `HalFormsSupport.getInputType()` always unwraps to the real element type — so this branch is
dead code that should be removed while touching this area.

## What Changes

- Change `UpdateTrainingGroupRequest.trainers` from `PatchField<List<String>>` to `PatchField<List<MemberId>>`
  (mirrors the fix already applied to `InviteMemberRequest` in commit `83e3e985`), relying on `MemberId`'s
  existing JSON/MVC SerDe.
- Remove the now-unnecessary `trainerUuids()` conversion helper on `UpdateTrainingGroupRequest`.
- Simplify `TrainingGroupController.updateTrainingGroup` to pass `request.trainers()` straight through
  instead of manually parsing UUID strings back into `MemberId`.
- Remove the dead `case "List"` branch in `frontend/src/components/KlabisFieldsFactory.tsx`.
- No other multi-member field with this mismatch was found (checked all `List<...>` properties in
  `*Request.java`/`*Dto.java`; the only other multi-select field, `levelIds` in `PublishYearRequest`,
  correctly uses an explicit `MembershipFeeTierMultiSelect` `@HalForms` type and is not a member picker).

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/user-groups/spec.md` — describes trainer add/remove via dedicated `addTrainer`/
  `removeTrainer` endpoints (single-member operations) and via the bulk `trainers` field on
  `updateTrainingGroup`. The requirement is about the outcome (member added/removed as trainer),
  not the wire representation of the `trainers` property's HAL-FORMS `type` metadata. Unaffected.
- `openspec/specs/members/spec.md` — mentions training groups only in the context of navigation links
  and suspension warnings, not this update endpoint. Unaffected.

**Why no spec update is needed:**
The JSON wire format for `trainers` (`["uuid1", "uuid2", ...]`) is unchanged — `MemberId`'s existing
SerDe serializes/deserializes to/from the same UUID string representation `String`/`UUID` did. Only the
HAL-FORMS *template metadata* describing the field's declared type changes (`"text"` → `"MemberId"`),
which is purely a rendering hint consumed by the frontend field factory. No request/response schema,
validation rule, authorization check, or documented scenario changes. The frontend change corrects a
rendering bug (member picker instead of broken text input) without introducing new capability. Removing
the dead `case "List"` branch has no runtime effect since it is unreachable.

## Impact

- `backend/src/main/java/com/klabis/groups/traininggroup/infrastructure/restapi/UpdateTrainingGroupRequest.java`
- `backend/src/main/java/com/klabis/groups/traininggroup/infrastructure/restapi/TrainingGroupController.java`
- `frontend/src/components/KlabisFieldsFactory.tsx`
- Associated backend/frontend tests for these files
