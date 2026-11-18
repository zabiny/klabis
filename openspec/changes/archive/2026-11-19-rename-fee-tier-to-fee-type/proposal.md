## Why

Business terminology for membership fees is "typ členského příspěvku" (issue #439), but code, API and routes still use "tier". The mismatch forces every developer to translate between the domain language and the code. The rename is done as a pure refactor first, so the functional change `membership-fee-type-member-page` builds on the final names.

## What Changes

- Backend (`membershipfees` module): `MembershipFeeTier*` → `MembershipFeeType*` (aggregate, id, ports, services, exceptions, memento, repository, options port/service); `tier*`/`level*` identifiers referring to this concept renamed to `type`.
- Error type `/errors/membership-fee-tier-not-found` → `/errors/membership-fee-type-not-found`.
- OpenAPI spec (`docs/openapi/spec/membershipfees.yaml`) and generated code:
  - paths `/api/membership-fee-tiers[/{id}[/rules...]]` → `/api/membership-fee-types[...]`
  - operations `listTiers`, `createTier`, `getTier`, `editTier`, `deleteTier`, `chooseTier` → `listFeeTypes`, `createFeeType`, `getFeeType`, `editFeeType`, `deleteFeeType`, `chooseFeeType`
  - HAL-FORMS field type `MembershipFeeTierMultiSelect` → `MembershipFeeTypeMultiSelect`
  - schema names `*MembershipFeeTier*` → `*MembershipFeeType*`
- Other modules referencing the concept (e.g. `members` ORIS sync adapter, member registration options).
- Frontend: SPA routes `/membership-fee-tiers[/:id]` → `/membership-fee-types[/:id]`; `MembershipFeeTierDetailPage`, `TierCatalogSection`, `useMembershipFeeTierOptions`, `KlabisFieldsFactory` case, `halTypes.ts` keys; generated types regenerated from spec.
- E2E: steps and support code (`membershipFeesApi.ts`, `membershipFeesPages.ts`, step files) updated to new URLs and names.
- Database table/column names: renamed only if they contain "tier" (migration with no data change; decided in design.md).
- **Not changed:** `MembershipFeeGroup*` (published snapshot) keeps its name. Czech UI labels and OpenSpec spec text are handled by `membership-fee-type-member-page`.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/membership-fees/spec.md` — describes behavior, contains no identifiers, paths or operation names; the word "tier" appears only in prose.
- `openspec/specs/application-navigation/spec.md`, `openspec/specs/users/spec.md` — "tier" appears only in prose (menu entry and shared-link scenario); the user-visible menu and page stay the same.

**Why no spec update is needed:**
Every scenario keeps its outcome. Only internal names change: classes, OpenAPI paths and operation ids, HAL template names, SPA route paths. The only API client is the Klabis frontend, deployed together with the backend, so no backward compatibility is kept. Side effect: old bookmarks of `/membership-fee-tiers` stop working (accepted, no redirect). Czech UI texts remain untouched in this change.

## Impact

- Modules: `backend/.../membershipfees`, `members` (references), `docs/openapi/spec`, `frontend/src`, `frontend/e2e`.
- Generated artifacts (`klabis-full.json` bundle, backend API interfaces/models, `klabisApi.d.ts`, `halTypes.ts`) must be regenerated.
- Backend and frontend must be deployed together.
- Must be implemented **before** `membership-fee-type-member-page`.
- Verification: full backend suite, frontend `npm run build` + vitest, E2E membership-fee scenarios.
