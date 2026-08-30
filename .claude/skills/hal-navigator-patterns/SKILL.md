---
name: hal-navigator-patterns
description: This skill should be used when the user asks to "implement frontend page in Klabis", "create HAL page component", "display HAL table", "add form modal", "work with HalRouteContext", "build page with KlabisTable", "add MultiStepFormModal", "render HAL+FORMS data", "navigate HAL resources", or mentions Klabis frontend patterns for HAL+FORMS API.
version: 0.2.0
---

# Klabis Frontend Patterns for HAL+FORMS API

Patterns for building React pages in Klabis that display and interact with HAL+FORMS API data.

## Architecture Overview

Pages in Klabis follow a layered architecture for HAL resource display:

```
Page Component
  ├── useHalPageData()           — access current HAL resource + helpers
  ├── HalEmbeddedTable           — display _embedded collections as paginated tables
  │   └── KlabisTableWithQuery   — data-fetching wrapper (React Query)
  │       └── KlabisTable        — pure UI table (no data fetching)
  ├── HalFormButton              — trigger HAL-FORMS templates as modal/inline forms
  │   └── HalFormsForm           — Formik-based form from HAL template
  │       └── MultiStepFormModal — multi-step form layout (optional)
  └── HalSubresourceProvider     — fetch nested HAL resources via links
```

## Core Hook: useHalPageData

Entry point for every page. Combines route data, actions, and admin state.

```tsx
import type { GetMemberResource } from '@/api';

const { resourceData, isLoading, error, isAdmin, route } = useHalPageData<GetMemberResource>();
```

**Type the response with the generated `*Resource` type.** `npm run openapi` emits one per HAL
operation into `frontend/src/api/halTypes.ts`, re-exported from `@/api`:
`GetMemberResource`, `ListEventsResource`, `GetFeeGroupResource`, … Each is `<payload schema> &
<typed _links/_templates>`. Passing it makes `resourceData.<field>`, `resourceData._embedded?.<list>`
and `resourceData._templates?.<rel>` all typed. Omit the type argument only on pages that read just
`route` / `isAdmin` and never touch `resourceData`.

- Do **not** write `useHalPageData<GetMemberResource & HalResponse>()` — `& HalResponse` collapses
  every payload field to `unknown`. Pass the bare `*Resource`.
- The generic is unconstrained (`<T = HalResponse>`); the hook reads the envelope through an
  internal `HalEnvelope` cast, so any `*Resource` is accepted.
- Legacy pages still use `EntityModel<{...}>` hand-written shapes and `resourceData as any` — that
  is the old pattern; use `*Resource` / `components['schemas'][...]` in new code.

**Key properties:**
- `resourceData` — fetched HAL response, typed as `T` (with `_links`, `_embedded`, `_templates`)
- `isLoading` / `error` — loading and error states
- `route.navigateToResource(resource)` — navigate to a HAL resource (accepts `HalResponse` or `Link`)
- `route.getResourceLink(linkName?)` — get specific link from current resource (defaults to `'self'`)
- `route.refetch()` — manually refetch current resource
- Helper methods: `hasLink()`, `hasTemplate()`, `isCollection()`, `getEmbeddedItems()`, `hasForms()`
  — `getEmbeddedItems()` stays `unknown[]`; for typed rows read `resourceData._embedded?.<listKey>`
  directly

## Displaying Collections: HalEmbeddedTable

Display `_embedded` collections as paginated, sortable tables. Automatically fetches data from the current resource's self link.

```tsx
<HalEmbeddedTable<MyItemType>
  collectionName="itemResponseList"
  defaultOrderBy="lastName"
  onRowClick={route.navigateToResource}
>
  <TableCell sortable column="firstName">Jmeno</TableCell>
  <TableCell sortable column="lastName">Prijmeni</TableCell>
  <TableCell column="email"
    dataRender={({value}) => <a href={`mailto:${value}`}>{value}</a>}
  >Email</TableCell>
  <TableCell column="_links"
    dataRender={props => <HalLinksSection links={props.value as any}/>}
  >Akce</TableCell>
</HalEmbeddedTable>
```

**Props:** `collectionName` (required), `defaultOrderBy`, `defaultOrderDirection`, `onRowClick`, `emptyMessage`, `children` (TableCell definitions)

**TableCell props:** `column` (data field name), `sortable` (enables sorting), `dataRender` (custom cell renderer receiving `{value, item}`)

**Type the row** with the generated schema type for the `_embedded` item, not a hand-written
`EntityModel<{...}>`:
```tsx
import type { components } from '@/api/klabisApi';

// row payload + EntityModel wrapper, straight from the spec
type MyRow = components['schemas']['EntityModelMemberSummaryResponse'];

// if the backend adds per-row affordances at runtime (not in the schema), extend:
type MyRow = components['schemas']['EntityModelMemberSummaryResponse'] & {
  _templates?: Record<string, HalFormsTemplate>;
};
```
Generated payload fields are all optional (`firstName?`), reflecting field-level authority — expect
`?.` / `?? ''` at the call site.

