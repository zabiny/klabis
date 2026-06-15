## 1. Backend — expose active/past campaign links on the tier catalog resource

- [ ] 1.1 Write a controller/postprocessor test asserting `GET /api/membership-fee-tiers` carries `activeCampaign` (when a campaign is active) and `pastCampaigns` links for a `MEMBERS:MANAGE` user, and carries neither link for a non-admin user
- [ ] 1.2 Write a test asserting `activeCampaign` is absent when no campaign is active, while `pastCampaigns` is still present for an admin
- [ ] 1.3 Add the `activeCampaign` and `pastCampaigns` links to the tier catalog response, gated on `MEMBERS:MANAGE`, with `activeCampaign` present only when an active campaign exists (minimal implementation to pass tests)
- [ ] 1.4 Write a test asserting `GET /api/fee-selection-campaigns?status=closed` returns only closed campaigns (active excluded) with self links, and that the endpoint without the parameter still returns all campaigns
- [ ] 1.5 Add the optional `status=closed` filter to the campaign list endpoint; point the `pastCampaigns` link at `?status=closed`
- [ ] 1.6 Refactor; confirm domain ≥1-group and single-active-campaign invariants remain untouched

## 2. Backend — replace the two menu root links with one merged-page link

- [ ] 2.1 Write a test asserting the API root navigation exposes a single `membership-fees` link for `MEMBERS:MANAGE` users and no longer exposes `membership-fee-tiers` / `fee-selection-campaigns` as root menu links
- [ ] 2.2 Write a test asserting a non-admin user receives no `membership-fees` root link
- [ ] 2.3 Replace the two old root-link postprocessor contributions with a single `membership-fees` root link gated on `MEMBERS:MANAGE`
- [ ] 2.4 Regenerate the OpenAPI spec / frontend API types if the link changes are reflected there

## 3. Frontend — shared FeeGroupsTable component

- [ ] 3.1 Write a test for a `FeeGroupsTable` component rendering group rows (name, member count, status badge) with navigation to group detail and an empty state
- [ ] 3.2 Extract the inline groups table from `FeeSelectionCampaignDetailPage` into `components/membership-fees/FeeGroupsTable.tsx`
- [ ] 3.3 Replace the inline table in `FeeSelectionCampaignDetailPage` with `FeeGroupsTable`; keep existing campaign-detail tests green
- [ ] 3.4 Retarget the campaign detail "back" breadcrumb to the merged membership fees page

## 4. Frontend — section components

- [ ] 4.1 Write a test for `TierCatalogSection` (renders tiers, create-tier action, row navigation to tier detail)
- [ ] 4.2 Implement `TierCatalogSection` by moving the current tiers-page UI into a component
- [ ] 4.3 Write a test for `ActiveCampaignSection` (renders inline year/deadline, change-deadline action, `FeeGroupsTable`; renders nothing when no active campaign link is present)
- [ ] 4.4 Implement `ActiveCampaignSection` following the `activeCampaign` link and reusing `FeeGroupsTable`
- [ ] 4.5 Write a test for `PastCampaignsSection` (lists closed campaign years, navigates to campaign detail, empty state)
- [ ] 4.6 Implement `PastCampaignsSection` following the `pastCampaigns` link

## 5. Frontend — merged page and routing

- [ ] 5.1 Write a test for the merged membership fees page composing the three sections from the tier catalog resource, including the case with and without an active campaign
- [ ] 5.2 Implement the merged page as a composition of `ActiveCampaignSection`, `TierCatalogSection`, and `PastCampaignsSection`
- [ ] 5.3 Add the route for the merged page and retarget tier-detail "back" navigation to it
- [ ] 5.4 Remove the standalone tiers-list and campaigns-list page routes/usages that are superseded (keep tier-detail and campaign-detail pages)

## 6. Frontend — navigation wiring

- [ ] 6.1 Write a test asserting `membership-fees` is mapped to the Administrace section and the old fee rels are no longer present as separate menu entries
- [ ] 6.2 Add `membership-fees` to the admin-rel set in `useRootNavigation` and add its Czech label "Členské příspěvky"; remove the old fee menu labels/rels as separate entries

## 7. Verification and cleanup

- [ ] 7.1 Run the full backend and frontend test suites; ensure >80% coverage on changed code and 100% on any touched domain logic
- [ ] 7.2 Manually verify in the browser (admin user) that the merged page shows all three sections, the active campaign is inline and excluded from the past list, and all actions (create tier, publish year, change deadline, navigate to tier/campaign/group detail) work
- [ ] 7.3 Verify a non-admin member does not see the "Členské příspěvky" entry
- [ ] 7.4 Run `refresh-backend-server-resources` to publish frontend changes
- [ ] 7.5 Run `openspec validate merge-membership-fees-admin-page --strict` and resolve any issues
