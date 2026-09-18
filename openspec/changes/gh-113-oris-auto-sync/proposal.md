## Why

GitHub issue #113 ("Automaticky vytvaret a synchronizovat (a tagovat pro ktere skupiny, termin prihlasek -1D,..) akce z ORIS", milestone `MVP`, labels `ORIS`, `přihlašovatel`, `question`) asks for automatic mass import of ORIS events. Today a manager must open the "Import z ORISu" dialog and pick events by hand; a newly published ORIS event stays invisible to the club until somebody remembers to look.

Everything that happens *after* an event is imported is already solved. The bidirectional synchronisation engine (`data-synchronization` capability) keeps every imported event in step with ORIS, detects upstream changes, escalates a manager's local edit as a conflict instead of overwriting it, and retries failures with backoff. The gap is narrower than when this issue was filed: **nothing discovers newly published ORIS events and enrols them on its own**.

This change closes exactly that gap — scheduled discovery and automatic import. The other two features bundled in issue #113 are split into their own changes, since neither depends on the import schedule:

- `gh-113-event-training-group-tagging` — tagging events with the training groups they are relevant to
- `gh-113-oris-internal-deadline-offset` — the club's internal registration deadline ahead of the ORIS one

## What Changes

- **Persisted ORIS import settings.** A single club-level configuration for which ORIS events to pick up, replacing today's values hardcoded in `OrisController` (region `JIHOMORAVSKA`, window "today .. today + 1 year"). Editable by a manager on a settings page. The exact set of fields is an open question below.
- **Scheduled discovery.** A recurring job asks ORIS for events matching the configured rules, drops the ones already present, and imports the rest. Can be switched off.
- **Automatic import and enrolment.** Each discovered event is created through the existing single-event import path, so it is enrolled into the synchronisation engine on creation exactly like a manually imported one. From that moment the engine owns it — no separate refresh mechanism is introduced.
- **Manual trigger.** A manager can run the discovery immediately instead of waiting for the schedule.
- **Run visibility.** The outcome of the last run (when, how many found / imported / skipped / failed) is visible to a manager, so a silently failing job is noticeable.
- **Import settings page** in the administration area, plus a marker in the events list distinguishing automatically imported events.

Explicitly **not** in this change: group tagging, the internal deadline offset, notifications about newly discovered events (issue #91 — the project has no notification engine yet), and any change to conflict handling or retry behaviour.

## Capabilities

### New Capabilities

- `oris-import`: scheduled discovery and automatic import of ORIS events, and the club-level settings that drive it.

Rationale for a separate capability rather than extending `events`: this is about *acquiring* events from an upstream system on a schedule, which has its own settings, its own permissions surface and its own failure modes. The `events` capability describes what an event is and what a manager can do with one; `data-synchronization` describes how an already-linked entity is kept in step. Neither is a natural home for an import schedule. The existing manual-import requirements stay where they are.

### Modified Capabilities

- `events`: the existing ORIS import requirements (`Multi-Event ORIS Import`, `ORIS Import Includes Registration Deadlines`, `ORIS Import Tolerates Missing Location`, `ORIS Import Auto-Maps Event Type by Name`) gain the notion that an event may arrive without a manager selecting it, and that such an event is marked as automatically imported. The import rules themselves (which fields are read, how the event type is mapped) are unchanged.

`data-synchronization` is **not** modified: an automatically imported event is enrolled through the same port and behaves identically to a manually imported one from the engine's point of view.

## Impact

**Affected code (backend):**

- `com.klabis.oris` — a new settings aggregate and its persistence; a new discovery service holding the logic currently inlined in `OrisController.listOrisEvents` (build the ORIS filter, merge regions, drop already-imported ids, order by date), so that both the interactive dialog and the scheduled job share one implementation; a new scheduled job following the pattern established by `SyncScheduler` (cron from configuration, per-item try/catch so one failure cannot abort the run).
- `com.klabis.events.application.OrisEventImportService` — unchanged in behaviour. The job MUST import through `importEventFromOris`, which already creates the event and calls `synchronizationPort.enroll` in one transaction. Bypassing it would create events invisible to the synchronisation engine; this is the main implementation risk of this change.
- `Event` — a flag distinguishing an automatically imported event from a manually imported one.

**APIs (REST):** additive — read/update of the import settings, a "run discovery now" action, and the last-run summary. All under `EVENTS:MANAGE`.

**Data:** a settings table (single row) and a column on `events` marking automatic import. No migration of existing data: events imported before this change stay marked as manual.

**Dependencies:** none new. `oris-client` already provides event listing; the Spring scheduling infrastructure is already in use by `SyncScheduler` and `SyncHistoryRetentionJob`.

**Notable constraint:** `OrisEventListFilter` — the only filter the ORIS API accepts — carries exactly four fields: `region`, `dateFrom`, `dateTo`, `officialOnly`. Any rule beyond these (competition level / ranking, organizer, event type) cannot be pushed to ORIS and would have to be applied client-side after fetching the list. This bounds the settings design and is the reason the field set is an open question rather than a decided list.

## Open Questions

1. **Which fields does the settings page expose?**

   The ORIS API can filter only by region, date range and "official events only". Everything else must be filtered on our side after fetching.

   To decide: which of these belong in the MVP?
   - region — multiple values, or one? (the dialog already accepts a list)
   - how far ahead to look — a number of months rather than today's fixed one year
   - official events only — yes/no
   - client-side filters: competition level / ranking, organizer, event type — worth the extra complexity now, or defer?
   - the schedule itself — fixed (nightly), or manager-editable?

2. **How many events may one run import?** A first run against a year-wide window can match hundreds of events. Is a cap per run needed, and what happens to the remainder — next run, or reported as skipped?

3. **Is an automatically imported event created as DRAFT?** A manually imported event's status follows the existing import rules. An event nobody asked for arguably should not go straight to the members' calendar, which argues for DRAFT and an explicit publish. To confirm against the current import behaviour.

4. **Can a manager reject a discovered event?** Without it, an unwanted event is imported again on the next run after being deleted. A per-event "ignore" list would prevent that. Needed for MVP, or acceptable to leave out?

5. **Where does the settings page live?** Administration area, next to the other club-level settings — to confirm.
