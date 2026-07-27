## Context

The frontend HAL+FORMS framework (`components/HalNavigator2/halforms`) renders form fields from HAL+FORMS metadata. The base resolver is `halFormsFieldsFactory(fieldType, conf)`; custom field types are added by wrapping it:

```ts
export const expandHalFormsFieldFactory = (additionalFactory: HalFormFieldFactory): HalFormFieldFactory =>
    (fieldType, conf) => additionalFactory(fieldType, conf) ?? halFormsFieldsFactory(fieldType, conf)
```

`klabisFieldsFactory` (and `eventFormFieldsFactory` on top of it) are built this way.

The multi-item (`multi`/`multiple`) branch of `halFormsFieldsFactory` delegates to `HalFormsCollectionField`, which renders per-row by calling a factory it reads off the shared `conf`:

```ts
// HalFormsInputProps carries fieldFactory? solely so the collection field can recurse
return fieldFactory(prop.type, indexedInputProps)   // HalFormsCollectionField
```

**The defect:** the collection field recurses through whatever `fieldFactory` was threaded into `conf`. In the base path that is `halFormsFieldsFactory` — which does **not** know custom types. Because custom types live *outside* the base factory (in the wrapper), a custom element type inside a `multi` field never reaches the custom factory. The wrapper sits above the base, but the recursion happens *inside* the base — below the wrapper.

This surfaced concretely in `eventFormFieldsFactory`: the `categories` field (`multi`, element type `CategoryRequest`) was mis-rendered until a defensive `isMultipleProperty` guard was added (change `event-category-identity`). That guard treats the symptom per-feature; the framework should compose custom types through collections on its own.

Two coupled smells:
1. Custom-type resolution is a wrapper *above* the base, but the collection recursion is *inside* the base — they can't meet.
2. `fieldFactory` lives on `HalFormsInputProps` (shared by every field), though only `HalFormsCollectionField` ever consumes it.

## Goals / Non-Goals

**Goals:**
- Custom field types resolve correctly inside multi-item collections without per-feature guards.
- Custom-type resolution composes through a single factory the collection field also uses.
- Remove `fieldFactory` from the shared `HalFormsInputProps` — pass it to `HalFormsCollectionField` as an explicit component prop.
- No change to rendered output for any existing form (regression-covered).

**Non-Goals:**
- No change to HAL+FORMS metadata, API contracts, or the set of built-in field types.
- No redesign of `HalFormsForm` orchestration beyond removing the `fieldFactory`-through-`conf` threading.
- No new custom field types introduced by this change.

## Decisions

### D1: `halFormsFieldsFactory` takes a custom factory as a 3rd optional parameter

New signature:

```ts
type CustomFieldFactory = (fieldType: string, conf: HalFormsInputProps) => ReactElement | null;

export const halFormsFieldsFactory = (
    fieldType: string,
    conf: HalFormsInputProps,
    customFactory?: CustomFieldFactory
): ReactElement | null => {
    // 1. multi FIRST — a collection is always a collection (see D2)
    if (isMultipleProperty(conf.prop) && !conf.prop.options && !conf.prop.suggest) {
        return <HalFormsCollectionField {...conf} fieldFactory={fullFactory(customFactory)} />
    }
    // 2. custom type BEFORE the built-in switch
    const custom = customFactory?.(fieldType, conf);
    if (custom) return custom;
    // 3. existing options / switch resolution unchanged ...
}
```

- **Why a parameter, not a wrapper:** the collection field can now recurse per-row through the *same* custom-aware factory, so custom element types resolve inside collections. The wrapper could never do this because the recursion lives inside the base.

### D2: `multi` branch runs FIRST, then custom, then built-ins

A collection of a custom type must still route the *array itself* to `HalFormsCollectionField` — otherwise a custom factory that knows the element type would answer the array-level call and render a single row, bypassing the collection entirely (exactly the original bug the `eventFormFieldsFactory` guard worked around). So the order is:

1. **If the prop is `multi`** (and not options/suggest) → `HalFormsCollectionField`, no matter whether the element type is custom or built-in.
2. Else consult `customFactory` — a custom **single** field wins over any built-in.
3. Else existing options / `switch` resolution.

The collection field calls back per row with `multiple:false`, so:
- **array-level call** (`multi:true`) → step 1 → collection field;
- **per-row call** (`multi:false`, type e.g. `CategoryRequest`) → step 2 → custom factory.

This is the same array/row split the `eventFormFieldsFactory` guard emulated, now intrinsic to the framework — the guard is deleted and custom factories never need to ask "am I the array or a row?".

### D3: `HalFormsCollectionField` receives the FULL (custom-aware) factory as an explicit prop

The collection field must render rows of **both** custom and built-in element types, so it recurses through the *full* factory, not the bare `customFactory`. Bind the custom factory into a full factory once and pass it as an explicit prop:

```ts
// helper — a full factory that knows custom + built-in types
const fullFactory = (customFactory?: CustomFieldFactory): HalFormFieldFactory =>
    (fieldType, conf) => halFormsFieldsFactory(fieldType, conf, customFactory);
```

```tsx
<HalFormsCollectionField {...conf} fieldFactory={fullFactory(customFactory)} />
```

