# Implementation Tasks

Organized as **vertical end-to-end slices**: each slice delivers one complete synchronisation behaviour through domain → persistence → port → tests, and is independently committable.

Slices 1–5 build and prove the engine against a **test adapter and test projection** — no ORIS, no change to any existing behaviour. Slice 6 exposes it over REST. Slices 7–8 introduce the ORIS event adapter and move the existing event synchronisation behind the engine; **the user-visible behaviour change lands in slice 8**. Slice 9 records the architecture and documentation.

Each slice follows TDD (Red-Green-Refactor) and runs tests via the `test-runner` agent before commit. Never run Gradle or `test-runner` in parallel — one invocation at a time.

References in brackets point at the decision in `design.md` that a task implements.

## 1. Slice: Inward synchronisation end-to-end (walking skeleton)

Delivers: enrol an entity, first pass adopts the external side, a later external change flows inward, an unchanged pair does nothing. Establishes every shared building block the later slices plug into.

- [x] 1.1 Module skeleton: `com.klabis.sync` with `application`, `domain`, `infrastructure` packages and `@NamedInterface("application")` in `application/package-info.java` [D1]
- [x] 1.2 Domain tests (red): `SyncProjection` canonicalisation and hashing — equal projections hash equally; field order, null representation and `BigDecimal` scale (`100` vs `100.00`) do not change the hash; different values do [D3]
- [x] 1.3 Domain: `SyncProjection` interface plus `SyncHash`, and the canonical JSON serialisation used for both storage and hashing [D3]
- [x] 1.4 Domain: `SyncSnapshot` (projection + its own hash, neither half replaceable independently) and `SyncBaseline` (the local/external snapshot pair) [D4]
- [x] 1.5 Domain: `SyncTarget`, `ExternalReference`, `ExternalVersionToken`, `SyncEntityType` (with its declared REST path segment), `ExternalSystem`, `SyncStatus`, `SyncDirection`, `SyncTriggerKind`, `SyncOutcome`
- [x] 1.6 Domain tests (red): direction resolution table — neither side changed → nothing to do; only external changed → inward; enrolled record with no baseline → adopt external [D4, D5]
- [x] 1.7 Domain: `SyncRecord` aggregate with enrolment, claim, `recordSuccess`, and the direction resolution for the cases above
- [x] 1.8 Domain: `SyncAttempt` as a separate append-only aggregate with its repository interface [D15]
- [x] 1.9 Secondary port: `SynchronizationAdapter` with `SyncCapabilities` (reads/writes local and external, creates, `containsSensitiveData`) and the optional external version token [D3]
- [x] 1.10 Persistence: `sync_record` and `sync_attempt` tables in `V001` — projection columns `TEXT` and typed as `EncryptedString`, hash columns plaintext, `baseline_external_*` nullable, unique constraints, `@Version`, index on `(sync_record_id, started_at DESC)` [D13, D19]
- [x] 1.11 Persistence: mementos and repository adapters; verify encryption is transparent through the globally registered converters
- [x] 1.12 Persistence tests: round-trip a record with all three snapshots; the projection column is unreadable as plaintext in the database; ciphertext differs between two saves of an identical projection while the hash column does not [D13]
- [x] 1.13 Application: `SynchronizationPort` with `enroll`, `synchronizeNow`, `state`, `retire`; the pass orchestration performing the version-token short-circuit, the reads, the comparison and the attempt append [D3, D9]
- [x] 1.14 Test fixtures: a test adapter and test projection (in the sync module's test sources) with configurable capabilities and controllable both-side state — the harness every later slice extends
- [x] 1.15 Integration tests: enrol → first pass adopts external; external change → inward write, baseline from the post-write re-read; unchanged pair → nothing written, attempt recorded; unknown entity type → enrolment rejected
- [x] 1.16 Run tests (`test-runner`), code review, commit

## 2. Slice: Outward synchronisation, convergence and the re-read rules

Delivers: the remaining non-conflict outcomes, and the correctness rules that keep a concurrent local edit from being lost.

- [x] 2.1 Domain tests (red): only local changed and outward write available → outward; both sides changed to the *same* values → converged, rebase both baselines, write nothing [D4]
- [x] 2.2 Domain: extend direction resolution with the outward and convergence rows
- [x] 2.3 Application: outward pass — write through the adapter, then re-read the local projection; if it no longer matches what was pushed, skip the baseline write and leave the record due [D9]
- [x] 2.4 Application: inward pass — re-read the local projection immediately before the write and abort if it moved; re-read again after the write and use that state as both local snapshot and baseline [D9]
- [x] 2.5 Domain: `dirtySince` as a scheduling marker only — set on an observed local change, never consulted to decide whether a write is safe [D9]
- [x] 2.6 Integration tests: local edit committed between the decision read and the inward write → attempt aborts, nothing overwritten, record stays due; local edit committed during an outward pass → baseline not written, next pass pushes again
- [x] 2.7 Integration test: an inward write marks its own record dirty, and the following pass lands on "nothing to do" or convergence and clears it — no conflict is raised [D9]
- [x] 2.8 Run tests, code review, commit

## 3. Slice: Conflicts — detection, attribution and resolution

Delivers: the losslessness guarantee end-to-end, including the accepted divergence and its protection against later external changes.

- [ ] 3.1 Domain tests (red): both sides changed to different values → conflict; local changed with no outward capability → conflict; external changed while the baseline pair is diverged → conflict [D4, D6]
- [ ] 3.2 Domain: conflict detection including the inward guard that protects a standing accepted divergence [D4]
- [ ] 3.3 Domain tests (red): per-field attribution from the three snapshots — a field only Klabis moved, a field only the external side moved, a field both moved [D14 response shape]
- [ ] 3.4 Domain: `ChangedSide` and the diverged-fields computation, derived by comparing decrypted projections in memory — never from stored per-field hashes [D13]
- [ ] 3.5 Domain: `ConflictAcknowledgement` bound to the hash pair current when it was made, plus the acting user [D7, D15]
- [ ] 3.6 Application tests (red): resolution re-reads both sides first and proceeds only when the fresh hash pair equals the acknowledged one; a side that moved in between causes refusal, refreshes the record's snapshots and leaves the conflict standing [D7]
- [ ] 3.7 Application: `acknowledgeConflict` and `resolveConflict` with `SyncResolution` — `INWARD`, `OUTWARD`, `ACCEPT_DIVERGENCE`; a direction the adapter cannot perform is refused [D6, D7]
- [ ] 3.8 Application: `ACCEPT_DIVERGENCE` sets the baseline pair to the freshly read snapshots, writes nothing, clears the conflict [D6]
- [ ] 3.9 Domain event: `SyncConflictDetected` carrying identifiers, direction and hashes only — no projections [D15]
- [ ] 3.10 Integration tests: a conflict clears itself when a side reverts, and when both sides come to agree (the "fixed in the external system" path); after an accepted divergence a later external change raises a new conflict instead of overwriting; a conflicted record is written to by no pass
- [ ] 3.11 Integration test: resolving without acknowledging is refused; acknowledging a conflict that no longer exists is refused
- [ ] 3.12 Run tests, code review, commit

## 4. Slice: Failure handling — retry, terminal state, outage

Delivers: a failing record that is retried with growing delay, becomes visibly stopped, and can be restarted; an outage that strands nobody.

- [ ] 4.1 Domain tests (red): failure classification — outage-shaped (breaker open, connection failure, timeout) counts for nothing, other transport and server errors are retryable, everything else is terminal immediately [D10, D11]
- [ ] 4.2 Domain: failure classification and the `RETRYING` status, entered from `NEW` and `IN_SYNC` alike; a retrying record is still compared and synchronised on every due pass [D10]
- [ ] 4.3 Application tests (red): "attempts since the most recent success or reset" derived from the history, ignoring `OUTAGE` rows [D10, D11]
- [ ] 4.4 Application: derived failure count, `nextAttemptDueAt` computed from it (initial delay, multiplier, ceiling), and the `RESET` attempt row that restarts the count [D10, D19]
- [ ] 4.5 Application: terminal `FAILED` on reaching `max-attempts`, `reset` returning the record to service, and `SyncTerminallyFailed` [D10, D15]
- [ ] 4.6 Infrastructure: Resilience4j retry instance for in-attempt transport blips, and the circuit breaker around the external client, both configured under the existing `resilience4j` block [D10, D11]
- [ ] 4.7 Application: an open breaker ends the pass leaving the remaining records untouched; outage failures reschedule at the initial delay [D11]
- [ ] 4.8 Application: per-record claim with lease expiry; a second pass skips a freshly claimed record and proceeds with others [D12]
- [ ] 4.9 Integration tests: a record fails repeatedly → `RETRYING` with a growing due date → `FAILED` at the limit → reset → synchronises again; a multi-day outage terminates nobody; an expired claim is picked up by the next pass
- [ ] 4.10 Run tests, code review, commit

## 5. Slice: Scheduling, retention and configuration

Delivers: the engine running on its own, and history that does not grow without bound.

- [ ] 5.1 Configuration properties record with the documented defaults — `max-attempts`, `claim-lease`, `history-retention`, `retry-delay.initial/multiplier/max`, `scan-cron`, `due-scan-interval` [D19]
- [ ] 5.2 Scheduler: nightly full pass re-comparing every active record [D10, D19]
- [ ] 5.3 Scheduler: due scan on `due-scan-interval` running one indexed query for dirty or retry-due records, costing one query when nothing is due [D10]
- [ ] 5.4 Scheduler: history retention cleanup removing attempt rows older than the retention period and never removing a record, in the style of `TokenCleanupJob` [D19]
- [ ] 5.5 Tests: the due scan picks up exactly the dirty and due records and skips retired, conflicted, terminally failed and claimed ones; the full pass re-compares everything active; retention removes only expired attempt rows and leaves last-success information intact
- [ ] 5.6 Tests: defaults apply when nothing is configured, and overridden values take effect
- [ ] 5.7 Run tests, code review, commit

## 6. Slice: REST resources and the SYNC:MANAGE authority

Delivers: the synchronisation state and the resolution workflow over the API, for any entity type.

- [ ] 6.1 `Authority.SYNC_MANAGE` as a global authority [D14]
- [ ] 6.2 OpenAPI: new `docs/openapi/spec/sync.yaml` describing `getSyncState`, `synchronizeNow`, `acknowledgeSyncConflict`, `resolveSyncConflict`, `resetSyncRecord` under `/api/{entityType}/{id}/sync…`, with `entityType` enumerated rather than free-form; add the `sync` springdoc group in `application.yml` [D14]
- [ ] 6.3 Controller implementing the generated API: one controller for every entity type, with a converter binding `{entityType}` that rejects unknown segments before any handler runs [D14]
- [ ] 6.4 Response model: status, external system and identifier, last successful synchronisation and direction, next attempt due, failed attempts since last success, accepted-divergence marker, the projections, and — in conflict — diverged fields with their changed side [D14]
- [ ] 6.5 HAL affordances rendered per state: `synchronizeNow` for `NEW`/`IN_SYNC`/`RETRYING`, `acknowledgeSyncConflict` and `resolveSyncConflict` for `CONFLICT`, `resetSyncRecord` for `FAILED` [D14]
- [ ] 6.6 Controller tests: each operation's success and refusal paths; affordances appear only in the states that allow them; every operation requires `SYNC:MANAGE`; an unenrolled entity and an unknown entity type are not found
- [ ] 6.7 Run tests, code review, commit

## 7. Slice: ORIS event adapter

Delivers: the first real adapter, still without changing how events behave.

- [ ] 7.1 `OrisEventProjection` in `com.klabis.oris.sync` — the ORIS-owned event fields (name, date, location, organiser, website, registration deadlines, ranking, base entry fee, ORIS-origin categories) as a plain data carrier; Klabis-owned fields deliberately absent [D3]
- [ ] 7.2 Projection tests: mapping an `Event` and an ORIS `EventDetails` to the same shape; two sides holding equal data hash equally, including monetary amounts of differing scale [D3]
- [ ] 7.3 `OrisEventSyncAdapter` declaring inward-only capabilities (no outward write, no create, no sensitive data), reusing the mapping in `OrisEventImportService` [D2, D3]
- [ ] 7.4 Adapter: external version token from `getEventListVersions`, and the fallback when it is unavailable [D3]
- [ ] 7.5 Adapter: `applyToLocal` invoking `Event.syncFromOris`, preserving its existing category merge and event-type behaviour [D2]
- [ ] 7.6 Adapter tests against a stubbed ORIS client: inward write, unchanged token short-circuit, a local edit to an ORIS-owned field producing a conflict rather than an overwrite
- [ ] 7.7 Run tests, code review, commit

## 8. Slice: Events move onto the engine (observable behaviour change)

Delivers: the change managers actually see — automatic synchronisation, and edits that are no longer silently overwritten.

- [ ] 8.1 `events`: enrol an event with the engine on ORIS import; retire the record when the event becomes finished or cancelled [D17]
- [ ] 8.2 `events`: mark the record dirty on `EventUpdatedEvent` [D9]
- [ ] 8.3 `syncEventFromOris` delegates to the engine, keeping its path, operation identifier and affordance; refuses with a problem detail pointing at the sync resource when the record is in conflict or terminally failed [D18]
- [ ] 8.4 `sync-from-oris/all-upcoming` becomes a manual pass over all active records — ignoring due-ness and the dirty flag, honouring claims and the breaker — reporting records awaiting a decision and records stopped by failures separately from failures [D18]
- [ ] 8.5 Remove the per-event loop from `OrisBulkSyncService`, keeping its result summary shape extended with the two new categories
- [ ] 8.6 `events`: `sync` link on the event resource when enrolled, obtained through `SynchronizationPort` [D18]
- [ ] 8.7 Integration tests: importing an event enrols it; a manager's edit to the name raises a conflict on the next synchronisation and the name survives; resolving inward restores the ORIS values; accepting the divergence keeps the edit and re-asks on the next ORIS change; editing a fee override or adding a category raises no conflict
- [ ] 8.8 Integration tests: bulk synchronisation skips and reports conflicted and terminally failed events; a finished event is retired and no longer synchronised
- [ ] 8.9 Regression: existing ORIS import and synchronisation tests still pass unchanged where behaviour is unchanged; update those that assert silent overwriting of ORIS-owned fields
- [ ] 8.10 Run the full backend suite (`test-runner`), code review, commit

## 9. Slice: Architecture record and documentation

- [ ] 9.1 `docs/design-decisions.md`: new ADR for the synchronisation engine — the generic module with integration-owned adapters, the identity mapping held only by the record, and the uniformly addressed REST resources
- [ ] 9.2 `backend/CLAUDE.md`: add the `sync` module to the module overview and note the new `SYNC:MANAGE` authority and the synchronisation configuration block
- [ ] 9.3 `backend-patterns` skill: document the adapter contract and the projection convention, so a future integration follows the same shape
- [ ] 9.4 Verify the module structure test passes with the new module and its named interface, and that no module reaches past a primary port
- [ ] 9.5 Run tests, code review, commit
