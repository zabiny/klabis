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
- [x] 1.3 Record the baseline: `sha256sum docs/openapi/klabis-full.json` and, after
      `./gradlew openApiGenerateFinance openApiGenerateMembers …`, a recursive checksum of
      `backend/build/generated/openapi/`. These are the reference values for every later step.

      `docs/openapi/klabis-full.json` → `1420db6e7f664945dc4884b874a778250519138ac5acd87411f2a7a641d7bdc4`
      (111 operations). **Superseded by task 1.4** — after the accommodation fix the reference
      baseline is `66b815aee1cbce9a5dcdcbade07296559324e916608397a6353974adb8966f14`. Note the file
      is gitignored (`.gitignore:36`), so this hash — not `git diff` — is the check.

      Generated Java: raw checksums are useless because `@Generated(date = "...")` carries a fresh
      timestamp on every run. Stripping that attribute gives stable per-module checksums (224 files
      total), recorded **after** the task 1.4 fix:

      | module | files | checksum (timestamp-stripped) |
      |---|---|---|
      | calendar | 16 | `fa20e1f38535a7f077a8bd46d0914afcc89379a0c926c7b980aacee3a7127f69` |
      | common | 20 | `ba304de8a8cef1b8acd9869285132b7e753873a31331cb2802419a04401c067a` |
      | events | 51 | `e1e8d182c66508d669a22df3840292717dcfaef81d383385df7d6572d50a4420` |
      | finance | 13 | `68cf952698ab4e24cea90834cd1d9a39a14684144418844852f668b7b3b8e0e7` |
      | groups | 42 | `7e7bfcc21d6939256661b2771090fbd49a90f77047095fe4a89211d3820624e3` |
      | members | 31 | `62942dda74fa72563affc275271a6b72370426ee4c37963ba59bde91cde1c390` |
      | membershipfees | 46 | `e10e839fc6ee5aa1a03bf6f2106020b126e995ef97ed55938aaaa8a12641e6c0` |
      | oris | 5 | `62d8e4528b1fd45257e825e5022b2c3684995fb8e5493b3597bd2ed9ffa5bd72` |

      A full copy lives at `~/.cache/klabis-openapi-baseline/openapi-baseline/`, with
      `~/.cache/klabis-openapi-baseline/cmp-generated.sh [module ...]` comparing the current output
      against it file by file with the timestamp stripped. (Kept outside `/mnt/ramdisk`, which is
      cleared without warning — it already vanished once mid-change.) Rebuild it after any step that
      intentionally changes the generated Java.

- [x] 1.4 **Straighten the one collection whose items were not `EntityModel`-wrapped.** Surveying the
      16 collection envelopes in the bundle showed `CollectionModelEntityModelAccommodationListItemDto`
      pointing `_embedded.accommodationList.items` at the bare `AccommodationListItemDto` — the sole
      exception to the rule Decision 2 relies on. Fixed rather than special-cased: added
      `AccommodationListItemPostprocessor` (`EventController.java`) giving each row a `self` link to
      `/api/events/{eventId}/registrations/{memberId}`, and wrapped the items in a new
      `EntityModelAccommodationListItemDto` schema in `events.yaml`. `AccommodationListSupport` was
      extracted so both accommodation postprocessors share the URI-template eventId lookup.

      The row's self link is authorization-gated like any other: `getRegistration` carries
      `x-klabis-authority: EVENTS_REGISTRATIONS`, so `klabisLinkTo` withholds it from a coordinator
      who lacks that authority. `EventControllerTest` gained a present/absent pair pinning this down,
      mirroring the existing `eventLinkPresent`/`eventLinkAbsent` tests.

      Verified: bundle diff contains exactly the two intended hunks (items `$ref` retarget + the new
      schema) and nothing else; generated Java for `events` unchanged; `EventControllerTest` 115/115;
      `npm run build` passes. Live response confirms each row now carries
      `_links.self → /api/events/{eventId}/registrations/{memberId}`.

      Unrelated pre-existing drift surfaced here: `frontend/src/api/klabisApi.d.ts` was already stale
      against the committed specs (44 insertions / 32 deletions independent of this change, verified
      by regenerating from an unmodified tree). Regenerating brought it back in sync, so that diff
      rides along in this commit.

- [ ] 1.5 **Two `EntityModel*` schemas carry a `description` the deriver cannot reproduce**
      (`members.yaml:433`, `:534` — "X as served, wrapped in a HAL EntityModel."). The other 38 have
      none. Delete both as part of step 4.1 (`members`), accepting a two-line intended bundle diff;
      they are decoration with no effect on the frontend types or the generated Java.

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
- [ ] 3.2 Run `./gradlew openapiBundle`; confirm `sha256sum docs/openapi/klabis-full.json` still
      matches the 1.3 baseline. (`git diff` cannot be used — the file is gitignored, see Decision 6.)
      If the hash differs, fix the deriver — do not adjust the spec to match the deriver.
- [ ] 3.3 Run `./gradlew openApiGenerateFinance`; confirm the generated Java under
      `build/generated/openapi/finance/` is unchanged against the 1.3 baseline, via
      `~/.cache/klabis-openapi-baseline/cmp-generated.sh finance` (a plain `diff -r` cannot be used —
      the `@Generated` timestamp changes every run).
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
- [ ] 6.5 Confirm the bundle hash still matches the 1.3 baseline and that the generated Java still
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
