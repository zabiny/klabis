---
name: member-sync-projection
description: MemberProjection/MemberProjectionMapper for ORIS member sync (import-oris-members change) — field mapping quirks worth remembering
metadata:
  type: project
---

Built for openspec change `import-oris-members`, tasks 6.1-6.5. Files:
- `backend/src/main/java/com/klabis/members/infrastructure/orissync/MemberProjection.java`
- `backend/src/main/java/com/klabis/members/infrastructure/orissync/MemberProjectionMapper.java`
- `backend/src/main/java/com/klabis/members/infrastructure/orissync/PhoneNumberNormalizer.java`

Related: [[project_sync_entity_type_addition]] for the `SyncEntityType.MEMBER`/`SyncEntityTypeParam.MEMBERS` registration this builds on.

Key mapping quirks discovered against `oris-client` 0.2.0's `ClubMember` DTO (`com.dpolach.api.orisclient.dto.ClubMember`):
- `si` (chip number) arrives as coerced `int`, not String; `si == 0` must map to `null`, not `"0"` — otherwise "no chip" and "chip 0" hash differently and produce a permanent phantom sync conflict.
- ORIS blank strings (`""`) must normalize to `null` on every optional text field for hash stability across sides.
- Birth number (`persNum`) and postal code (`zip`, which may contain an inner space e.g. `"600 00"`) pass through with **no transformation** — Klabis's own value-object patterns already accept these shapes.
- Phone numbers need normalization since Klabis `PhoneNumber` requires E.164 (`+` prefix) but ORIS sends bare national numbers. Only `CZ` is confidently handled (`+420` prefix) — anything else maps to `null` rather than guessing, per design.md's explicit call-out that this mapping needs future tuning against real data. `PhoneNumberNormalizer` is a small package-private static helper, deliberately not over-engineered.
- `Member.syncFromOris(SyncFromOris)` already existed before this task (added in an earlier commit `c423c3d5`) — do not recreate it; only projection/mapper were missing.
- `Member.register(...)` does NOT set `chipNumber` — it's always `null` post-registration. Tests needing a member with a chip number must additionally call `member.syncFromOris(...)` (or `update`) after registering.

Test-writing note: `oris-client`'s `ClubMember` has a generated `ClubMemberBuilder` (`@RecordBuilder`) — use `ClubMemberBuilder.builder()...build()` in tests rather than the raw record constructor, matching the `@RecordBuilder` convention used everywhere else in this codebase.
