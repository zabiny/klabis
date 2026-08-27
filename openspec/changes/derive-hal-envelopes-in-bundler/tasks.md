## 1. Verify assumptions before writing anything

- [x] 1.1 Start the local environment (`./runLocalEnvironment.sh`) and capture live HAL responses for
      at least 3 collection endpoints — one declaring `x-klabis-relation`, one relying on the
      default, and one from `events`. Record the actual `_embedded` key of each.

      Observed:
      | endpoint | live `_embedded` key | source |
      |---|---|---|
      | `GET /api/events/{eventId}/accommodation-list` | `accommodationList` | declared `x-klabis-relation` on `AccommodationListItemDto` |
      | `GET /api/members` | `memberSummaryResponseList` | default from `MemberSummaryResponse` |
      | `GET /api/events` | `eventSummaryDtoList` | default from `EventSummaryDto` |

- [x] 1.2 Confirm every observed key matches either the declared `x-klabis-relation.collectionRelation`
      or `uncapitalize(schemaName) + "List"`. Any mismatch → add an explicit `x-klabis-relation` to
      that schema in a separate commit before proceeding; do NOT special-case it in the deriver.

      **Confirmed — no mismatch.** The declared case returns exactly its `collectionRelation`; both
      undeclared cases follow `uncapitalize(schemaName) + "List"`. `EventSummaryDto` → `eventSummaryDtoList`
      additionally shows the rule is purely lexical: the `Dto` suffix is carried through unchanged, so
      the fallback does not depend on schemas being named `*Response`. The deriver may rely on it.
- [ ] 1.3 Record the baseline: `sha256sum docs/openapi/klabis-full.json` and, after
      `./gradlew openApiGenerateFinance openApiGenerateMembers …`, a recursive checksum of
      `backend/build/generated/openapi/`. These are the reference values for every later step.

## 2. Deriver and validation (no spec changes yet)

- [ ] 2.1 Add `tools/openapi-bundle/lib/derive.mjs` implementing the Decision 2 rule: item →
      `EntityModel<Payload>`; array → `CollectionModel<EntityModel<Item>>`, or
      `PagedModel<EntityModel<Item>>` + `page` when the operation carries `x-spring-paginated`.
- [ ] 2.2 Implement `_embedded` key resolution per Decision 5: `x-klabis-relation.collectionRelation`
      when present, else `uncapitalize(schemaName) + "List"`.
- [ ] 2.3 Implement the `application/prs.hal-forms+json` content-entry insertion, skipped when the
      operation carries `x-klabis-hal: false`.
- [ ] 2.4 Implement `x-hal-entity-items` handling: emit `EntityModel<Payload>` for the array's items
      and rewrite the items `$ref` to it.
- [ ] 2.5 Make the deriver a **no-op on already-enveloped input** — a schema already shaped as an
      envelope, or an operation already carrying a hal-forms content entry, is left untouched. This
      is what lets migrated and unmigrated modules coexist.
- [ ] 2.6 Add `validate.mjs` rules: `x-hal-entity-items` must be `true`, only on `type: array` whose
      `items` is a `$ref`, and must not point at a schema already shaped as an envelope;
      `x-klabis-hal` must be `false` when present and only on an operation.
- [ ] 2.7 Unit-test the deriver in `tools/openapi-bundle/test/` — both shapes, paged vs unpaged,
      declared vs derived `_embedded` key, `x-hal-entity-items`, opt-out, and the no-op property.
- [ ] 2.8 Run `./gradlew openapiBundle` and confirm `klabis-full.json` still matches the 1.3
      checksum — the deriver is wired in but changes nothing yet.

## 3. Migrate `finance` (3 envelopes — proves the approach)

- [ ] 3.1 Rewrite `docs/openapi/spec/finance.yaml`: delete the envelope schemas and bare-array `*List`
      siblings, drop the `application/prs.hal-forms+json` content entries, point the
      `application/json` responses at the payload schemas directly.
- [ ] 3.2 Run `./gradlew openapiBundle`; confirm `git diff docs/openapi/klabis-full.json` is empty.
      If it is not, fix the deriver — do not adjust the spec to match the deriver.
- [ ] 3.3 Run `./gradlew openApiGenerateFinance`; confirm the generated Java under
      `build/generated/openapi/finance/` is unchanged against the 1.3 baseline.
