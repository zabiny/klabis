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

- [x] 2.1 Add `tools/openapi-bundle/lib/derive.mjs` implementing the Decision 2 rule: item →
      `EntityModel<Payload>`; array → `CollectionModel<EntityModel<Item>>`, or
      `PagedModel<EntityModel<Item>>` + `page` when the operation carries `x-spring-paginated`.

      Runs inside `bundleSpec`, after `inlineRefs` (it needs cross-file refs collapsed to local ones
      to look a payload up) and before `sortKeysDeep` (so insertion order cannot affect byte-identity).
      Restricted to `2xx` responses — see 2.5.
- [x] 2.2 Implement `_embedded` key resolution per Decision 5: `x-klabis-relation.collectionRelation`
      when present, else `uncapitalize(schemaName) + "List"`.
- [x] 2.3 Implement the `application/prs.hal-forms+json` content-entry insertion, skipped when the
      operation carries `x-klabis-hal: false`.
- [x] 2.4 Implement `x-hal-entity-items` handling: emit `EntityModel<Payload>` for the array's items
      and rewrite the items `$ref` to it.
- [x] 2.5 Make the deriver a **no-op on already-enveloped input** — a schema already shaped as an
      envelope, or an operation already carrying a hal-forms content entry, is left untouched. This
      is what lets migrated and unmigrated modules coexist.

      Verified empirically, not just by unit test: with the deriver wired in, the bundle differs from
      the pre-deriver baseline by exactly the 6 `x-klabis-hal: false` markers added in 2.8 and
      nothing else — zero derived schemas or content entries across all 105 HAL operations.

      This check caught a real bug: the first version also enveloped `suspendMember`'s **409**, which
      carries a plain `application/json` `SuspensionBlockedWarning` (every other error body is
      `problem+json`). Derivation is now restricted to `2xx` — error payloads are not hypermedia
      resources. Left unfixed, the migration would have invented an `EntityModelSuspensionBlockedWarning`
      that nothing serves.
- [x] 2.6 Add `validate.mjs` rules: `x-hal-entity-items` must be `true`, only on `type: array` whose
      `items` is a `$ref`, and must not point at a schema already shaped as an envelope;
      `x-klabis-hal` must be `false` when present and only on an operation.
- [x] 2.7 Unit-test the deriver in `tools/openapi-bundle/test/` — both shapes, paged vs unpaged,
      declared vs derived `_embedded` key, `x-hal-entity-items`, opt-out, and the no-op property.

      `tools/openapi-bundle/test/derive.test.mjs` plus additions to `validate.test.mjs`. Suite total
      120, all passing; no existing test needed changing.

      Code review added a guard for `isEnvelopeShaped`'s one silent failure mode: it classifies any
      schema owning a `_links`/`_embedded` property as an already-written envelope, which is what
      protects the hand-written `EntityModelRootModel`/`EntityModelDashboardModel` marker types — but
      a genuine payload declaring its own `_links` would be skipped just as quietly. `validate.mjs`
      now reports that case. The rule is exact rather than heuristic because validation runs *after*
      derivation, so a HAL response still lacking a hal-forms entry is precisely one the deriver
      walked over. That ordering is load-bearing and now says so in `bundle.mjs`.
- [x] 2.8 Run `./gradlew openapiBundle` and confirm `klabis-full.json` still matches the 1.3
      checksum — the deriver is wired in but changes nothing yet.

      **Required a spec edit after all.** HAL is the default (Decision 4), so the 6 non-HAL operations
      get enveloped unless they say otherwise — and no `x-klabis-hal: false` marker existed anywhere
      yet. Byte-identity at this step therefore needed information the specs did not carry. Added the
      marker to `getMySchedule` (`calendar.yaml`), the four pre-auth password endpoints
      (`common.yaml`) and `listOrisEvents` (`oris.yaml`). That is a declaration of an already-true
      fact, not an envelope migration, so it belongs here rather than in sections 4.3/4.4.

      New baseline: `308677b5d09a7509009153c97e1c17fa9c458f8b5b1cdf787729cbe99b09d0d6`. The diff
      against the previous baseline is exactly the 6 marker lines — extensions are carried into the
      bundle as a matter of course (`x-klabis-authority` appears 95 times), so this is expected.
      Generated Java unchanged for all modules; `klabisApi.d.ts` and `halTypes.ts` unchanged.

## 3. Migrate `finance` (3 envelopes — proves the approach)

- [x] 3.1 Rewrite `docs/openapi/spec/finance.yaml`: delete the envelope schemas and bare-array `*List`
      siblings, drop the `application/prs.hal-forms+json` content entries, point the
      `application/json` responses at the payload schemas directly.

      **Delete those entries outright — do not leave them emptied as `{}`.** An emptied entry is
      still HAL boilerplate in the source spec, which is what this change exists to remove.
      `KlabisSpringCodegen.addDerivedHalFormsContentType()` re-adds the media type for the
      `produces` clause; see the correction under 3.5. Leave the bodyless 201/204 entries alone —
      they have no `application/json` sibling and declare the media type for a response with no
      schema.
