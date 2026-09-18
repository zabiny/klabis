## Context

`GET /{entityType}/{id}/sync` is one of five operations on `SynchronizationController`,
all gated on `SYNC:MANAGE` at the operation level. This change opens the read operation to
any authenticated caller while keeping most of its payload behind the permission; the four
state-changing operations are untouched.

The response is generated from `docs/openapi/spec/sync.yaml` into `SyncStateResponse`, and
assembled by `SyncStateResponseConverter` — deliberately not a Spring bean, constructed
directly by the controller (see its javadoc).

## Goals / Non-Goals

**Goals**

- Any authenticated caller can read the headline synchronisation state of a linked entity.
- Everything beyond that headline requires `SYNC:MANAGE`.
- Reuse the project's existing field-level authorization; introduce no new mechanism.

**Non-Goals**

- Changing who may *act* on a record. Trigger, acknowledge, resolve and reset stay on
  `SYNC:MANAGE`.
- Per-entity-type read permissions. Whether a caller may read the underlying event is
  already settled where the `sync` link is offered; this change does not add a second,
  sync-side check. (See "Rejected alternatives".)
- Changing the HAL link or its affordances.

## Decisions

### D1: Field-level authorization, not two response shapes

The visibility split is expressed as `x-klabis-authority: SYNC_MANAGE` on individual
`SyncStateResponse` properties, evaluated at serialization time by
`FieldSecurityBeanSerializerModifier` / `SecuredBeanPropertyWriter`.

This is the pattern `MemberDetailsResponse` already uses for sixteen properties, so the
generator, the serializer and the tests all already understand it.

*Rejected:* a separate `SyncStateSummaryResponse` returned to non-managing callers. It
would mean two schemas, two converters and a branch in the controller, and the endpoint
would return different media-type shapes for the same URL — awkward for a generated
client and for HAL consumers alike.

`x-klabis-owner-visible` is **not** used. It exists to let a member see their own data;
a synchronisation record has no owner, and `SecuredBeanPropertyWriter` treats the
authority check as sufficient on its own (`ownerVisible` defaults to false and the
ownership branch is skipped entirely).

### D2: What stays visible, and why

| Field | Visible to all | Reasoning |
|---|---|---|
| `entityType` | yes | Says what the state is *about*; meaningless response without it. |
| `status` | yes | The headline fact. |
| `externalSystem` | yes | Names *which* system; "IN_SYNC" alone does not say with what. |
| `lastSuccessfulSyncAt` | yes | The second headline fact — when it was last in step. |
| `externalId` | **no** | The entity's identifier in ORIS. Not needed to know the state, and it is a pointer into another system's data. |
| `lastDirection` | no | Engine bookkeeping. |
| `nextAttemptDueAt` | no | Retry schedule; operational. |
| `failedAttemptsSinceLastSuccess` | no | Operational; also the one field whose computation costs a query (see D4). |
| `acceptedDivergence` | no | Only meaningful next to the baseline it describes. |
| `divergedFields`, `changedSides` | no | Conflict diagnosis, for the manager who must resolve it. |
| `local`, `external`, `baseline`, `baselineExternal` | no | Decrypted projections and baseline pair — the actual entity data on both sides. |

The four visible fields answer "is this kept in step, with what, and when did that last
work" and nothing further.

### D3: `required` shrinks to the always-present fields

A field that can be omitted must not be `required`. Three fields lose it: `externalId`,
`failedAttemptsSinceLastSuccess` and `acceptedDivergence`. `required` becomes
`[entityType, status, externalSystem]`.

`lastSuccessfulSyncAt` was already optional (nullable before the first successful pass)
and stays so.

This is the change's only breaking element for existing clients. It is unavoidable: the
alternative — keeping them required and masking rather than omitting — would hand
non-managing callers a placeholder string where a number or boolean is declared, which
neither the schema nor a generated client would accept.

**No consumer depends on the current required status.** The three fields appear in
`frontend/src/api/klabisApi.d.ts` (generated) and nowhere else in the frontend; in the
backend only `SyncStateResponseConverter` writes them, and no test binds
`SyncStateResponse` as a type — `SynchronizationControllerTest` asserts over JSON paths.

`required` is not merely documentation here: the generator turns it into `@NotNull` on
the record component (`@NotNull String externalId`, `@NotNull Integer
failedAttemptsSinceLastSuccess`, `@NotNull Boolean acceptedDivergence`). Outgoing
responses are not validated — no `@Valid` on any return type — so dropping it has no
runtime effect today. It is still the right change: a `@NotNull` on a field that is
absent for most callers is a false statement that a later `@Valid` would turn into a bug.

Dropping `required` changes the generated component types for the two non-nullable
fields — an `Integer`/`Boolean` may become `JsonNullable<...>`, as elsewhere in this
template. `SyncStateResponseConverter`'s builder calls must be adjusted to match. This is
a compile error, not a silent break, but it is expected work rather than a surprise.

### D4: Authorization runs at serialization, so the converter still computes everything

`SecuredBeanPropertyWriter` filters during Jackson serialization, after
`SyncStateResponseConverter` has built the full object. For a non-managing caller the
converter therefore still decrypts both projections and still calls
`synchronizationPort.failedAttemptsSinceLastSuccess(...)`, which loads the record's
attempt history — work whose result is then dropped.

This change accepts that. Making the converter authority-aware would push a security
decision down into a mapping class and duplicate the rule in two places, which is exactly
what the field-level mechanism exists to avoid. The cost is one extra query per read on a
low-traffic endpoint.

It is worth recording rather than leaving implicit: if this endpoint ever becomes hot, the
fix is to make the converter skip the fields it knows will be dropped — not to move the
authorization.

## Risks / Trade-offs

**A 404 for an unlinked entity now reaches ordinary users.** Asking for the sync state of
an entity that is not enrolled returns `SyncRecordNotEnrolledException` → 404, so any
authenticated caller can now learn whether a given entity is linked to an external system.
The `sync` HAL link already discloses exactly this to the same callers, so nothing new is
revealed; noted because it is a genuine widening of who can ask.

**Field-level authorization is invisible to unit tests.** A plain `@JsonTest` or a direct
call to the converter serializes every field regardless of authority — the modifier needs
a real `SecurityContext`. Tests that assert the split must go through the MVC layer with
an authenticated principal, or they will pass while the endpoint leaks. This is the most
likely way for the change to be implemented wrongly and still look correct.

**Absent, not masked.** Denied fields are omitted (no `@HandleAuthorizationDenied` with
`MaskDeniedHandler`), matching how `MemberDetailsResponse` behaves. Frontend code that
reads `response.local` must handle `undefined`.

## Migration Plan

Single deployment; no data migration. Order:

1. Edit `sync.yaml` (operation-level authority removed from `getSyncState`; property-level
   authority added; `required` shrunk).
2. Regenerate the backend model and the frontend types — both derive from the same spec,
   so they must move together.
3. Adjust `SynchronizationController` if the generated interface's authority annotation
   changes shape for this one method.
4. Update the frontend sync view to tolerate the omitted fields.

## Rejected alternatives

**Gating the read on the target entity's own read permission** (e.g. `EVENTS_READ` for an
event). Conceptually the cleanest answer — the synchronisation state is a property of the
entity, so it should follow the entity's visibility. Rejected because `sync` is
deliberately unaware of the modules whose entities it synchronises (D14 of the sync engine
design): the mapping from `SyncEntityType` to another module's authority does not exist,
and adding it would put a list of every synchronisable module inside the generic engine.
The natural home for it, if this is ever needed, is on `SynchronizationAdapter` — the
adapter already knows its entity type and its module — but that is a larger change than
this one warrants.

**Leaving the endpoint on `SYNC:MANAGE` and merely trimming the payload.** That would
narrow the response without giving anyone new access, so the trimming would be pointless:
every caller who can reach the endpoint holds the permission that unhides everything.
