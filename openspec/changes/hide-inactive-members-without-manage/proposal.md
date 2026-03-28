## Why

Members without `MEMBERS:MANAGE` authority can currently see suspended (inactive) members in the member list and access their detail pages. Inactive members should be invisible to regular members — they represent terminated memberships that are only relevant to administrators.

## What Changes

- `GET /api/members` returns only **active** members for users without `MEMBERS:MANAGE` authority; users with `MEMBERS:MANAGE` continue to see all members (active and inactive)
- `GET /api/members/{id}` returns **404 Not Found** when the requested member is inactive and the caller lacks `MEMBERS:MANAGE` authority (intentional: avoids revealing the existence of inactive members)

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `members`: visibility rules for inactive members — list filters to active-only without MANAGE, detail returns 404 for inactive without MANAGE

## Impact

- **Backend**: `MemberController.listMembers()` must conditionally filter by active status based on caller's authority; `ManagementService.getMemberAndRecordView()` must enforce 404 for inactive members without MANAGE
- **MemberRepository**: needs `findAllActive(Pageable)` method (paginated variant of existing `findAllActive()`)
- **Frontend**: no changes needed — inactive members simply disappear from the list for non-admin users
- **Tests**: `MemberControllerApiTest`, `MemberControllerSecurityTest` need new scenarios
