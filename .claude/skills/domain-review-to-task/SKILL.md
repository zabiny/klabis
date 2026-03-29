---
name: domain-review-to-task
description: "End-to-end domain layer code review that produces a task queue file. Runs parallel reviewers on specified modules, filters false positives with the user, grills the user on implementation variants, checks OpenSpec specs for conflicts, and writes a READY_FOR_IMPLEMENTATION task file. Use when user asks to review domain code and create fix tasks, audit domain layers for maintainability, or wants a structured review-to-task pipeline for backend modules."
user-invocable: true
---

# Domain Review to Task Queue

Orchestrates a structured pipeline: **review → filter → decide → spec-check → task file**.

The goal is to turn a domain-layer code review into a well-defined, implementation-ready task file — with the user making every design decision along the way.

## Phase 1: Parallel Domain Review

Launch one `feature-dev:code-reviewer` agent per module **in parallel**. Each agent reviews the module's domain layer with a focused prompt.

The user specifies which modules to review. If not specified, ask.

### Agent prompt template

For each module, spawn a code-reviewer agent with this prompt (adapt module name):

```
Review the domain layer of the "{module}" module in the backend for maintainability issues. Focus on:
- Business rules that are duplicated across multiple places
- Logic that should be in the domain but is in application/infrastructure layer
- Inconsistent patterns or abstractions
- Domain model anemia (logic outside aggregates/entities)
- Any other maintainability concerns

The backend is at {backend_path}

Start by finding all domain-layer files in the {module} module (look for packages like domain, model, aggregate, entity, valueobject, etc.). Read all domain files thoroughly, then also check application services/ports to see if business logic leaked there.

Produce a detailed review with specific file:line references for each issue found.
```

### Presenting results

After all agents complete, compile results into a single summary table per module. Each issue gets:
- Issue number (sequential across all modules)
- Short description
- Severity (Critical / Important / Moderate)
- File reference

Group cross-cutting patterns at the end (e.g., "duplicated business rules appear in 3 modules").

## Phase 2: Filter False Positives

Present the full table to the user and explicitly ask them to identify:
- **Intentional designs** — things that look like issues but are deliberate choices
- **Low-priority items to skip** — not worth fixing now

Remove flagged items from the working list. Acknowledge the user's reasoning for each removal — this avoids re-raising the same concerns later.

Keep track of the final validated issue list for subsequent phases.

## Phase 3: Grill Session — Resolve Implementation Variants

For each remaining issue where there are multiple reasonable implementation approaches, run a grill-me style interview with the user. The purpose is to reach a concrete decision, not to explore open-endedly.

### How to conduct the grill session

Walk through issues one by one. For each issue:

1. **Read the relevant code** — understand the current state before asking
2. **Present 2-3 concrete variants** with trade-offs. Name them (Varianta A/B/C) for easy reference
3. **State your recommendation** with reasoning
4. **Ask one question at a time** — wait for the user's answer before moving to the next issue
5. **If a question can be answered by reading code, read the code instead of asking**

Some issues have obvious fixes (e.g., a clear bug). Don't grill on those — state the fix and confirm. Save the interview for genuinely ambiguous decisions.

When issues are coupled (fixing one affects another), note the dependency and resolve them in the right order.

Record each decision as: issue number + chosen variant + brief rationale.

## Phase 4: OpenSpec Specification Check

Read all relevant OpenSpec specifications from `openspec/specs/` that relate to the affected modules.

For each validated issue, verify:
- The proposed fix does **not** change observable API behavior specified in specs
- If a fix would require spec changes, flag it and recommend either:
  - Adjusting the fix to stay within spec boundaries, or
  - Creating an OpenSpec proposal instead of a task

Only proceed to task file creation if all fixes are confirmed as pure refactoring (no functional changes).

## Phase 5: Create Task Queue File

Use the `openspec-ext:task-queue` skill to create the task file. Provide it with:
- The validated issue list with agreed implementation decisions
- File references for each issue
- The confirmation that specs were checked and no conflicts exist

The task file goes into `tasks/` with status READY_FOR_IMPLEMENTATION.

### Task file content expectations

- **Requirements**: One item per validated issue, describing WHAT to change (not HOW), except where a specific approach was agreed in the grill session — then include that decision
- **Success criteria**: One testable criterion per issue, plus "all existing tests pass"
- **Scope**: Explicitly list what was intentionally excluded (the false positives from Phase 2)
- **Notes**: Include coupling/ordering hints for implementation (e.g., "fix #12 before #11 — port signature change affects command records")
