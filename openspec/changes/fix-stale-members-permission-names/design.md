# Design

## Context

See proposal.md - Why. Verified in code and the API spec: `registerMember`, `suspendMember` and `listLegalGuardianOptions` require `MEMBERS_MANAGE`; `updateMember` requires `MEMBERS_MANAGE` or `MEMBERS_EDIT_PROFILE` held over the member. The `Authority` enum has no `MEMBERS:UPDATE`, `MEMBERS:CREATE` or `MEMBERS:DELETE`. `KLABIS_OAUTH2_CLIENT_SCOPES` configures OAuth scopes of the registered web client (default `openid,profile,email,MEMBERS,EVENTS,SYNC` in `OidcRegisteredClientsBootstrap`), not permissions.

## Goals / Non-Goals

**Goals:**
- No spec names a permission that does not exist.

**Non-Goals:**
- Any change of who may do what.
- Rewording the touched requirements beyond the permission name (style, structure).
- Historical documents (`MEMBERS_SECTION_PRD.md`, `openspec/openspec-critique.html`).

## Decisions

### D1: Replace with MEMBERS:MANAGE everywhere

Every occurrence maps to an operation guarded by `MEMBERS_MANAGE`, so a mechanical replacement is correct. "MEMBERS:CREATE or MEMBERS:MANAGE" collapses to "MEMBERS:MANAGE". The editing scenarios that also mention "Úprava údajů člena" keep it - that is the real `MEMBERS:EDIT_PROFILE` path. Alternative of introducing the missing permissions was rejected: it would change authorization, which this change does not intend.

### D2: Full requirement blocks under MODIFIED

The deltas copy each affected requirement whole, differing only in the permission name, so archiving keeps every scenario. No requirement is renamed, split or reworded.

### D3: Test fixture gets existing permissions

`test-member-lifecycle-setup.sql` stores `MEMBERS:CREATE/UPDATE/DELETE`. They are replaced with existing permissions of equivalent intent (`MEMBERS:MANAGE` plus the others already listed). `MemberLifecycleCrossModuleTest` authenticates through `@WithKlabisMockUser`, so the fixture row is test data only; the test run confirms nothing relies on the old names.

## Domain model

No domain changes.

## REST API

No API changes.

## Risks / Trade-offs

- [#398 modifies Membership Suspension too] -> this change is archived first and #398 is rebased; its delta already uses MEMBERS:MANAGE, so the two do not conflict in content.
- [A stale name survives somewhere else] -> the archive follow-up greps the main specs.

## Glossary

No new domain terms.
