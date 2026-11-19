## Context

Pure rename "tier"/"level" → "type" of the membership fee catalog item (see proposal.md). The concept appears in the OpenAPI spec (paths, operation ids, schemas, HAL template names), generated Java and TypeScript, the `membershipfees` module, a few references in `members`, the React frontend and E2E tests. `MembershipFeeGroup` is the per-year published snapshot and stays as is.

## Decisions

### D1: Naming map

| Old | New |
|---|---|
| `MembershipFeeTier`, `MembershipFeeTierId`, `*TierRepository`, `*TierManagement*`, `*TierOptions*`, `MembershipFeeTierNotFoundException`, `MembershipFeeTierController`, `MembershipFeeTierResponse*` | same with `MembershipFeeType` |
| `/api/membership-fee-tiers[...]` | `/api/membership-fee-types[...]` |
| `listTiers`, `createTier`, `getTier`, `editTier`, `deleteTier`, `chooseTier` | `listFeeTypes`, `createFeeType`, `getFeeType`, `editFeeType`, `deleteFeeType`, `chooseFeeType` |
| HAL-FORMS type `MembershipFeeTierMultiSelect` | `MembershipFeeTypeMultiSelect` |
| `tierIds`, `levelIds`, `source_level_id`, `recommendedLevelId` and other `level`/`tier` names that refer to the catalog item | `typeIds`, `sourceTypeId`, `recommendedTypeId`, … |
| SPA `/membership-fee-tiers[/:id]` | `/membership-fee-types[/:id]` |
| `/errors/membership-fee-tier-not-found` | `/errors/membership-fee-type-not-found` |

Generic words ("level" in unrelated code such as log levels, permission levels) are NOT touched. Methods such as `getLevelHistory`, `CurrentLevelInfo`, `chooseFeeLevel` (fee-choice flow) are renamed to `type` wording only where they denote the catalog item/assignment; `MembershipFeeGroup` stays.

Clash to watch: `EventType` is a different concept used inside fee type rules (`/membership-fee-types/{id}/rules/{eventTypeId}/{ranking}`); keep identifiers unambiguous (`feeType` vs `eventType`).

### D2: Database

Rename tables/columns with a new Flyway migration (`ALTER TABLE ... RENAME`, constraint/index rename, comments), not by editing `V001`: production databases already applied `V001`, a changed checksum would fail startup. Data is preserved. Affected: `membership_fee_tier`, `membership_payment_rule.membership_fee_tier_id` (+ unique constraint, index), `membership_fee_group.source_level_id`; the memento/JDBC mapping follows.

### D3: Order and verification

Rename the OpenAPI spec first, regenerate bundle + backend + frontend types, then fix the compile errors (compiler drives the rename); finish with the frontend, then E2E. The proof of "no behavior change" is that every test passes with only mechanical renames of identifiers/URLs (no assertion changed) and a grep finds no `tier` left outside `MembershipFeeGroup` history comments and the old migration.

Use IDE rename refactoring (JetBrains MCP, not in a git worktree) for classes and symbols; text/URL occurrences in YAML, TS and Gherkin by search-and-replace.

## Risks / Trade-offs

- [API and SPA URLs change; old bookmarks break] → accepted, only our frontend consumes the API, deployed together.
- [A generated name collides or the codegen drops a mapping silently] → regenerate and check the generated `*Api` interfaces exist; backend compiles; `schemaMappings`/`x-hal-input-type` entries updated together with the schema names.
- [Missed HAL template name in frontend (`chooseTier`) fails silently at runtime] → grep for old template/rel names across `frontend/src` and `e2e`, plus E2E membership-fee scenarios.
- [Migration on production data] → the migration only renames; run it against a copy with `example-data` before release.
