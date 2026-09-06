## Why

`GET /{entityType}/{id}/sync` today returns everything the engine knows about a linked
entity — both decrypted projections, both halves of the baseline, the per-field conflict
diagnosis, the retry schedule and the failure count — and the whole endpoint sits behind
`SYNC:MANAGE`. Two problems follow from that pairing.

The response is far more than a reader needs. Someone who only wants to know whether an
event is in step with ORIS receives the entity's full external representation and the
engine's internal bookkeeping alongside it. Projections are decrypted domain data; the
baseline pair and the diagnostics exist to support a manager making a resolution
decision, not to be read casually.

At the same time the endpoint is closed to everyone else. A club member looking at an
ORIS-imported event cannot see the one fact that actually concerns them — that it is
kept in step, and when it was last synchronised — because the only way to learn it is an
endpoint gated on an administrative permission.

## What Changes

- Open `GET /{entityType}/{id}/sync` to any authenticated caller. The four state-changing
  sync operations (trigger, acknowledge, resolve, reset) stay gated on `SYNC:MANAGE`
  exactly as they are.
- Split the response into a public part and a managed part. Without `SYNC:MANAGE` a caller
  sees `entityType`, `status`, `externalSystem` and `lastSuccessfulSyncAt` — enough to say
  *what* is kept in step with *which* system and *how it stands*, without naming the
  entity's identifier over there. Every other field — `externalId`, `lastDirection`,
  `nextAttemptDueAt`, `failedAttemptsSinceLastSuccess`, `acceptedDivergence`,
  `divergedFields`, `changedSides`, `local`, `external`, `baseline`, `baselineExternal` —
  is omitted unless the caller holds `SYNC:MANAGE`.
- **BREAKING** for the required-field contract: `externalId`,
  `failedAttemptsSinceLastSuccess` and `acceptedDivergence` are `required` in the current
  schema but become absent for non-managing callers, so all three stop being required. A
  client that assumes their presence must tolerate their absence. The `sync` HAL link and
  its affordances are unaffected.

Field visibility uses the project's existing field-level mechanism
(`x-klabis-authority` on the schema property, evaluated by
`FieldSecurityBeanSerializerModifier`), so no new machinery is introduced — the same
pattern `MemberDetailsResponse` already uses. `x-klabis-owner-visible` does not apply:
a synchronisation record has no owner.

## Capabilities

**New Capabilities:** none.

**Modified Capabilities:**

- `data-synchronization` — the requirement "Synchronisation State Is Visible Per Entity"
  currently scopes all visibility to "a user with the synchronisation permission". It
  gains a second, narrower audience: any authenticated user may read the headline state,
  while the detail stays with the permission.

## Impact

- **Spec:** `docs/openapi/spec/sync.yaml` — remove `x-klabis-authority: SYNC_MANAGE` from
  the `getSyncState` operation only; add it to eleven `SyncStateResponse` properties;
  shrink `required` to `entityType`, `status` and `externalSystem`.
- **Code:** `SyncStateResponse` is generated from the spec, so the field annotations follow
  from regeneration. `SynchronizationController#getSyncState` loses its operation-level
  authority check; the other four methods keep theirs.
- **Reading an unlinked entity:** the 404 for a non-enrolled entity now reaches
  unauthenticated-but-logged-in callers too. This leaks only whether an entity is linked
  to an external system at all, which the `sync` HAL link already reveals to the same
  callers.
- **Tests:** the `@WebMvcTest` slice for `SynchronizationController` needs a case per
  audience — with and without `SYNC:MANAGE` — asserting exactly which fields serialize.
  Field-level authorization is invisible to a plain `@JsonTest`, so the assertion must run
  through the MVC layer with a real `SecurityContext`.
- **Frontend:** any consumer of the sync detail must tolerate the omitted fields. The
  managing view is unchanged for callers who hold the permission.
