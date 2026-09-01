---
name: fe-bundle-asset-deletion
description: Refreshed frontend bundle in backend/src/main/resources/static leaves the old hashed asset's deletion unstaged — stage it explicitly before committing
metadata:
  type: feedback
---

When committing a refreshed frontend bundle (after `npm run refresh-backend-server-resources`), the new `assets/index-*.js` is often staged while the **old** hashed asset's deletion stays unstaged (` D` in `git status --short`).

**Why:** The refresh step runs `git add` only on the new file it writes; it never stages the removal of the superseded hash-named asset. Committing without staging the deletion leaves a stale ~old bundle tracked in the repo alongside the live one.

**How to apply:** Before committing anything under `backend/src/main/resources/static/`, read `git status --short` and explicitly `git add` the deleted old asset path too — then commit; git records it as a rename. Also confirm `index.html` and `sw.js` reference the new hash so the pair is consistent.

Related: [[git-operator-new-files]]
