# Adding or Migrating a Module

End-to-end walkthrough: registering the module for codegen, wiring the controller to the
generated interface, and the test gotchas that show up on the first build.

# Registering a module for codegen

Writing the YAML generates nothing on its own. Each module gets its own codegen task, registered with
`openApiModule(...)` in `backend/build.gradle.kts` (the helper is
`backend/buildSrc/src/main/kotlin/OpenApiModule.kt`):

```kotlin
openApiModule(
    module = "members",                                   // -> build/generated/openapi/members
    pkg = "com.klabis.members.infrastructure.restapi",     // same package as the controller
    specFile = "members.yaml",                             // under docs/openapi/spec/
    mappings = emptyMap()
)
```

Just four required parameters — `module`, `pkg`, `specFile`, `mappings` (plus an optional
`extraImportMappings`). **There is no `apis` / `models` enumeration.** The task points straight at
`docs/openapi/spec/<specFile>`, and that one file's entire `paths` + `components.schemas` content
*is* the module's generation scope. `globalProperties` sets `models=""` / `apis=""` inside the
helper (present-but-empty means "generate all" — an *omitted* key would skip generation entirely).

A module with no hand-written overrides passes `mappings = emptyMap()` — `members`, `finance`,
`events`, `calendar`, `membershipfees`, `groups`, `oris` all do. Only `common` has entries, for its
two marker records:

```kotlin
mappings = mapOf(
    "EntityModelRootModel" to "com.klabis.common.ui.RootModel",
    "EntityModelDashboardModel" to "com.klabis.common.ui.DashboardModel"
)
```

`schemaMappings` also always carries `ProblemDetail -> org.springframework.http.ProblemDetail` and
`importMappings` carries `Instant -> java.time.Instant`; the helper adds both, do not restate them.

One task **per module**, not one shared task: `modelPackage`/`apiPackage` are scalars and
`schemaMappings` is global per task, so a single task could never let two modules each define their
own `AddressRequest`.

`pkg` must be the package the hand-written controller already lives in — cross-module link processors
reach these types through Modulith named interfaces.

**`useTags` is on**, so the `*Api` interface name comes from the operation's `tags:` value. A tag
containing a space (`Calendar Feed Token`, `Event Registrations`, `My Profile`) is silently dropped:
the build succeeds, no warning is printed, and the interface simply never appears. Watch for a
trailing space too. Give every operation a single-word tag (`IcalToken`, not `Calendar Feed Token`).
`klabis-full.json` takes its tags from `@Tag` on the controller, so a spec-side tag rename changes
neither the wire nor the bundle.

**The generator never deletes.** It only writes, so a schema you rename or drop leaves its old record
behind in `build/generated/openapi/<module>/`— and since that directory is on `sourceSets.main`, the
ghost keeps compiling. Local builds stay green while a clean CI build fails. `openApiModule` handles
this with `doFirst { delete(outputDir) }`; keep it when touching that function.

**The bundle is still a build dependency**, though backend codegen no longer reads it: the task
`dependsOn(openapiBundle)` for its `validateSpec` / `validateModuleDocuments` side effect, so a
module file that has drifted from `klabis.yaml` fails the build here rather than silently.

# Adding a module

Every existing module is already spec-first; this is the recipe for a genuinely new one, and the
reference for how the existing ones are put together.

1. `./gradlew generateOpenApiDocs` then read `docs/openapi/generated/klabis-codefirst.json` — only
   useful if the endpoints already exist in Java; for a new module, skip to step 3
2. That dump is the ground truth for parameters, request bodies and status codes
3. Write `<module>.yaml`; add one `$ref` per path to `klabis.yaml`. Name payload schemas after the
   existing Java DTO classes — see "A payload schema's name is wire contract"
4. Add `x-hal-links` / `x-hal-templates` by reading the controller's postprocessors and
   `RepresentationModelProcessor` implementations — springdoc cannot see them, so the drift check
   will not catch a missing one
