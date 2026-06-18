## Why

Two primary adapters contain orchestration logic that belongs in the application layer: `MemberAccountController` performs transaction lookup and reversal mapping directly, and `FeeSelectionCampaignController` makes a routing decision between two port methods based on a query parameter. Controllers should translate HTTP to/from domain objects only.

## What Changes

- **`MemberAccountController.getTransaction()`** — removes inline `stream().filter()` lookup; a new `TransactionQueryPort.findTransaction(memberId, txId)` method handles the lookup in the application layer
- **`MemberAccountController.listTransactions()`** — removes the separate `memberAccountRepository.findReversalsOf()` call; the reversal map is folded into `TransactionQueryPort` so the controller makes a single port call
- **`MemberAccountController`** — removes the direct `MemberAccountRepository` injection (was only used for the above two patterns)
- **`FeeSelectionCampaignController.listPublications()`** — removes the `"closed".equals(status)` branch; a new `FeeSelectionCampaignManagementPort.listPublications(String status)` (or a dedicated query object) encapsulates the routing decision

No changes to API responses, HTTP status codes, or HATEOAS links.

## No Behavior Change Justification

This is purely an internal restructuring of where orchestration code lives. The HTTP contract — endpoints, request/response shapes, link relations, status codes, authorisation rules — is identical before and after.

**Specs reviewed:**
- `openspec/specs/member-accounts/spec.md` — unaffected; spec describes user-observable transaction and reversal behaviour, not which layer performs the lookup
- `openspec/specs/membership-fees/spec.md` — unaffected; spec describes campaign lifecycle, not how the list endpoint filters by status internally

## Impact

- `finance` module: `TransactionQueryPort`, `TransactionQueryService`, `MemberAccountController`
- `membershipfees` module: `FeeSelectionCampaignManagementPort`, `FeeSelectionCampaignManagementService`, `FeeSelectionCampaignController`
- Existing tests for both controllers will need updating to reflect the new port signatures