Per row `HalFormsCollectionField` calls `fieldFactory(prop.type, indexedInputProps)`, which resolves via `halFormsFieldsFactory(..., customFactory)` — so a row can be a `CategoryRequest` (custom) **or** a `text`/`select`/… (built-in). `fieldFactory` is removed from `HalFormsInputProps`; `subElementProps` and `HalFormsForm` stop copying it through `conf`.

- **Why the full factory, not bare `customFactory`:** rows may be standard types; a bare custom factory would return `null` for them and the collection would render nothing. The full factory covers both.
- **Why explicit prop:** it is a collection-only concern; keeping it on shared input props misleads every other field and was the coupling that forced the wrapper approach.
- **No infinite recursion:** the array-level call sets `multi:true` (step 1); every per-row call the collection makes sets `multiple:false`, so it never re-enters step 1 for the same property.

### D4: `expandHalFormsFieldFactory` reworked to compose via the 3rd parameter

`klabisFieldsFactory` and the member-filtered variant currently produce a wrapped `HalFormFieldFactory`. They are re-expressed so their custom logic becomes a `CustomFieldFactory` bound into `halFormsFieldsFactory` via the `fullFactory` helper from D3 — the public entry point consumers pass to `HalFormButton`/`HalFormDisplay` is exactly `fullFactory(customLogic)`, keeping the `HalFormFieldFactory` shape. `eventFormFieldsFactory` composes its extra types (`CategoryRequest` row) with the klabis custom logic into a single `CustomFieldFactory`, then wraps it the same way. Because rows now recurse through this full factory, the `isMultipleProperty` guard is gone and a `categories` collection renders custom rows and any built-in row types alike. Call-sites (`EventsPage`, `EventDetailPage`, `HalFormDisplay`) are unaffected — they still receive a `HalFormFieldFactory`.

### D5: `memberIdFieldRenderer` (MemberId/UUID) no longer special-cases `multi`

Discovered during migration (task 3): `memberIdFieldRenderer` in `KlabisFieldsFactory.tsx` special-cased `isMultipleProperty(conf.prop)` to render one `HalFormsCheckboxGroup` for the whole array — but it only attached `conf.prop.options` (the exemption D2's `multi` branch checks for) *inside* its own custom-factory call, which under D2 now runs *after* the base's `multi` branch already decided to route to `HalFormsCollectionField`. This broke multi-select MemberId/UUID fields (e.g. event coordinators/leaders).

D2 stays exactly as specified (multi branch first, unconditionally) — the fix is in `KlabisFieldsFactory.tsx`, not the framework: `memberIdFieldRenderer` no longer special-cases `multi` at all. It always returns the single-field component (`HalFormsMemberId`, extended with a read-only branch that resolves the selected id to the member's display name via the same options already loaded by `useHalFormOptions`). A multi MemberId/UUID property now goes through the base factory's `multi` branch like any other collection, and `HalFormsCollectionField` recurses per-row into this same single-field renderer.

**Behavior change (accepted, not a regression):** multi-select MemberId/UUID fields (e.g. event coordinators) render as N separate dropdown rows with add/remove buttons, instead of one inline checkbox-group listing all members. Read-only MemberId/UUID fields (single or per-row) now show the resolved member name as text instead of a raw id or disabled control. Verified manually in the browser (event coordinators field) and via updated `KlabisFieldsFactory.test.tsx` / `HalFormsMemberId.test.tsx` coverage. The single-select case (the common path — e.g. "add trainer/member" pickers) is unaffected.

## Risks / Trade-offs

- **[Signature change ripples to call-sites of `halFormsFieldsFactory` / `expandHalFormsFieldFactory`]** → All call-sites are in-repo (`HalFormsForm`, `KlabisFieldsFactory`, tests). The 3rd parameter is optional, so `halFormsFieldsFactory(fieldType, conf)` keeps working; only the collection-recursion and the custom factories are migrated.
- **[Regression in existing forms]** → Field output is behavior-locked by existing unit + integration tests, including the category collection regression test from `event-category-identity`. The migration keeps them green; that suite is the safety net for "no behavior change".
- **[Two ways to add custom types during migration]** (old wrapper + new parameter) → Mitigated by migrating all three custom factories in the same change and removing `expandHalFormsFieldFactory`'s wrapper semantics, leaving one mechanism.

## Migration Plan

1. Add optional 3rd `customFactory` param to `halFormsFieldsFactory`; consult before switch; thread it into the `multi` → `HalFormsCollectionField` branch.
2. Add explicit factory prop to `HalFormsCollectionField`; recurse per-row via the 3-arg `halFormsFieldsFactory`. Remove `fieldFactory` from `HalFormsInputProps`, `subElementProps`, and `HalFormsForm` threading.
3. Rework `expandHalFormsFieldFactory` / `klabisFieldsFactory` / member-filtered variant to compose via the 3rd parameter.
4. Remove the `isMultipleProperty` guard from `eventFormFieldsFactory`.
5. Update tests to the new signature/prop; confirm the category collection regression test still passes.

**Rollback:** forward-only refactor with no data or API impact; revert the commit to restore the wrapper + `conf.fieldFactory` mechanism.
