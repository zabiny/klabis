## Context

A HAL-FORMS modal is opened through one of four mechanisms (see proposal). The only one with a narrow interface is `HalFormButton`. It goes through `HalFormContext` to `HalFormsPageLayout`, which is mounted once in `pages/Layout.tsx`. Because the layout sits outside nested `HalRouteProvider`s (`HalSubresourceProvider`), the button snapshots its resource into `HalFormRequest.resourceContext`. Everything the button cannot express ends up hand-rolled in pages.

`HalFormDisplay` (17 props) stays the renderer. It owns submit, `Location` navigation, toast and 422 mapping. Since `deepen-mutation-cache-invalidation`, `useAuthorizedMutation` invalidates every `authorized` query, including the route query.

## Goals / Non-Goals

**Goals:**
- One module for "open template X of resource R in a modal and submit it". Callers name the template and the resource and nothing else.
- Callers stop knowing about `pathname`, the submit URL or `_templates` casts.
- `HalFormDisplay` stops knowing about members.
- Page tests exercise the real dialog through a HAL fixture and a stubbed `fetch`.

**Non-Goals:**
- Inline edit (`useInlineEditing`), the inline panel (`HalFormButton modal={false}` → `HalFormPanel`), `EventRegistrationDialog`, non-HAL-FORMS dialogs.
- Unifying dialog titles or success messages. That changes visible texts and would need a spec-driven change.
- A field-type registry (review candidate 3). The member filter only moves to the caller here.

## Decisions

### D1: Local hook, not a global context

```ts
const d = useTemplateDialog(name?: string, options?: TemplateDialogOptions);
// d.available: boolean                       — options.resource has template `name`
// d.availableOn(resource, name?): boolean
// d.label: string                            — button label for `name`
// d.open(resource?, name?): void             — arguments override options.resource / name
// d.dialog: ReactElement | null              — render anywhere in the caller's tree
```

The state lives with the caller and the dialog renders in the caller's tree. As a result the hook sees its own resource, so the `resourceContext` snapshot is no longer needed. Tests can render a component without `HalFormProvider`.

*Alternative rejected:* extending `HalFormRequest` with all options. That keeps the snapshot workaround and the hidden dependency on `HalFormsPageLayout`.

### D2: Resource instead of `resourceData` / `pathname` / `resourceUrl`

The hook takes one HAL object: the page resource or a row (owner, trainer, rule, list item). It looks the template up in `resource._templates` and builds the submit URL in this order:

1. `template.target`;
2. the path of `resource._links.self`;
3. for the page resource without `self`, today's `'/api' + normalizeKlabisApiPath(route.pathname)` fallback, kept internally so behavior does not change.

`HalFormDisplay` keeps its props. The hook computes them.

### D3: Template name fixed or late-bound

Detail pages fix the name (`useTemplateDialog('suspendMember', …)`). Row action menus (`MembersPage`, `EventsPage`, `CategoryPresetsPage`, `EventTypesPage`) create one hook without a name and call `open(row, templateName)`. Without a name, `available` is `false` and `label` is empty, so callers use `availableOn(row, name)`.

### D4: Narrow options

The options are `resource`, `title`, `size`, `note`, `submitLabel`, `successMessage`, `onSubmitSuccess`, `onSubmitError`, `navigateOnSuccess` and `fieldsFactory`, plus `postprocessPayload`. These are what the modal call sites use today. `submitIcon`, `refineValidationSchema` and `customLayout` are left out because only inline cases outside the scope use them. A new option is added with its first real caller.

Defaults are unchanged from today:
- title: `getDialogTitleLabel(name) ?? getTemplateLabel(name) ?? template.title`, the `HalFormButton` order;
- label: `getTemplateLabel(name) ?? template.title ?? name`, the `HalFormTemplateButton` order;
- success message: the `HalFormDisplay` default;
- `navigateOnSuccess`: `true`.

### D5: Titles are preserved per call site

Call sites build titles differently today, for example:
- `template.title ?? templateName` in `MembersPage`;
- `template.title ?? labels.templates[x]` in `CategoryPresetsPage`, `EventTypesPage`, `GroupDetailPage` delete/cancel and `TrainingGroupDetailPage`;
- fixed strings in `RulesTable` ("Upravit pravidlo", "Smazat pravidlo");
- `labels.dialogTitles.*` in `MemberDetailPage`.

Rule: every migrated call site that passes a title today keeps the same expression in `options.title`. When the expression depends on the template or its name, it goes in a `title: (template, name) => string` callback. Only call sites that pass no title today (the `HalFormButton` path) use the default.

Explanatory text shown above the form, such as the account activation confirmation in `MemberDetailPage`, moves to `note`. `Modal` renders `note` as a band under the title instead of a paragraph in the body. This changes placement only, not text or behavior, and was accepted so that all template dialogs look the same.

### D6: Member filter through `fieldsFactory`

`GroupDetailPage` passes `fieldsFactory: createMemberFilteredFactory(excludeIds, includeIds)`, memoised on the id list. The `excludeMemberIds` and `includeOnlyMemberIds` props and their branch in `HalFormDisplay` are deleted.

### D7: `HalFormButton` modal variant on the hook; context only for inline

`HalFormButton` with `modal` (the default) renders `HalFormTemplateButton` and calls `useTemplateDialog(name, {resource: resourceData, …})` itself. `modal={false}` keeps the current context path unchanged. `resourceContext` is removed from `HalFormRequest` and from `HalFormsPageLayout`. That is safe because the only inline buttons (`registerMember`, `createEvent`) are page-level and never inside a nested provider. `HalFormsPageLayout` then keeps only the inline branch.

### D8: Dialog rendered through a portal

`Modal` renders `fixed` divs without a portal. Today the context path renders every form modal at layout level, including the ones opened from `SyncStatusOverlay`, which is itself a `Modal`. With a local hook, the form modal would render inside the overlay's subtree. The hook therefore renders `dialog` through `createPortal(…, document.body)` to keep today's stacking.

### D9: No `route.refetch()` on close

The 7 `onClose={() => {…; void route.refetch();}}` handlers in `GroupDetailPage` and `TrainingGroupDetailPage` are dropped. A successful submit already refetches the route through `authorized` invalidation. A cancel changes nothing.

## Risks / Trade-offs

- **[Visible title or label drift]** → D5 keeps every expression. Page tests assert dialog titles for each migrated dialog.
- **[Dialog unmounts with its caller]** If the caller unmounts while the dialog is open (a row removed by a refetch), the dialog disappears. Today the same happens for the hand-rolled modals, so nothing is lost. The context path did not have this issue, but it has no row callers. Mitigation: after a successful submit the dialog closes before the refetch lands, so this only affects concurrent changes.
- **[Stacking regression in `SyncStatusOverlay`]** → D8, plus a test that opens `synchronizeNow` from the overlay.
- **[Inline and modal now use two mechanisms]** This is accepted until the inline step. `HalFormContext` becomes inline-only and its name stays.

## Migration Plan

Vertical slices, each committable with green tests (see tasks). `HalFormModal` and the member props are deleted in the last slice, after their callers are gone. To roll back, revert the slice.