- [ ] 3.4 Run backend tests via the `test-runner` agent; confirm no failures and no test edits needed.
- [ ] 3.5 Commit. This is the go/no-go point for the whole change.

## 4. Migrate the remaining simple modules

- [ ] 4.1 `members` (3 envelopes, 1 `x-spring-paginated`) — same 5 steps as section 3, one commit.
- [ ] 4.2 `calendar` (3 envelopes, 1 `x-klabis-hal: false` on `getMySchedule`) — same, one commit.
- [ ] 4.3 `common` (3 envelopes, 4 `x-klabis-hal: false` on the pre-auth password endpoints) — same,
      one commit.
- [ ] 4.4 `oris` (`listOrisEvents` needs only `x-klabis-hal: false`) — same, one commit.

## 5. Migrate the large modules

- [ ] 5.1 `events` (13 envelopes, 1 `x-spring-paginated`, 2 `x-klabis-relation`) — same 5 steps,
      one commit.
- [ ] 5.2 `membershipfees` (13 envelopes, 1 `x-klabis-relation`) — same, one commit.

## 6. Migrate `groups` (18 envelopes + 7 nested + 14 mappings)

- [ ] 6.1 Resolve the open question on `CollectionModelEntityModelPendingInvitationResponseForInvitationsList`
      and `EntityModelPendingInvitationResponseForInvitationsList`: determine whether the deriver's
      naming reproduces them, or whether an explicit override is required for byte-identity.
- [ ] 6.2 Rewrite the 11 response-level envelopes in `groups.yaml` as in section 3.
- [ ] 6.3 Replace the 7 nested envelope schemas with `x-hal-entity-items: true` on
      `FamilyGroupResponse.parents`/`.members`, `GroupResponse.owners`/`.members`/`.pendingInvitations`,
      `TrainingGroupResponse.trainers`/`.members`.
- [ ] 6.4 Remove the 7 `schemaMappings` + 7 `extraImportMappings` entries from the `groups`
      `openApiModule(...)` block in `backend/build.gradle.kts`.
- [ ] 6.5 Confirm `git diff docs/openapi/klabis-full.json` is empty and that the generated Java still
      resolves `TrainingGroupResponse.trainers` to `List<EntityModel<TrainerResponse>>` (not
      `List<TrainerResponse>`) — the exact property the old detector guaranteed.
- [ ] 6.6 Run backend tests via `test-runner`; commit.

## 7. Remove the detector

- [ ] 7.1 Delete `backend/buildSrc/src/main/java/com/klabis/openapi/codegen/HalEnvelopeDetector.java`
      and `EnvelopeUnwrap.java`.
- [ ] 7.2 Delete `HalEnvelopeDetectorShape1Test`, `HalEnvelopeDetectorShape2Test`,
      `HalEnvelopeDetectorPropertyItemTest` and `HalEnvelopeFixtures`.
- [ ] 7.3 Simplify `KlabisSpringCodegen`: remove the envelope-detection paths in
      `handleMethodResponse()`, `fromProperty()` and `getContent()`, leaving the
      `x-hal-entity-items` reader.
- [ ] 7.4 Update `KlabisSpringCodegenFromPropertyTest`, `KlabisSpringCodegenGetContentTest` and
      `KlabisSpringCodegenHandleMethodResponseTest` — remove envelope-detection cases, keep and adapt
      the assertions covering the marker-driven path.
- [ ] 7.5 Full clean build (`./gradlew clean build`) plus the frontend check
      (`npm run openapi && npm run build`); confirm `klabis-full.json`, `halTypes.ts` and the
      frontend TypeScript types are all unchanged.

## 8. Documentation

- [ ] 8.1 Update `.claude/skills/klabis-api-spec/SKILL.md` — remove the hand-written-envelope
      authoring rules, document `x-hal-entity-items` and `x-klabis-hal`, and state that HAL is added
      by default.
- [ ] 8.2 Update `docs/openapi/spec/README.md` with the same.
- [ ] 8.3 Delete the obsolete `groups.yaml` header comment explaining the `schemaMapping` /
      Category-C interaction.
- [ ] 8.4 Add an ADR section to `docs/design-decisions.md` recording that HAL envelope structure now
      lives solely in the bundler, and why.
