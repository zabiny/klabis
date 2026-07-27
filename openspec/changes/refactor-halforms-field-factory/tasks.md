> **No behavior change:** existing form-rendering tests must keep passing with only mechanical updates (new signature/prop), preserving assertions. The `event-category-identity` category-collection regression test is the primary safety net.

## 1. Base factory: 3rd custom-factory parameter

- [ ] 1.1 Add optional `customFactory` (3rd param) to `halFormsFieldsFactory`; keep the `multi` branch first, then consult `customFactory` before the built-in `switch`, returning its element when non-null (D1, D2)
- [ ] 1.2 Add the `fullFactory(customFactory)` helper and pass it to `HalFormsCollectionField` from the `multi` branch (D3)
- [ ] 1.3 Update/extend `HalFormsFieldFactory.test.tsx`: `multi` routes to collection even for a custom element type, custom single field wins over defaults, `null` falls through, a collection of built-in element type still works

## 2. Collection field: explicit factory prop

- [ ] 2.1 Add explicit factory prop to `HalFormsCollectionField`; recurse per-row via 3-arg `halFormsFieldsFactory(prop.type, indexedInputProps, fieldFactory)` (D3)
- [ ] 2.2 Remove `fieldFactory` from `HalFormsInputProps` (`types.ts`), from `subElementProps`, and from `HalFormsForm` threading (D3)
- [ ] 2.3 Update `HalFormsCollectionField.test.tsx` to pass the factory via the explicit prop; confirm add/remove and per-row custom rendering

## 3. Custom factories migrated to the new mechanism

- [ ] 3.1 Rework `expandHalFormsFieldFactory` to compose custom logic via the 3rd parameter instead of an outer wrapper (D4)
- [ ] 3.2 Migrate `klabisFieldsFactory` and the member-filtered variant in `KlabisFieldsFactory.tsx` to the new mechanism; keep the public `HalFormFieldFactory` shape for consumers
- [ ] 3.3 Rework `eventFormFieldsFactory` onto the new mechanism and remove the `isMultipleProperty` guard

## 4. Documentation

- [ ] 4.1 Update the `hal-navigator-patterns` skill (`references/component-api.md`) to document the new `halFormsFieldsFactory(fieldType, conf, customFactory?)` signature and the `fullFactory` helper, replacing the old `expandHalFormsFieldFactory(customFactory)` wrapper guidance; explain that `multi` fields route to the collection field first and the custom factory only handles per-row (single) types

## 5. Verification

- [ ] 5.1 Run `tsc --noEmit`; fix any signature/type fallout at call-sites (`HalFormsForm`, consumers)
- [ ] 5.2 Run full frontend test suite; confirm no behavior change — especially the category collection regression test and `eventFormFieldsFactory.test.tsx`
- [ ] 5.3 Browser sanity check on `http://localhost:3000`: create/edit event categories (add/remove rows, fee, preserved id) still work