- [x] 3.2 Run `./gradlew openapiBundle` and diff the bundle against the current baseline
      (`308677b5…`, see 2.8). (`git diff` cannot be used — the file is gitignored, see Decision 6.)

      **Expect a non-empty but bounded diff, not an unchanged hash.** Per Decision 7 the deriver
      emits `_templates` on every envelope, while the hand-written ones are inconsistent about it —
      so migrating a module whose envelopes omit `_templates` legitimately adds them. The check is
      that the diff contains *only* such `_templates` additions. Anything else — a changed
      `_embedded` key, a renamed schema, a lost property — means the deriver is wrong; fix the
      deriver, never the spec.
- [x] 3.3 Run `./gradlew openApiGenerateFinance`; confirm the generated Java under
      `build/generated/openapi/finance/` is unchanged against the 1.3 baseline, via
      `~/.cache/klabis-openapi-baseline/cmp-generated.sh finance` (a plain `diff -r` cannot be used —
      the `@Generated` timestamp changes every run).

      Result: 7 modules byte-identical. `finance` differs only by 4 **removed** empty `record X()`
      types (`EntityModel{MemberAccountResource,TransactionResource}AllOf{Value,OptionsInlineOneOf}`),
      emitted only because the generator walked the hand-written `allOf` wrappers; 0 usages in
      `src/`. The finance baseline is therefore stale by exactly those 4 files.
- [x] 3.4 Run backend tests via the `test-runner` agent; confirm no failures and no test edits needed.
      Result: finance 100/100, buildSrc 42/42, bundler 121/121, `npm run build` OK,
      `klabisApi.d.ts` unchanged vs HEAD.
- [x] 3.5 Commit. This is the go/no-go point for the whole change.

      **Two corrections were required here; both apply to every later module.**

      *Decision 7 was not actually implemented.* The deriver gated `_templates` on
      `x-hal-templates`, contradicting the design. `TransactionResource` is returned both by
      `getTransaction` (has it) and as the item of `listTransactions` (does not), so one payload
      derived two incompatible `EntityModelTransactionResource` definitions and hit the collision
      guard. `_templates` is now unconditional on response-level envelopes, keeping the
      `x-hal-entity-items` exception for nested rows.

      *`produces` is built from content-map keys, not from schemas.* `DefaultCodegen`'s private
      `addProducesInfo()` reads the response's content keys verbatim, so deleting the hal-forms
      entry dropped the media type from `produces` — and because method-level `produces` overrides
      `MemberAccountController`'s class-level `@RequestMapping`, those endpoints answered 406 to
      the Accept header the frontend sends. `KlabisSpringCodegen.addDerivedHalFormsContentType()`
      now adds the media type itself for the same responses the deriver walks. The "is this a HAL
      response" rule consequently exists on both sides of the language boundary
      (`derive.mjs`'s `forEachHalResponse` ↔ `isHalResponse`/`isHalOptedOut`) — **change one,
      change the other.**

      Landed as `563ad846` (migration, containing the rejected emptied-entry intermediate) plus a
      follow-up commit correcting it.

## 4. Migrate the remaining simple modules

- [x] 4.1 `members` (3 envelopes, 1 `x-spring-paginated`) — same 5 steps as section 3, one commit.
      `cdca77ca`, tests 608/608. Also resolved task 1.5: both `description` lines went with the
      schemas that carried them, no separate edit needed.

      `MembersApi.java` differs from baseline by `@Content`/`produces` **ordering** only — the spec
      listed hal-forms first, the codegen now appends it. Entry counts identical; `produces` is a
      content-negotiation set and `@Content` is documentation, so this is a permutation, not a
      change. **Expect the same wherever a spec listed hal-forms before `application/json`.**
- [x] 4.2 `calendar` (3 envelopes, 1 `x-klabis-hal: false` on `getMySchedule`) — same, one commit.
      `d6a09b04`, tests 289/289. `CalendarApi.java` byte-identical to baseline.

      This is the module that justifies Decision 7's unconditional `_templates`: `IcalTokenResponse`
      is returned by two operations, only one declaring `x-hal-templates`. Gating on that extension
      derives two incompatible `EntityModelIcalTokenResponse` definitions and hits the collision
      guard.
- [x] 4.3 `common` — **partial by design**, one commit `aa3d7f93`, tests 619/620.

      Only `EntityModelPermissionsResponse` is derived. `EntityModelRootModel` and
      `EntityModelDashboardModel` stay hand-written: `RootModel`/`DashboardModel` are empty marker
      records with no `application/json` payload to derive from, and adding one collapses the
      generated return type to `ResponseEntity<Void>` (documented empirically in the module header).
      A comment at both schemas records why they are exempt.

      The one test failure, `RootControllerTest.shouldAddAdminLinkForDeveloper`, is pre-existing and
      unrelated — `57dec7bf` renamed the admin link target from `/sandplace` to `/admin` without
      updating the test. Verified failing on clean HEAD.
- [x] 4.4 `oris` (`listOrisEvents` needs only `x-klabis-hal: false`) — **no work required**: the
      marker was added in 2.8 and the response was already an inline array with no envelope schema.

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
