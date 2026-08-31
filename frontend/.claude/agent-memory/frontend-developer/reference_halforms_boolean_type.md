---
name: halforms-boolean-type
description: Klabis backend serialises boolean HAL-FORMS template properties with type "Boolean" (capitalised), not "boolean" or "checkbox"
metadata:
  type: reference
---

When a Java `Boolean` field appears in a HAL-FORMS `_templates[].properties[]`, the emitted
`type` is the string `"Boolean"` (the class simple name).

**Why:** Spring HATEOAS `HtmlInputType.from(Boolean.class)` returns `null` (it only maps
LocalDate/LocalDateTime/numerics/URI/URL/String/LocalTime). Klabis'
`KlabisHalFormsPropertyMetadataWrapper.getInputType()` (backend `common/ui/HalFormsSupport.java`)
then falls back to `type.getSimpleName()` → `"Boolean"`. `JsonNullable<Boolean>` unwraps the
same way.

**How to apply:** The generic field renderer `halFormsFieldsFactory`
(`src/components/HalNavigator2/halforms/HalFormsFieldFactory.tsx`) now routes
`case 'checkbox' | 'Boolean' | 'boolean'` → `HalFormsCheckbox`. `'Boolean'` is also in
`SIMPLE_FIELD_TYPES` (types.ts) so read-only rendering works. `HalFormsBoolean` (a Switch)
is NOT wired into the factory — `type: "boolean"` lowercase is essentially never emitted.
`HalFormsCheckbox` label falls back to `getFieldLabel(prop.name)` because the backend sends no
`prompt` (no message bundle) — add the Czech label to `labels.fields` keyed by the property name.
