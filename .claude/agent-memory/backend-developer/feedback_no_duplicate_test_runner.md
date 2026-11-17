---
name: no-duplicate-test-runner
description: Never spawn a second test-runner agent to "wait for" a running one — it may start a concurrent Gradle run; notifications arrive on their own
metadata:
  type: feedback
---

Never spawn a second `developer:test-runner` agent while one is already running — not even with a prompt telling it to wait. The test-runner agent can start its own Gradle invocation, and two concurrent Gradle builds deadlock on cache locks (see the project rule against parallel Gradle).

**Why:** Spawned a "check the red run status" test-runner alongside the real run. It started its own tool call immediately (a Gradle invocation) and could not be stopped from the parent session — `TaskStop` fails for a sibling-owned task. Luck alone prevented a lock deadlock.

**How to apply:** Launch exactly one test-runner per run and wait for its completion notification. If a result seems slow, do not delegate a status check — background agents notify automatically. If a duplicate was spawned, `SendMessage` it to stop and do not start any other Gradle work until it reports idle.
