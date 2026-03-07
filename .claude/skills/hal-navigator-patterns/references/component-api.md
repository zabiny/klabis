# Component API Reference

## HAL Type System

Core types from `frontend/src/api/types.ts`:

```typescript
interface HalResponse {
  _links?: { [rel: string]: HalResourceLinks }
  _embedded?: HalEmbeddedResources
  _templates?: { [name: string]: HalFormsTemplate }
  [key: string]: unknown  // arbitrary additional properties
}

interface HalCollectionResponse extends HalResponse {
  page: PageMetadata  // { size, totalElements, totalPages, number }
}

interface HalFormsTemplate {
  method?: 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  target?: string       // URL for form data fetch + submission
  contentType?: string
  title?: string         // used as button label by HalFormTemplateButton
  properties: HalFormsProperty[]
}

interface HalFormsProperty {
  name: string
  prompt?: string        // field label
  type: string           // text, number, email, textarea, select, radioGroup, checkboxGroup, checkbox, boolean, datetime, date, url, tel
  value?: string | number
  required?: boolean
  regex?: string
  readOnly?: boolean
  options?: HalFormsOption  // { inline?: OptionItem[], link?: Link }
  multiple?: boolean
}

type EntityModel<T> = T & { _links: { [rel: string]: Link | Link[] } }
type Link = { href: string }
type SortDirection = 'asc' | 'desc'
```

## KlabisTable (Pure UI)

Location: `frontend/src/components/KlabisTable/KlabisTable.tsx`

Presentation-only table component. No data fetching.

**Props:**
- `data: T[]` — row data array
- `page?: TablePageData` — pagination metadata (`{ size, totalElements, totalPages, number }`)
- `error?: Error` — display error alert
- `onSortChange?(column: string, direction: SortDirection)` — sort callback
- `onPageChange?(page: number)` — page change callback
- `onRowsPerPageChange?(rowsPerPage: number)` — rows per page callback
- `onRowClick?(item: T)` — row click handler
- `currentPage`, `currentSort`, `rowsPerPage` — controlled state
- `children` — `<TableCell>` column definitions

## KlabisTableWithQuery (Data-Fetching Wrapper)

Location: `frontend/src/components/KlabisTable/KlabisTableWithQuery.tsx`

Wraps KlabisTable with React Query data fetching, pagination state, and sort state management.

**Props:**
- `link: Link` — HAL Link with href to fetch from
- `collectionName?: string` — extract from `_embedded[collectionName]` (otherwise uses `content`)
- `onRowClick?`, `defaultOrderBy?`, `defaultOrderDirection?` — passed through
- `children` — column definitions

**Behavior:**
- Manages page/size/sort query params automatically
- Uses `useAuthorizedQuery` for OAuth2-authenticated fetch
- Persists rows-per-page preference via `usePersistedState`

## HalEmbeddedTable (High-Level)

Location: `frontend/src/components/HalNavigator2/HalEmbeddedTable.tsx`

Highest-level table component. Uses `useHalPageData()` to get the current resource's self link, then delegates to KlabisTableWithQuery.

**Props:**
```typescript
interface HalEmbeddedTableProps<T> {
  collectionName: string          // key in _embedded
  onRowClick?: (item: T) => void
  defaultOrderBy?: string
  defaultOrderDirection?: SortDirection  // default 'asc'
  emptyMessage?: string           // default 'Zadna data'
  children: ReactNode             // TableCell definitions
}
```

## HalFormButton

Location: `frontend/src/components/HalNavigator2/HalFormButton.tsx`

Renders a button for a HAL-FORMS template. Returns `null` if template doesn't exist in current resource.

**Props:**
```typescript
interface HalFormButtonProps {
  name: string                    // template name in _templates
  modal?: boolean                 // default true
  customLayout?: ReactNode | RenderFormCallback  // custom form layout
}
```

**Form flow:**
1. Checks `resourceData._templates[name]` exists
2. On click (modal mode): calls `requestForm()` from `HalFormContext`
3. `HalFormsPageLayout` detects request, renders `HalFormDisplay`
4. `HalFormDisplay` fetches initial values from `template.target`, submits to same URL
5. On success: invalidates all query caches, refetches current resource, closes form

