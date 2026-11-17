## 1. Core module

- [ ] 1.1 Red: test `halResource(undefined).template('x')` and `.link('y')` return `undefined`; a resource without `_templates`/`_links` returns `undefined` for any name
- [ ] 1.2 Red: test `template(name).href` — `template.target` wins, absent `target` falls back to the resource self link; `link(rel).href` is the link's `href`
- [ ] 1.3 Red: test the text conventions of `template(name)` — `label` from `labels.templates`, else `template.title`, else the name; `dialogTitle` from `labels.dialogTitles`, else `label`; `successMessage` is `<title> — úspěšně uloženo` when the template has a title and `labels.ui.savedSuccessfully` otherwise
- [ ] 1.4 Red: test `link(rel)` on a single link and on an array — the first non-`templated` link is returned; `label` from `labels.links`, else `link.title`, else the rel
- [ ] 1.5 Green: implement `src/api/halResource.ts` (`halResource`, `HalTemplateAction`, `HalLinkAction`, `getLinkLabel`)
- [ ] 1.6 Red/Green: add the missing tests for `toHref` and `asLinkArray`, including the empty-array and unknown-shape throws
- [ ] 1.7 Green: delete `templateTarget` from `src/api/hateoas.ts` and move its four callers to the module (`NotificationSettingsPage`, `usePushReregistration`, `FinancesPage`, `EventDetailPage`)
- [ ] 1.8 Verify: module, `hateoas` and migrated-caller tests green

## 2. Dialog, button and form pipeline

- [ ] 2.1 Red: test `useTemplateDialog.open(action, overrides)` — submits to `action.href`; per-open overrides win over the action's texts; a `undefined` action does not open; success, toast, `navigateOnSuccess` and `onSubmitError` behaviour is unchanged
- [ ] 2.2 Green: refactor `useTemplateDialog` to take a `HalTemplateAction`; remove `TemplateDialogOptions.resource`, `templateOf`, `available` and `availableOn`
- [ ] 2.3 Green: `HalFormDisplay` takes the resolved `href`; delete the `template.target ?? resourceUrl ?? selfHref ?? route` chain and update its direct (non-dialog) callers
- [ ] 2.4 Green: `HalFormButton` resolves the template through `halResource` while keeping every existing prop (`modal`, `label`, `icon`, `variant`, `dialogTitle`, `navigateOnSuccess`, `hideLabel`, `fieldsFactory`)
- [ ] 2.5 Green: `HalRouteContext.getResourceLink` delegates to `halResource().link()` with identical `templated` behaviour
- [ ] 2.6 Verify: `useTemplateDialog`, `HalFormButton`, `HalFormDisplay`, `HalRouteContext` and `HalFormsSection` tests green

## 3. Dialog call sites (phase 1)

- [ ] 3.1 Migrate `GroupDetailPage` (7 dialogs) and `TrainingGroupDetailPage` (5 dialogs); preserve each label, dialog title and success message exactly
- [ ] 3.2 Migrate `RulesTable` (3), `CampaignDetail` (2) and `LegalGuardiansSection` (1), keeping per-row availability and `fieldsFactory`/`initialData` overrides at the call site
- [ ] 3.3 Migrate `MemberDetailPage` (2), `MembersPage` (1), `MembershipFeeGroupDetailPage` (1) and `MembershipFeeTierDetailPage` (1), including the `status === 'EDITABLE'` conjunction in `MembershipFeeGroupDetailPage`
- [ ] 3.4 Migrate the late-bound dialogs: `EventsPage`, `EventTypesPage`, `CategoryPresetsPage` (`size` stays at the call site)
- [ ] 3.5 Verify: affected page and component tests green

## 4. Phase 1 verification

- [ ] 4.1 Full frontend unit suite green
- [ ] 4.2 `npm run build` (type check) and `npm run lint` green
- [ ] 4.3 E2E smoke of the affected journeys — groups, training groups, member detail (activation, suspension), events, event types, category presets, fee campaign detail, fee tiers
- [ ] 4.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA

## 5. Row actions (phase 2)

- [ ] 5.1 Migrate `MembersPage` and `EventsPage` `_actions` columns to `halResource(row)` for both template and link actions
- [ ] 5.2 Migrate `EventTypesPage` and `CategoryPresetsPage` `_actions` columns; keep the submit target fallback to the row self link
- [ ] 5.3 Migrate `FinancesPage` and `EventDetailPage` `_actions` columns, including the `template.target` presence checks
- [ ] 5.4 Add the `labels.links` entries the migrated columns need so every rendered label stays identical
- [ ] 5.5 Verify: affected page tests green

## 6. Phase 2 verification

- [ ] 6.1 Full frontend unit suite green
- [ ] 6.2 `npm run build` (type check) and `npm run lint` green
- [ ] 6.3 E2E smoke of the affected journeys — member list, event list and detail, event types, category presets, finances
- [ ] 6.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA
