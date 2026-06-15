## Context

Membership fee administration is today reachable through two separate main-menu destinations:

- **Tier catalog** (`/membership-fee-tiers`) — list of fee tiers with a create action, backed by `GET /api/membership-fee-tiers`.
- **Fee selection campaigns** (`/fee-selection-campaigns`) — flat list of campaign years with a publish-year action, backed by `GET /api/fee-selection-campaigns`. Clicking a year opens a standalone campaign detail page showing the campaign's fee groups.

Both list pages share the same structural shape (header + primary action + HAL embedded table). The menu is HAL-link driven: each module contributes root links via a postprocessor, and the frontend `useRootNavigation` hook assigns each link to the main or Administrace section based on a fixed set of admin rels.

The domain already guarantees that a published campaign has at least one fee group (`FeeSelectionCampaign.publish` asserts a non-empty tier selection), and there is at most one active campaign at a time (active = voting deadline has not passed).

This change merges the two pages into one administrative page with three sections, driven from the tier catalog resource as the single entry point.

## Goals / Non-Goals

**Goals:**

- One administrative page "Členské příspěvky" in the Administrace menu section replacing the two existing entries.
- Reuse existing UI as much as possible — extract sections and the fee-groups table into reusable components rather than rewriting.
- Keep all existing actions (create tier, publish year, change deadline, navigate to tier / campaign / group detail).
- Drive the page purely through HATEOAS: the tier catalog resource carries the links needed to populate all three sections for an administrator.

**Non-Goals:**

- No change to the member-facing fee choice page (`MemberFeeChoicePage`).
- No change to the campaign detail page's internal behavior (only its "back" target).
- No domain model changes — the ≥1-group invariant and single-active-campaign rule already hold.
- No change to the underlying fee/campaign/group data model or persistence.

## Decisions

### Decision 1: Tier catalog resource is the page's single entry point

The merged page loads `GET /api/membership-fee-tiers` and renders all three sections from that one resource:

- Section 2 (tier catalog) from the embedded tiers and the existing `createTier` template.
- Section 1 (active campaign) by following an `activeCampaign` link.
- Section 3 (past campaigns) by following a `pastCampaigns` link.

The `activeCampaign` and `pastCampaigns` links are added only for users with `MEMBERS:MANAGE`. `activeCampaign` is present only when an active campaign exists, so the frontend shows section 1 exactly when the link is present. `pastCampaigns` targets the existing campaign list endpoint with a `?status=closed` filter (see REST API Changes) so the active campaign is excluded server-side.

**Why:** Keeps the page hypermedia-driven with no client-side business logic for "which campaign is active". The frontend reacts to link presence rather than recomputing the active-campaign rule (which lives in the domain).

**Alternatives considered:**

- *Flag on each campaign summary (`active: true/false`)* — pushes the active/closed split to the client and still requires the client to fetch the active campaign's detail separately.
- *Client derives active from `votingDeadline` vs today* — duplicates a domain rule on the client; brittle.

### Decision 2: Active campaign rendered as full inline detail; excluded from past list

Section 1 renders the active campaign's full detail inline (year, voting deadline, change-deadline action, fee groups table), reusing the campaign detail content. The active campaign year is excluded from the `pastCampaigns` list so it is not duplicated.

**Why:** Matches the user's mental model — the active campaign is the thing being worked on, shown prominently; past campaigns are an archive.

**Alternatives considered:**

- *Compact summary + link to detail page* — extra navigation for the most-used campaign.
- *Inline expand/accordion in the list* — more rework; the detail page would have to become a component.

### Decision 3: Campaign and tier detail remain standalone pages

Clicking a past campaign opens the existing `FeeSelectionCampaignDetailPage`; clicking a tier opens the existing `MembershipFeeTierDetailPage`. Only their "back" navigation is retargeted to the merged page.

**Why:** Minimal change, preserves deep-linkable detail URLs and existing tests.

### Decision 4: Extract sections and the fee-groups table into components

