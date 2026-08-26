# Validation & Cross-Field Rules

Bean Validation constraints are declared in the spec so the generated DTO carries them.
Covers per-field constraints and `x-klabis-class-constraint` for rules spanning two fields.

# Validation lives in the spec too

Bean-validation constraints are generated from standard OpenAPI keywords, so they belong in the spec
alongside the types:

| spec | generates |
|---|---|
| `required: [firstName, …]` | `@NotNull` (not `@NotBlank` — see below) |
| `x-klabis-not-blank: true` | `@NotBlank` (Klabis extension; schema properties only) |
| `x-klabis-past: true` | `@Past` (Klabis extension; schema properties only) |
| `x-klabis-url: true` | `@URL` (Klabis extension; schema properties only) |
| `x-klabis-class-constraint: <FQN>` | that annotation on the record itself — cross-field rules |
| `maxLength` / `minLength` | `@Size(max=…, min=…)` |
| `pattern` | `@Pattern(regexp=…)` |
| `format: email` | `@Email` |
| `minimum` / `maximum` | `@Min` / `@Max` |
| `type: [x, 'null']` | `JsonNullable<X>` — PATCH tri-state, see below |

A schema that omits them produces a DTO that accepts anything — the failure shows up as a controller
test expecting `400` and getting `200`, or as missing entries under `fieldErrors`. When migrating a
module, transcribe the Jakarta annotations off the hand-written record; springdoc reports most of
them in `klabis-codefirst.json` already.

**`required` is not `@NotBlank`.** In OpenAPI `required` only means the key must be present, so it
generates `@NotNull` — which accepts `""`. Adding `minLength: 1` gets you `@Size(min = 1)`, which
rejects `""` but still accepts `"   "`. OpenAPI has no standard keyword meaning "not blank".

Klabis therefore has its own: **`x-klabis-not-blank: true`** on the property, emitted as `@NotBlank`
by the overridden `pojo.mustache`. Use it wherever the hand-written record had `@NotBlank`; the field
also keeps the redundant `@NotNull` from `required`, which is harmless.

**`x-klabis-past: true`** works the same way for `@Past`, which OpenAPI likewise cannot express
(`format: date` says nothing about the range), and **`x-klabis-url: true`** for `@URL`. All three
live in `PROPERTY_ONLY_CONSTRAINT_EXTENSIONS` in `validate.mjs`; adding a fourth constraint of this
kind means one entry there plus one branch in `pojo.mustache`.

**Do not pair `x-klabis-url` with `format: uri`.** That format makes the generator emit
`java.net.URI`, and Hibernate's `@URL` constrains `CharSequence` — the combination changes the Java
type out from under the mapper and the constraint silently never applies. Leave the property a plain
string and let the extension carry the validation.

**Check the schema is actually generated before converting a `pattern` hack to it.** Not every
schema in the spec has a generated counterpart: one that is mapped away via `schemaMappings`, or
simply absent from a module's `models` allow-list, is documented for the frontend but produces no
Java. There the extension emits nothing and the validation still lives in a hand-written annotation,
so swapping `pattern: '^(?!\s*$).+'` for `x-klabis-not-blank` is a pure regression.
`find backend/build/generated/openapi -name '<Schema>.java'` settles it.

## Cross-field rules: `x-klabis-class-constraint`

A rule spanning two properties ("these deadlines must be non-decreasing") has no OpenAPI keyword, and
the `@AssertTrue` accessor that used to express it cannot survive migration — a generated record has
no method bodies. Write a class-level Bean Validation constraint instead and name it on the schema:

```yaml
UpdateEventRequest:
  type: object
  x-klabis-class-constraint: com.klabis.events.infrastructure.restapi.DeadlinesOrdered
```

The value is a fully-qualified annotation name **without** the leading `@`; `pojo.mustache` renders it
above the record. Unlike `additionalModelTypeAnnotations` — which applies to every model in the task —
this is keyed on the individual schema.

The annotation is rendered with **no argument list**, so all its members must have defaults; a
constraint needing a mandatory attribute means extending the template block to carry arguments.

Two things the validator must handle, both learned from `DeadlinesOrderedValidator`:
- **It must be `public`.** Hibernate's default factory instantiates validators reflectively and
  rejects a package-private class with `HV000064`.
- **Re-anchor the violation on the property** via `addPropertyNode`, or the 400 response stops naming
  the offending field and just reports a class-level error.

Reading the value usually means reflection over the record component, since the same rule tends to
apply to both a POST and a PATCH request — and on the PATCH side it arrives wrapped in
`JsonNullable<T>`.

Two limits:
- **Schema properties only.** Only `pojo.mustache` has a branch for it, so the generator would drop
  the extension on a `parameters` entry; `validate.mjs` rejects it there rather than letting it pass as
  a silent no-op. For a constrained `@RequestParam` use `pattern: '^(?!\s*$).+'` instead — see the
  `validatePasswordSetupToken` `token` parameter in `common.yaml`.
- **The custom message is still lost.** `@NotBlank(message = "…")` texts do not survive; assertions
  must expect the Bean Validation default (`"must not be blank"`).

**Validation messages become the Bean Validation defaults** (`"size must be between 1 and 100"`).
OpenAPI cannot express a custom message, so hand-written `@NotBlank(message = "…")` texts are lost on
migration. Update the assertions to the default text rather than contriving a way to keep the old
one — the constraint still fires, only the wording changes.
