## Why

Custom HAL+FORMS field types (types the base `halFormsFieldsFactory` doesn't know) are today injected by *wrapping* the base factory via `expandHalFormsFieldFactory` — the custom factory runs, and on `null` falls through to the base. But the base factory's own `HalFormsCollectionField` recurses **only into the base factory** for each row, so custom element types inside a multi-item (`multi`) field never reach the custom factory. This forces per-feature workarounds (e.g. the `isMultipleProperty` guard in `eventFormFieldsFactory`) instead of the framework handling it once.

## What Changes

- **`halFormsFieldsFactory` gains a 3rd optional parameter** — a custom factory `(fieldType, conf) => ReactElement | null`, consulted **before the built-in `switch`** (a custom single field wins over defaults). The `multi` branch stays **first**, ahead of the custom factory, so a collection always routes to `HalFormsCollectionField` and the custom factory only ever answers per-row calls — no per-feature guard needed.
- **`HalFormsCollectionField` receives the FULL (custom-aware) field factory as an explicit prop** (its own component attribute) instead of reading `fieldFactory` off the shared `conf` (`HalFormsInputProps`). It recurses per-row through that full factory, so rows can be **either** custom types **or** built-in types.
- **`fieldFactory` is removed from `HalFormsInputProps`** — it was used solely to smuggle the factory into `HalFormsCollectionField`; with the explicit prop it no longer belongs on the shared input-props type.
- **`expandHalFormsFieldFactory` is reworked (or removed)** so custom types compose through the new 3rd parameter rather than an outer wrapper — the wrapper is what breaks recursion today. Existing custom factories (`klabisFieldsFactory`, `eventFormFieldsFactory`, member-filtered variant) are migrated to the new mechanism.
- **The `isMultipleProperty` guard in `eventFormFieldsFactory` is removed** — it becomes unnecessary once the collection field recurses through the custom-aware factory.

## No Behavior Change Justification

This is an internal refactoring of the frontend HAL+FORMS rendering framework. Field rendering output (which component renders for which HAL+FORMS property, including multi-item category rows) is unchanged for the user; the change only fixes *how* the factory composes so custom types work inside collections without per-feature guards. No API contract, no HAL+FORMS metadata, and no user-observable form behavior changes.

**Specs reviewed:**
- `openspec/specs/event-categories/spec.md` — unaffected; category editing scenarios (add/remove rows, fee) keep the same observable behavior, which regression tests already pin.
- `openspec/specs/event-registrations/spec.md` — unaffected; category selection at registration is unchanged.

**Why no spec update is needed:**
The change touches only the internal `components/HalNavigator2/halforms` framework and its consumers. Specs describe user-facing form behavior, which is preserved; there is no ADDED/MODIFIED/REMOVED requirement.

## Impact

- **Frontend framework:** `halforms/HalFormsFieldFactory.tsx` (3rd param, `expandHalFormsFieldFactory` rework), `halforms/types.ts` (`fieldFactory` removed from `HalFormsInputProps`), `halforms/fields/HalFormsCollectionField.tsx` (explicit factory prop), `halforms/HalFormsForm.tsx` (stops threading `fieldFactory` through `conf`).
- **Consumers:** `KlabisFieldsFactory.tsx` (base custom factory + member-filtered variant), `events/eventFormFieldsFactory.tsx` (guard removed).
- **Tests:** factory unit tests, `HalFormsCollectionField` tests, and the `eventFormFieldsFactory` integration test adjusted to the new signature/prop; the category collection regression coverage must stay green.
- **Developer workflow:** custom field types become composable without wrapper-induced recursion bugs — simpler mental model for future custom fields.
