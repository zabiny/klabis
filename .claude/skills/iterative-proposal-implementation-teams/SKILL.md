---
name: iterative-proposal-implementation-teams
description: Implement an OpenSpec proposal using a Claude Code agent team (parallel teammates) instead of sequential isolated subagents. Use when the proposal spans multiple independent file domains (typically backend + frontend) and parallel work would shorten wall-clock time. Falls back to sequential mode for single-domain proposals.
disable-model-invocation: false
---

You are the **team lead** implementing OpenSpec proposal `$1`. You follow the
`openspec-apply-change` skill, but instead of delegating to isolated subagents
one after another, you orchestrate a **Claude Code agent team** of parallel
teammates and gate their work yourself.

Agent teams are experimental and must be enabled
(`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`). If teams are unavailable, tell the
user and stop — do not silently fall back to subagents, because the user
explicitly chose the team workflow by invoking this skill.

## 0. Setup

1. Rename the conversation to match the proposal name (`$1`).
2. Read `openspec/changes/$1/proposal.md`, `design.md` (if present), `tasks.md`,
   and the affected `openspec/specs/*` deltas. These documents are the source of
   truth for every decision.
3. Build a TODO plan (TODO tool) for **all** task groups before starting.

## 1. Parallelizability analysis (decides team vs. fallback)

Inspect `tasks.md` and the proposal's **Impact** section. Classify the work by
**file domain**:

- `backend/src/**` (backend module)
- `frontend/src/**` (React/TS)
- shared/other (specs, docs)

Then:

- **Multi-domain (backend + frontend):** proceed with the team workflow below.
- **Single-domain (e.g. backend-only):** parallel teammates would edit the same
  files and race. **Fall back to sequential mode** — implement exactly as
  `iterative-proposal-implementation` does (one subagent per task group, in
  sequence). Do not spawn a team.

Never split two teammates across the **same** file domain — that causes write
conflicts. Teammates must own disjoint directory trees.

## 2. Identify the shared contract (sync point)

When backend and frontend run in parallel, find the **contract** that couples
them — almost always the REST DTO / API shape (for example, an event DTO field
changing from `coordinator` to `coordinators[]`). This becomes a **task
dependency**: the frontend teammate must not wire UI to the API until the
backend teammate has confirmed the final DTO shape.

State this contract explicitly in the spawn prompt.

## 3. Spawn the team

Spawn teammates sized to the domains, with disjoint file ownership:

- **backend teammate** — agent type `backend-developer`. Owns `backend/`.
  Instruct it to follow the `backend-patterns` skill.
- **frontend teammate** — agent type `frontend-developer`. Owns `frontend/`.
  Instruct it to follow the `hal-navigator-patterns` skill.

In each teammate's prompt include: the relevant task groups from `tasks.md`, the
shared contract, its file-ownership boundary, and the rule that it must **not**
run Gradle/build/tests itself (the lead serializes all testing — see §5).

Express the contract dependency: the frontend teammate waits for the backend
teammate to confirm the DTO shape before binding forms/detail/list to it.

Keep the team to **2–3 teammates**. More is coordination overhead, not speed.

## 4. Parallel implementation

Teammates implement their domains concurrently. As lead you:

- Relay the confirmed contract from backend to frontend when it lands.
- Keep each teammate's iteration scoped to a task group from `tasks.md`.
- Update `tasks.md` (mark completed tasks) as groups finish.

You (the lead) **never edit `src/*` yourself** — all code changes go through a
teammate.

## 5. Test gate — ALWAYS sequential (critical)

**Concurrent Gradle invocations deadlock on the build cache lock.** Teammates
may *write code* in parallel, but **all test execution is serialized by the
lead**:

- Run tests via the `test-runner` subagent (`developer:test-runner-skill`), one
  run at a time. Never let two teammates run Gradle/tests simultaneously.
- On failure: **resume the teammate that wrote the code** to fix it — do not
  spawn a fresh teammate for fixes. Fix at most **5 failing tests per
  iteration**; if more remain, iterate again.
- It must be confirmed that all tests **compile and pass** before any commit.
  Never commit on red.

## 6. Commit per iteration

After a task group is green and `tasks.md` is updated, the **lead** commits
(delegate to `git-operator`). Use Conventional Commits.

## 7. Finalization (sequential, unchanged)

After all iterations are complete:

1. Run the `simplify` skill over all commits created for this proposal. Ask the
   appropriate teammate (resume it) to fix all medium-and-higher priority
   issues.
2. Run the full test suite via `test-runner`; confirm everything compiles and is
   green.
3. Commit the final fixes (lead → git-operator), verify `git status`.
4. Sync and archive the completed proposal.

## Autonomy and escalation

The team runs **autonomously** through implementation, testing, and commits —
you do not stop to ask the user for routine choices. **The one exception:** if a
decision touches **design** and the answer is **not** in
`proposal.md`/`design.md`/specs, you must **escalate to the user** rather than
invent it — guessing risks implementing the wrong thing. Minor implementation
approaches (naming, local structure, test fixes, iteration splitting) you decide
yourself.

## Bad practices (avoid)

- Do not run Gradle/tests in parallel across teammates — cache-lock deadlock.
- Do not split two teammates across the same file domain — write conflicts.
- Do not ask a teammate to fix *all* failing tests at once — iterate in batches
  of ≤5, resuming the teammate that wrote the code.
- Do not spawn a fresh teammate to fix tests — resume the original author.
- Do not commit without confirmed green tests.
- Do not edit `src/*` as lead — always go through a teammate.
- Do not implement anything outside the scope of the open proposal. If unrelated
  changes are needed, ask the user to create a new spec proposal.