## Forms: HalFormButton

Render a button for a HAL-FORMS template. Automatically hidden if template doesn't exist in resource.

```tsx
<HalFormButton name="create" />                    // modal (default)
<HalFormButton name="edit" modal={false} />        // inline
<HalFormButton name="register" customLayout={...}/> // custom layout
```

Template existence is checked via `resourceData._templates[name]`. The button label comes from `template.title`.

## Multi-Step Forms: MultiStepFormModal

Split a HAL-FORMS template into wizard steps. Pass as `customLayout` to `HalFormButton`.

```tsx
const steps: FormStep[] = [
  { title: 'Krok 1: Osobni udaje', fields: ['firstName', 'lastName', 'dateOfBirth'] },
  { title: 'Krok 2: Kontakt', fields: ['address', 'contact'] },
  { title: 'Krok 3: Doplnkove udaje', fields: ['siCard', 'bankAccount'] },
];

<HalFormButton
  name="memberRegistrationsPost"
  customLayout={<MultiStepFormModal steps={steps} />}
/>
```

Field names in `steps` must match `HalFormsProperty.name` values from the template. MultiStepFormModal validates fields per step before allowing navigation to the next step.

## Nested Resources: HalSubresourceProvider

Fetch a related resource via a link from the parent resource and provide it as a new HAL context.

```tsx
<HalSubresourceProvider subresourceLinkName="owner">
  <OwnerDisplay />  {/* useHalRoute() here returns owner data */}
</HalSubresourceProvider>

<HalSubresourceProvider subresourceLinkName="transactions">
  <HalEmbeddedTable collectionName="transactionItemResponseList">
    <TableCell column="date">Datum</TableCell>
    <TableCell column="amount">Castka</TableCell>
  </HalEmbeddedTable>
</HalSubresourceProvider>
```

## Navigation Patterns

**Navigate to resource via row click:**
```tsx
onRowClick={route.navigateToResource}
```

**Navigate via HAL link (e.g. prev/next):**
```tsx
const navigate = useNavigate();
const prevLink = resourceData?._links?.prev;
if (prevLink) navigate(extractNavigationPath(toHref(prevLink)));
```

**Navigate to link from item details:**
```tsx
const handleNavigateToLink = (href: string) => {
  navigate(extractNavigationPath(href));
};
```

**Conditionally show buttons based on links:**
```tsx
{member._links?.editByAdminForm && (
  <Button onClick={() => handleNavigateToLink(member._links.editByAdminForm.href)}>
    Upravit (Admin)
  </Button>
)}
```

## Page Structure Template

Standard page loading/error/content pattern:

```tsx
export const MyPage = (): ReactElement => {
  const { resourceData, isLoading, error, route } = useHalPageData();

  if (isLoading) return <Spinner />;
  if (error) return <Alert severity="error">{error.message}</Alert>;

  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-3xl font-bold text-text-primary">Page Title</h1>

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-text-primary">Section</h2>
          <HalFormButton name="create" />
        </div>
        <HalEmbeddedTable collectionName="itemList" onRowClick={route.navigateToResource}>
          <TableCell sortable column="name">Nazev</TableCell>
        </HalEmbeddedTable>
      </div>
    </div>
  );
};
```

## Additional Resources

### Reference Files

For detailed component APIs and advanced patterns, consult:
- **`references/component-api.md`** — detailed props, types, and internals for KlabisTable, HalEmbeddedTable, HalFormsForm, MultiStepFormModal, and HalFormButton
- **`references/page-examples.md`** — complete source of real page implementations (MembersPage, EventsPage, FinancesPage, CalendarPage, MemberDetailsPage)

### Key Source Files

- `frontend/src/hooks/useHalPageData.ts` — main page data hook (`useHalPageData<T>()`)
- `frontend/src/contexts/HalRouteContext.tsx` — HAL route context + HalSubresourceProvider
- `frontend/src/components/HalNavigator2/HalEmbeddedTable.tsx` — embedded table
- `frontend/src/components/HalNavigator2/HalFormButton.tsx` — form button
- `frontend/src/components/HalNavigator2/MultiStepFormModal.tsx` — multi-step modal
- `frontend/src/components/HalNavigator2/halforms/HalFormsForm.tsx` — form renderer
- `frontend/src/api/index.ts` — api barrel; re-exports `halTypes.ts` (`Get*Resource`, `List*Resource`, `*Hal`, `*Rels`)
- `frontend/src/api/halTypes.ts` — generated per-operation HAL types (do not edit; `npm run openapi`)
- `frontend/src/api/klabisApi.d.ts` — generated OpenAPI types (`components['schemas'][...]`)
- `frontend/src/api/types.ts` — hand-written HAL media-type types (`HalResponse`, `HalFormsTemplate`, `HalEnvelope`, `EntityModel`)
