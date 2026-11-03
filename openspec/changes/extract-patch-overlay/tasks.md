## 1. `PatchOverlay` (`common.patch`)

- [x] 1.1 Write `PatchOverlayTest` in `backend/src/test/java/com/klabis/common/patch`. It covers `overlay` and `overlayNonNull` against an undefined field, a present null and a present value (6 cases). It asserts whether the consumer ran and with which value.
- [x] 1.2 Implement `PatchOverlay` (final, private constructor) with `overlay(JsonNullable<T>, Consumer<T>)` delegating to `field.ifPresent(apply)`, and `overlayNonNull(JsonNullable<T>, Consumer<T>)`. Write the class javadoc that states the three PATCH states and both rules (design decision 2). 1.1 passes.

## 2. Mappers

- [x] 2.1 `UpdateMemberRequestMapper`:
  - replace `overlay` with `PatchOverlay.overlay` and `overlayValue` with `PatchOverlay.overlayNonNull` (static imports), with no other change per line;
  - delete both private helpers;
  - shorten the class javadoc to the fields without a cleared state (`PersonalInformation`), referring to `PatchOverlay`;
  - keep the `IllegalArgumentException` wrapping.
- [x] 2.2 `UpdateEventRequestMapper`:
  - replace the helpers the same way and delete both;
  - shorten the class javadoc to the fields without a cleared state (`name`, `eventDate`, `organizer`);
  - leave the body of `overlaySiPolicy` unchanged, and in its javadoc refer to `PatchOverlay.overlayNonNull` as the rule it follows.
- [x] 2.3 `UpdateTrainingGroupRequestMapper`:
  - replace `overlayValue` with `PatchOverlay.overlayNonNull` and delete the helper;
  - rewrite the class javadoc: none of the three fields has a cleared state, so a present null keeps the baseline. This removes the stale "surfaces as a 400" sentence.
- [x] 2.4 Confirm with `grep` that no `overlayValue` remains in `backend/src/main` and that no `private static .* overlay` remains outside `PatchOverlay`.
- [x] 2.5 Run the tests (test-runner agent):
  - `PatchOverlayTest`;
  - `UpdateMemberApiTest`, `EventControllerTest`, `TrainingGroupControllerTest`;
  - `PatchRequestWrapperArchitectureTest`.

  All pass with unchanged assertions.

## 3. Documentation

- [ ] 3.1 `.claude/skills/backend-patterns/references/rest-controller.md` (PATCH section):
  - the overlay example calls `PatchOverlay.overlay` / `overlayNonNull`;
  - replace the two private helper snippets with a pointer to `common.patch.PatchOverlay`;
  - state that a mapper never declares its own helpers.
- [ ] 3.2 `.claude/skills/backend-patterns/references/aggregate-checklist.md`: the PATCH mapper item names `PatchOverlay`.

## 4. Verification

- [ ] 4.1 Run the backend module tests of `members`, `events`, `groups` and `common` (test-runner agent). All pass.
- [ ] 4.2 Review the mapper diffs: every call site keeps its rule (`overlay` stays `overlay`, `overlayValue` becomes `overlayNonNull`). Run the code review before the commit.
