## Why

The term "fee level" is confusing — it reads as a numeric rank rather than what it actually is: a named membership category (e.g. "Adult competitor", "Junior", "Recreational member") carrying a yearly fee and co-participation rules. At the same time, the catalog's rule management is only half-built: the frontend detail page already offers per-rule add/delete affordances, but the backend has no per-rule endpoints — rules can only be replaced as a whole list. The ranking field is a free-text input even though the valid values come from the ORIS code list.

## What Changes

- **BREAKING** Rename the "membership fee level" concept to "membership fee **tier**" across the whole application — domain classes, DB columns, REST URLs (`/api/membership-fee-levels` → `/api/membership-fee-tiers`), frontend components/routes, and Czech labels. The rename MUST be performed using IntelliJ MCP rename-refactoring tools to keep references consistent.
- Add **per-rule management endpoints** on a catalog tier:
  - `POST /api/membership-fee-tiers/{id}/rules` — add a rule (rejects a duplicate event-type + ranking combination)
  - `PATCH /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}` — edit a rule's value only (the key is immutable)
  - `DELETE /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}` — remove a rule
- **BREAKING** Tier creation no longer accepts a `rules[]` array — a tier is created with name + yearly fee only; rules are added afterwards through the per-rule endpoints.
- The rule form exposes a **ranking dropdown** populated from the ORIS code list (`OrisApiClient.listLevels()`), served as HAL-FORMS inline options on the add-rule affordance — replacing today's free-text ranking input. This follows the existing pattern used for event-type discipline options.
- Fix the rule response payload so `eventTypeId` is carried as a proper typed ID value object instead of a bare UUID (consistency with the rest of the API). The frontend continues to resolve the event-type display name itself via the existing `useEventTypes()` hook.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `membership-fees`: The "Membership Fee Level Catalog" and "Membership Payment Rules" requirements are reworded from *level* to *tier*, rule management becomes a set of explicit per-rule operations (add / edit-value / delete) instead of whole-list replacement, tier creation drops the rules argument, and the ranking value is selected from the ORIS code list rather than entered as free text.

## Impact

- **Backend** (`com.klabis.membershipfees`): rename `MembershipFeeLevel` → `MembershipFeeTier`, `MembershipFeeLevelId` → `MembershipFeeTierId` and related application/infrastructure/JDBC/REST types (~195 references). New rule sub-resource endpoints + commands on the management port. ORIS ranking options wired into the add-rule affordance. DB migration script V001 updated (table/column rename).
- **Events module dependency**: read access to `OrisApiClient.listLevels()` for ranking options (same cross-module pattern already used for discipline options).
- **Frontend** (`src/pages/membership-fees/`): rename components/routes, wire per-rule add/edit/delete to the new endpoints, replace the free-text ranking input with the ORIS-backed dropdown, update Czech labels in `src/localization/labels.ts`.
- **API consumers**: URL and request-shape changes are breaking. The application is pre-production with no external consumers, so no deprecation window is needed.
- **OpenSpec**: `specs/membership-fees/spec.md` updated via delta.
