# Design

## Context

See proposal.md for motivation. Implemented **after** `rename-fee-tier-to-fee-type`, so all names below are the post-rename ones (`MembershipFeeType`, `chooseFeeType`, `/api/membership-fee-types`). `MembershipFeeGroup` (published type of one year) keeps its name.

Current state:
- The member's page (`/members/:memberId/fee-history`) renders `MemberFeeSection` (status + link) and the history table. The link leads to `MemberFeeChoicePage` (`/members/:memberId/fee-choice/:year`), which builds the `fee-summary` URL by hand and renders radio cards without prices.
- `feeSummary` link on `fee-history` is added for everyone who may read the history, with the year from `relevantFeeYear` (active campaign year, else latest not-past campaign year, else current year). `chooseFeeType` / `removeChoice` templates exist only while voting is open and the caller has `MEMBERS_EDIT_PROFILE` (owner or guardian); an administrator with only `MEMBERS_MANAGE` receives the summary without templates.
- History entries (`FeeAssignmentResponse`) carry year, group id/name, joined date and source — no price. The published group already stores `yearlyFeeSnapshot`.

## Goals / Non-Goals

**Goals:**
- One place on the member's page to see and decide the membership fee type, with prices.
- The section exists only when the member may act on it and a campaign is active; this is decided by the backend, not by frontend guesswork.
- History shows the price paid per year.

**Non-Goals:**
- Detailed display of the current type outside the history table (deferred, per issue).
- Changing sanctions, deadlines, emergency assignment or administrator pages.
- The unused `GET /api/members/{id}/fee-choice/{year}` resource stays as it is (not part of this change).

## Decisions

### D1: HAL link controls the section; frontend does not decide visibility

The `feeSummary` link on `fee-history` is added only when **all** hold: a campaign is active (`findActiveCampaign` present) and the caller may choose for the member (member themself or guardian with the choice right — the same check that gates the `chooseFeeType` affordance). The link carries no year: `GET /api/members/{memberId}/fee-summary` resolves the active campaign's year itself and answers 404 when no campaign is active (nobody follows the link then). The response gains `year` for display. `MemberFeeSection` renders only when the link is present and follows it; it no longer builds URLs.

Alternatives: (a) keep the link for everybody and hide in the frontend by template presence — rejected, the issue asks that the backend not offer the section, and administrators would still receive data they cannot act on; (b) deny `fee-summary` to administrators altogether — rejected, would be a breaking authorization change for an endpoint that stays accessible to `MEMBERS_MANAGE` for reading.

Active campaign includes the period after the deadline until the campaign is closed, so the locked/closed-voting states in the spec are rendered by the same section. `relevantFeeYear` is still used elsewhere; the history and summary endpoints stop using it.

### D2: Offered types and prices come from the options link of `chooseFeeType`

The `chooseFeeType` template's `membershipFeeGroupId` property carries `options.link` pointing at `GET /api/fee-selection-campaigns/{year}/groups` (`listGroupsForYear`, built via `HalFormsOptionsDef.Remote`), instead of inline options. That endpoint already returns the published groups with `id`, `name` and `yearlyFeeAmount`, so nothing is copied into `MemberFeeSummaryResponse`. The property is pinned with `x-hal-input-type: MembershipFeeGroupChoice`; a dedicated field component in the frontend fetches `options.link.href` and renders each group with name and yearly price (the generic `convertToSelectOptions` expects `{value, prompt}` and cannot map this payload). The recommended group still comes from `recommendedGroupId` in the summary. `id`s have a single source, so options cannot drift from the shown prices.

Alternatives: separate `feeGroups` HAL link on the summary — rejected, the form would not be self-describing and the template would still need its own options; `offeredGroups` copied into the summary — rejected, duplicates an existing API.

### D3: Choice UI is merged into the section; separate page is removed

The radio-card selection, pre-selected recommended type, withdraw action and locked card move from `MemberFeeChoicePage` into `MemberFeeSection`. The page, its route and `/fee-choice/:year` SPA path are deleted. After a successful choose/withdraw the page data is refetched (existing mutation invalidation) so the section and history update in place.

