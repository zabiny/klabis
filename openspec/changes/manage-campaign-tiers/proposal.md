## Why

After a fee selection campaign is published, an administrator can currently only edit the yearly fee and payment rules of an already-published level (and only until the first surcharge). There is no way to correct the *composition* of a running campaign — adding a tier that was forgotten when publishing, or removing a tier that was published by mistake. Administrators routinely discover such mistakes right after publishing and today must wait for the campaign to close before fixing them.

## What Changes

- Allow a membership administrator to **add** a fee tier (catalog level) to an active, editable campaign after it has been published. Adding a tier publishes a new fee group for that campaign year as a snapshot of the catalog level, identical to how levels are snapshotted at publish time.
- Allow a membership administrator to **remove** a published fee tier (fee group) from an active campaign, but only when no member is assigned to that group. Removing a group that has members is rejected.
- Keep the existing ability to **edit** a published level's yearly fee and rules unchanged (already covered until the first surcharge); this change only adds the add/remove composition operations.
- Surface the add/remove operations as HAL-FORMS affordances on the campaign / fee-group resources, gated on `MEMBERS:MANAGE`, and present only while the campaign is editable.
- Frontend: the active-campaign section and the campaign detail page gain an "add tier" action and a per-group "remove" action (the latter shown only when the group has no members).

## Capabilities

### New Capabilities

<!-- none -->

### Modified Capabilities

- `membership-fees`: The existing "Starting a Fee Selection Campaign" / published-level behavior is extended so that an administrator can adjust the set of published levels of an *active* campaign — adding a new published level from the catalog, and removing a published level that has no assigned members. The single-active-campaign and "published level is a frozen-on-demand snapshot" rules are unchanged. The domain invariant that a published campaign has at least one fee group is preserved (removal cannot empty the campaign).

## Impact

- **Backend**: `membershipfees` module — new domain commands on the campaign / published-level aggregate (add level to active campaign, remove level when memberless); new application port operations; new REST endpoints/affordances on `FeeSelectionCampaignController` and/or `MembershipFeeGroupController` gated on `MEMBERS:MANAGE`; validation rejecting removal of a group with members and rejecting removal of the last group.
- **Frontend**: `ActiveCampaignSection` / `FeeSelectionCampaignDetailPage` / `FeeGroupsTable` gain add-tier and conditional remove-group actions driven by HAL-FORMS affordances.
- **Specs**: `membership-fees`.
- **Out of scope**: Editing the yearly fee/rules of an existing published level (already specified). Member-facing fee choice page. Closed (past) campaigns remain immutable.
