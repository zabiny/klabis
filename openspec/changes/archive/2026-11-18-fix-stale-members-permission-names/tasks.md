# Tasks

No behavior changes, so there is no failing test to write first: the spec deltas carry the correction, and the two files outside the specs are test data and a configuration comment.

## 1. Specs

- [x] 1.1 Check that the deltas in `specs/members`, `specs/legal-guardians` and `specs/non-functional-requirements` differ from the main specs only in the permission name (`MEMBERS:UPDATE`/`MEMBERS:CREATE` -> `MEMBERS:MANAGE`). Verify: `openspec validate fix-stale-members-permission-names --strict` passes and `grep -rn "MEMBERS:UPDATE\|MEMBERS:CREATE" openspec/changes/fix-stale-members-permission-names` finds nothing.

## 2. Files outside the specs

- [x] 2.1 `backend/src/test/resources/sql/test-member-lifecycle-setup.sql`: replace the stored authorities with existing ones (`CALENDAR:MANAGE`, `MEMBERS:MANAGE`, `MEMBERS:READ`, `MEMBERS:PERMISSIONS`, `EVENTS:READ`, `EVENTS:MANAGE`). Verify: `MemberLifecycleCrossModuleTest` passes (test-runner).
- [x] 2.2 `backend/.env.example`: the `KLABIS_OAUTH2_CLIENT_SCOPES` example lists OAuth scopes (`openid,profile,email,MEMBERS,EVENTS,SYNC`, the default of `OidcRegisteredClientsBootstrap`). Verify: `grep -rn "MEMBERS:UPDATE\|MEMBERS:CREATE\|MEMBERS:DELETE" backend/.env.example backend/src` finds nothing.

## Workflow follow-up

- Sync the deltas and archive the change; afterwards `grep -rn "MEMBERS:UPDATE\|MEMBERS:CREATE" openspec/specs` finds nothing.
- Rebase `resolve-group-ownership-on-suspension` (#398) onto this change.