## HalFormsForm

Location: `frontend/src/components/HalNavigator2/halforms/HalFormsForm.tsx`

Formik-based form renderer from a HAL-FORMS template.

**Props:**
```typescript
interface HalFormsFormProps {
  data: Record<string, unknown>           // initial form values
  template: HalFormsTemplate             // HAL-FORMS template
  onSubmit?: (values: Record<string, unknown>) => Promise<void>
  onCancel?: () => void
  fieldsFactory?: HalFormFieldFactory     // custom field renderer
  renderForm?: RenderFormCallback         // custom form layout (render prop)
  children?: ReactNode                    // custom form layout (children)
  serverValidationErrors?: Record<string, string>
  submitButtonLabel?: string              // default 'Odeslat'
}
```

**Three rendering modes:**
1. `children` provided → wraps children in `HalFormsFormContext` (used by MultiStepFormModal)
2. `renderForm` provided → calls render callback with `renderField` function
3. Neither → auto-renders all template properties sequentially

**HalFormsFormContext** provides `renderField(fieldName: string)` function. Special field names:
- `'submit'` — renders submit button
- `'cancel'` — renders cancel button

## MultiStepFormModal

Location: `frontend/src/components/HalNavigator2/MultiStepFormModal.tsx`

Multi-step wizard layout for HAL-FORMS. Must be used within HalFormsForm (receives context).

**Props:**
```typescript
interface MultiStepFormModalProps {
  steps: FormStep[]
  nextButtonLabel?: string      // default 'Dalsi'
  backButtonLabel?: string      // default 'Zpet'
  submitButtonLabel?: string    // default 'Odeslat'
  showStepNumbers?: boolean     // default true
}

interface FormStep {
  title: string       // step title displayed to user
  fields: string[]    // field names matching HalFormsProperty.name
}
```

**Behavior:**
- Validates current step fields before allowing next
- Shows progress bar
- Resets to step 1 if submission fails with errors
- Renders cancel button via `renderField('cancel')`

## HalSubresourceProvider

Location: `frontend/src/contexts/HalRouteContext.tsx`

Fetches a linked resource and provides it as new HAL context for children.

**Props:** `subresourceLinkName: string` — link relation name in parent's `_links`

**Behavior:** Extracts link from parent's `_links[subresourceLinkName]`, creates new `HalRouteProvider` with that link. Children using `useHalRoute()` or `useHalPageData()` get the subresource data.

## HalFormsFieldFactory

Location: `frontend/src/components/HalNavigator2/halforms/HalFormsFieldFactory.tsx`

Maps HAL-FORMS property types to React components:

| HAL type | Component |
|----------|-----------|
| text, email, number, date, url, tel | HalFormsInput |
| textarea | HalFormsTextArea |
| checkbox | HalFormsCheckbox |
| checkboxGroup | HalFormsCheckboxGroup |
| radioGroup | HalFormsRadio |
| select | HalFormsSelect |
| boolean | HalFormsBoolean |
| datetime | HalFormsDateTime |

Extend with `expandHalFormsFieldFactory(customFactory)` to add custom field types.

## HATEOAS Utility Functions

Location: `frontend/src/api/hateoas.ts`

- `toHref(source: HalResourceLinks): string` — extract href from Link/array/string
- `isLink(item): item is Link` — type guard
- `normalizeUrl(input: string): string` — convert relative paths to absolute

Location: `frontend/src/utils/navigationPath.ts`

- `extractNavigationPath(href: string): string` — strip API base URL prefix, return router-compatible path

## Error Handling

**Form submission errors:**
1. Server returns 400 + `application/problem+json`
2. `toFormValidationError()` from `frontend/src/api/hateoas.ts` maps field errors
3. Errors shown inline on form fields or as error display

**Data fetch errors:**
- HalEmbeddedTable shows `ErrorDisplay` component
- Pages typically show `Alert` component with error message
