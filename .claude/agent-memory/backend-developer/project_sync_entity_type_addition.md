---
name: sync-entity-type-addition
description: How to add a new SyncEntityType value (domain enum + generated SyncEntityTypeParam) when wiring a new sync adapter
metadata:
  type: project
---

Adding a new `SyncEntityType` constant (e.g. `MEMBER`) touches two enums that must stay in sync by wire value, not by name:

1. **Spec-first source of truth**: `docs/openapi/spec/sync.yaml` — `SyncEntityTypeParam` schema `enum: [events, disciplines, ...]`. Add the new path segment string here.
2. Regenerate: `./gradlew :openapiBundle` then `./gradlew compileJava` (or just `compileJava`, which triggers `openApiGenerateSync` first) — this regenerates `com.klabis.sync.infrastructure.restapi.SyncEntityTypeParam` (generated, in `build/generated/openapi/sync/...`) with the new constant.
3. **Domain enum**: `com.klabis.sync.domain.SyncEntityType` — add `MEMBER("members")` by hand. The generated `SyncEntityTypeParam.MEMBERS` and domain `SyncEntityType.MEMBER` deliberately have different constant names (`MEMBER` vs `MEMBERS`) — `SyncEntityTypeResolver` matches them only by `pathSegment()`/`getValue()` wire string, never by enum constant name.

No other file needs touching to register the enum value itself — `SyncProjectionType` (adapter registry) and the concrete adapter (`SynchronizationAdapter` impl) are separate, later wiring steps.

See [[project_member_sync_projection]] for the companion `MemberProjection`/`MemberProjectionMapper` pattern built on top of this.