The merged page is a thin composition of three section components — active campaign, tier catalog, past campaigns — plus a shared fee-groups table component reused by the active-campaign section and the campaign detail page. New components live under `frontend/src/components/membership-fees/`, following the convention already established there (`RulesTable`, `CoParticipationRuleTypeBadge`).

**Why:** The fee-groups table is currently hand-written inline in the campaign detail page and would otherwise be duplicated in the active-campaign section. Extraction satisfies the "create components and reuse" goal and keeps the merged page declarative.

### Decision 5: Navigation — one new rel, two old rels removed

A single root link with a new rel (`membership-fees`) is contributed for `MEMBERS:MANAGE` users and added to the frontend's admin-rel set so it lands in the Administrace section. The previous root links for `membership-fee-tiers` and `fee-selection-campaigns` as menu entries are removed.

**Why:** One destination, one menu entry, placed where administrators expect it.

## REST API Changes

### `GET /api/membership-fee-tiers` (modified response)

Unchanged: embedded tier summaries and the `createTier` affordance.

Added `_links` for callers with `MEMBERS:MANAGE`:

| Link rel | Target | Presence |
| --- | --- | --- |
| `activeCampaign` | The active fee selection campaign detail resource (`/api/fee-selection-campaigns/{id}`) | Only when an active campaign exists |
| `pastCampaigns` | Collection of closed campaigns: `/api/fee-selection-campaigns?status=closed` | Always, for administrators |

No request/response body changes to the tier list payload itself; only links are added. Links are omitted for non-administrators.

### `GET /api/fee-selection-campaigns` (modified — optional `status` filter)

The existing campaign list endpoint gains an optional `status` query parameter:

| `status` value | Result |
| --- | --- |
| absent | all campaigns (unchanged from today) |
| `closed` | only closed (past) campaigns — the active campaign is excluded |

The response shape is unchanged: campaign summaries (year, voting deadline, self link to campaign detail). The `pastCampaigns` link from the tier catalog targets this endpoint with `?status=closed`, so the past-campaigns section never includes the active campaign. Other values MAY be rejected or treated as "all"; only `closed` is required by this change.

### Root navigation links (modified)

| Before | After |
| --- | --- |
| `membership-fee-tiers` root link (menu entry) | removed as a menu entry |
| `fee-selection-campaigns` root link (menu entry) | removed as a menu entry |
| — | `membership-fees` root link (Administrace), `MEMBERS:MANAGE` only |

Existing resource-level rels used for in-page navigation (`activeCampaign`, `pastCampaigns`, campaign `self`, group `self`, `levels`, `changeDeadline` affordance) are unaffected except as listed above.

## Domain Changes

No domain changes. The relevant invariants already hold:

- A published campaign has at least one fee group (`FeeSelectionCampaign.publish` rejects an empty tier selection).
- At most one campaign is active at a time (enforced when publishing).

The frontend publish-year form SHOULD continue to require selecting at least one tier so the request does not fail server-side; this is existing behavior, not a new requirement.

## Risks / Trade-offs

- **[Removing two menu entries is a visible navigation change]** → Covered by the `application-navigation` spec delta and by updating `useRootNavigation` tests; the merged entry is authorization-gated identically to the old admin items.
- **[`activeCampaign` link only present conditionally — frontend must handle absence]** → The section-1 component renders only when the link exists; this is the intended trigger and is covered by spec scenarios.
- **[Extracting the fee-groups table could regress the campaign detail page]** → The campaign detail page is migrated to the shared component in the same change and its existing tests are kept green.

## Glossary

- **Active campaign**: The single fee selection campaign whose voting deadline has not yet passed.
- **Past campaign**: A closed fee selection campaign (voting deadline passed); listed in section 3.
- **Tier catalog**: The reusable template list of membership fee tiers (section 2).
- **Fee group**: A per-year published snapshot of a tier within a campaign; members choose among the campaign's fee groups.