### D4: History price is the published snapshot

`FeeAssignmentResponse` gets `yearlyFee`, read from the published group's `yearlyFeeSnapshot` via the history service (`LevelAssignment` read model gets the fee). Shown as a new "Cena" column (`labels.memberFee`), formatted as in other money columns. No data migration: the snapshot already exists.

### D5: Terminology sweep in UI texts

`labels.ts` (catalog, actions, section titles, error type labels, field labels such as "Tier poplatku", "Tiery") and Czech messages in backend error mapping switch to "typ členského příspěvku" in the right grammatical form. Identifier names are handled by the rename change, so this change touches texts only.

### D6: Published-groups endpoint renamed

`/api/fee-selection-campaigns/{year}/levels` becomes `/api/fee-selection-campaigns/{year}/groups` (it returns `MembershipFeeGroup`; "levels" was left over from the old terminology). `operationId` `listGroupsForYear` is unchanged. No backward compatibility, only our frontend consumes it.

### Domain model

No aggregate changes. Read models only:

```mermaid
classDiagram
    class MemberFeeHistoryPort
    class LevelAssignment {
        year
        groupId
        groupName
        yearlyFee
        joinedAt
        source
    }
    class CurrentLevelInfo {
        groupId
        name
        yearlyFee
        votingOpen
    }
    MemberFeeHistoryPort --> LevelAssignment
    MemberFeeHistoryPort --> CurrentLevelInfo
    LevelAssignment ..> MembershipFeeGroup : snapshot
```

| Element | Change |
|---|---|
| `LevelAssignment` | added `yearlyFee` |
| `MembershipFeeGroup` | unchanged |

### API (format after change)

| Endpoint | Change |
|---|---|
| `GET /api/members/{memberId}/fee-history` | `assignments[].yearlyFee` (number, CZK) added. HAL links: `self`, `member`, `feeSummary` — `feeSummary` present only with an active campaign and when the caller may choose |
| `GET /api/members/{memberId}/fee-summary` | `{year}` removed; year is the active campaign's, response gains `year`, 404 when no campaign is active. Links `self`, `group` unchanged. Template `chooseFeeType`: property `membershipFeeGroupId` has `x-hal-input-type: MembershipFeeGroupChoice` and `options.link` to `listGroupsForYear` (no inline options); `removeChoice` unchanged |
| `GET /api/fee-selection-campaigns/{year}/groups` | path renamed from `/levels`; payload unchanged (`id`, `name`, `yearlyFeeAmount`, …) |
| `GET/POST/DELETE /api/members/{memberId}/fee-choice/{year}` | unchanged (UI no longer uses a dedicated page) |

Both response schemas and the two paths change in `docs/openapi/spec/membershipfees.yaml`; bundle and frontend types are regenerated.

### Glossary

- **Typ členského příspěvku (membership fee type):** catalog item with a name, yearly fee and payment rules; formerly "tier"/"level".
- **Published type (`MembershipFeeGroup`):** snapshot of a type for one campaign year.
- **Sekce „Členský příspěvek":** part of the member's "Členské příspěvky" page where the type is chosen.

## Risks / Trade-offs

- [Old bookmarks to `/members/:id/fee-choice/...` break] → accepted; nothing else links to it, the member page is the entry point.
- [A guardian/owner check duplicated between the link condition and the affordance] → reuse the one existing authorization evaluation instead of a second implementation; cover both with a `@WebMvcTest` (member, guardian, administrator, other member).
- [Custom field depends on the `/groups` payload shape] → covered by a frontend test against the real response schema; the endpoint is the single source of ids and prices.
- [Direct `fee-summary` call without an active campaign] → 404 by design; the UI never calls it (link absent).
- [Terminology sweep misses a text] → check `labels.ts`, backend messages and E2E feature files with a final grep for `tier`/`úroveň` (excluding `MembershipFeeGroup` comments).
