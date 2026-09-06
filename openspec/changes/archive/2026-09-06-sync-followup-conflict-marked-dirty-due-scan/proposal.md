## Why

Code review of `sync-followup-due-scan-status-filter` surfaced a symmetric instance of
the same defect that change fixes for `FAILED`.

`SyncRecord#markDirty` (`SyncRecord.java:107`) sets `dirtySince` with **no status
guard**. `recordConflict` (`SyncRecord.java:274-275`) clears both `dirtySince` and
`nextAttemptDueAt`, so a `CONFLICT` record self-excludes from `findDueForScan` — *until*
an ordinary local edit publishes `EventUpdatedEvent`, `EventsSyncListener` calls
`markDirty` unconditionally, and `dirtySince` is set again.

Once that happens the `CONFLICT` record matches the due-scan predicate
(`dirty_since IS NOT NULL`), `SyncScheduler#runDueScan` hands it to a pass, and
`SyncRecord.assertBeingAttempted` (`SyncRecord.java:405-407`) rejects it — the identical
caught-and-logged-at-ERROR symptom that `sync-followup-due-scan-status-filter` removes
for `FAILED`.

After that sibling change lands, `findDueForScan` reads
`AND status <> 'FAILED'`, and its javadoc now flatly claims `CONFLICT` self-excludes.
That claim is only true until the record is edited again — the loophole this change
closes.

## What Changes

- Broaden the `findDueForScan` predicate from `status <> 'FAILED'` to
  `status NOT IN ('FAILED', 'CONFLICT')` in `SyncRecordJdbcRepository`.
- Correct the `findDueForScan` javadoc: `CONFLICT` is excluded by the predicate, not by
  construction, because `markDirty` can re-set `dirtySince` on a standing conflict.
- Leave `markDirty` and `SyncRecord#markDirty` unchanged, for the same reason as the
  sibling change: a conflicted-then-edited record genuinely is dirty and must
  re-evaluate once a manager resolves the conflict.
- Add a `SyncRecordJdbcRepositoryTest` case: a `CONFLICT` record driven through the
  domain, then `markDirty`, must be absent from `findDueForScan`, and that test must
  fail if the `CONFLICT` half of the predicate is removed.

## No Behavior Change Justification

Same reasoning as `sync-followup-due-scan-status-filter`: no record changes status, no
attempt is recorded that was not recorded before, no record becomes eligible that was
not eligible before. The only change is *where* a `CONFLICT` record is rejected — in the
SQL predicate instead of in a domain assertion one call later, whose exception is caught
and swallowed by the scheduler's per-record handler. `data-synchronization` spec's
conflict requirements are unaffected: a standing conflict still waits for a manager.

## Impact

- **Modules:** `sync` only, persistence layer.
- **Code:** one SQL predicate and one javadoc block in `SyncRecordJdbcRepository`.
- **Tests:** one new `SyncRecordJdbcRepositoryTest` case.
- **Depends on:** `sync-followup-due-scan-status-filter` (this widens the predicate that
  change introduces).
- **Risk:** low — additive to a `WHERE` clause, removes candidates the next layer
  rejects outright.