5. Transcribe each `@HasAuthority` off the controller into `x-klabis-authority` on the matching
   operation, then delete it from the controller. Compare the generated `*Api` interface against the
   controller as it was — an authority that silently changes or disappears here is not something the
   tests will necessarily catch
   Authorization is not always an annotation. A controller may enforce it **imperatively** — a
   private `checkXxxAccess()` throwing `AccessDeniedException`, typically "owner OR MANAGE
   authority". That is the `x-klabis-authority` + `x-klabis-owner-visible` pair; move it into the
   spec and delete the helper. A helper that permits *only* the caller themselves, with no authority
   alternative, is `x-klabis-owner-visible` on its own — declaring it alone does not widen access
   (see that extension's section). Read each method body before concluding an endpoint is
   unprotected, because an imperative check is invisible both to reflection and to the drift check.
6. Register the module with `openApiModule(...)` (above), then `./gradlew compileJava`
7. Rework the controller: implement the generated `*Api`, return plain payloads, and register the
   domain objects with `HalResponseContext` (below).
   **Strip the path from the class-level `@RequestMapping`.** Generated interface methods carry the
   full absolute path, so a controller that still declares `@RequestMapping(value = "/api/foo")`
   makes Spring concatenate the two into `/api/foo/api/foo` and every endpoint 404s. Keep the
   annotation for `produces` only: `@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)`,
   as `MemberAccountController` and `EventTypeController` do.
8. Re-run the drift check until the module reports `mismatched: 0`
9. `cd frontend && npm run openapi`, then `npx tsc --noEmit -p tsconfig.app.json`

## Returning plain payloads

A generated interface method returns the payload, not a `RepresentationModel`. The controller stores
the domain object(s) in `HalResponseContext`; `HalResponseBodyAdvice` picks them back up, builds the
`EntityModel`/`PagedModel`/`CollectionModel`, and runs the existing postprocessors:

```java
HalResponseContext.setDomain(eventType);          // single
HalResponseContext.setDomainList(eventTypes);     // collection or page — paired 1:1 by index
```

Links and affordances that belonged to the **collection itself** cannot be built in the method any
more (there is no model to add them to). They move into a
`RepresentationModelProcessor<CollectionModel<EntityModel<T>>>`; the advice contributes the self
link, the processor contributes affordances via `klabisAfford*` so they stay authorization-sensitive.

Controllers that still build their own models are untouched — without an entry in
`HalResponseContext` the advice passes the body through unchanged.

## Request bodies bound to domain types

Some controllers deserialize straight into a domain command
(`@RequestBody EventType.CreateEventType`). That violates "DTOs carry wire types" and cannot survive
migration: generate a `CreateFooRequest` DTO from the spec and add a mapper to the domain command.
Keep the domain record — the domain and its service still use it, it just stops being the
deserialization target.

**The drift check compares schemas by name, not by content.** Two schemas called
`RegisterMemberRequest` match even when their properties differ wildly. After the check goes green,
diff the module's schemas property-by-property against `klabis-codefirst.json` — that is where
transcription mistakes actually surface.

Expect the springdoc output to be wrong in places — it does not know about `@JsonValue` mixins, and
it introspects Java types rather than the wire, so a wrapper like `JsonNullable<T>` can surface as an
object with the wrapper's own fields rather than as the value it serializes to. Where it disagrees
with the actual wire format, the spec follows the **wire**, and the discrepancy is documented in the
spec rather than mirrored.

## A newly annotated method can fail a link/affordance unit test

Moving authorization into the spec makes it **discoverable by reflection** for the first time. That
can break a passing unit test without any behaviour changing, and the failure looks alarming — a HAL
link silently disappears.

The cause is in `HalFormsSupport`: every `klabisLinkTo` / `klabisAfford*` guard reads
`INSTANCE != null && !INSTANCE.isMethodAuthorized(...)`. `INSTANCE` is a static set by
`@PostConstruct`, so in a plain unit test with no Spring context it is null and **authorization is
skipped entirely**. Such a test passes without ever exercising the check. Once the target method
carries `@HasAuthority` / `@OwnerVisible`, a leftover `INSTANCE` from another test class's context in
the same fork activates the real check — and `isMethodAuthorized` returns `false` unless an
`OwnershipResolver` is actually available, since the ownership branch falls through to `return false`
when `ownershipResolverProvider.getIfAvailable()` is null.

Symptom: the test passes standalone and fails when run after any `@SpringBootTest` in the same fork.

Fix the test, not the assertion — and verify the production behaviour separately (the module's
MockMvc controller test with `@WithKlabisMockUser` is the real evidence, since it exercises genuine
authentication). Either wire a real `OwnershipResolver` into a `HalFormsSupport` and set `INSTANCE`
for the test's duration (`AccountRootLinkProcessorTest` and `AccountMemberDetailLinkProcessorTest`
do this, and must restore the previous value afterwards or they leak the same problem onward), or use
the `@WebMvcTest` + `@Import(HalFormsSupport.class)` + `@WithKlabisMockUser` slice that
`AffordanceAuthorizationTest` uses. Give the resolver the real UUID-comparison semantics; one that
returns `true` unconditionally makes the test assert nothing.
