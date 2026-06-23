## Context

Two primary adapters currently perform application-layer orchestration:

- `MemberAccountController` injects `MemberAccountRepository` directly to look up individual transactions (`stream().filter()`) and batch-fetch reversal mappings (`findReversalsOf()`), then merges that data before building the response.
- `FeeSelectionCampaignController.listPublications()` chooses between two port methods (`listPublications()` / `listClosedPublications()`) based on a `?status=closed` query parameter.

Both patterns violate the dependency rule: primary adapters should only translate HTTP ↔ domain, not coordinate domain queries.

## Goals / Non-Goals

**Goals:**
- Move transaction lookup and reversal resolution into `TransactionQueryPort`
- Move campaign-list filtering decision into `FeeSelectionCampaignManagementPort`
- Remove `MemberAccountRepository` injection from `MemberAccountController`
- Keep the HTTP API contract (URLs, request/response shapes, HATEOAS links) identical

**Non-Goals:**
- Changing domain model or aggregate boundaries
- Optimising query performance
- Modifying frontend code

## Decisions

### TransactionQueryPort — extend with single-transaction lookup and reversal data

**Current state:** `TransactionQueryPort` only has `findTransactions(TransactionQuery)` returning a `Page<Transaction>` without reversal information. The controller calls `memberAccountRepository.findReversalsOf()` separately.

**Decision:** Introduce a `TransactionWithReversal` projection (or enrich `TransactionQueryPort`) to return both the transaction page and the reversal map in one call. Also add `findTransaction(MemberId, TransactionId)` so `getTransaction()` no longer loads the full account and filters in-memory.

```
TransactionQueryPort
  findTransactions(TransactionQuery) → Page<TransactionWithReversal>
  findTransaction(MemberId, TransactionId) → TransactionWithReversal

record TransactionWithReversal(Transaction transaction, Optional<TransactionId> reversedBy)
```

**Alternative considered:** Keep two separate port calls but move them to a new `TransactionReadPort`. Rejected — the reversal map is always needed alongside the page; combining them avoids a second round-trip and keeps the port cohesive.

### FeeSelectionCampaignManagementPort — add status-aware list method

**Current state:** Port exposes two separate methods (`listPublications()`, `listClosedPublications()`); the controller picks between them.

**Decision:** Add `listPublications(CampaignStatusFilter filter)` where `CampaignStatusFilter` is an enum (`ALL`, `CLOSED`). Existing methods remain as default implementations delegating to the new one to avoid breaking other callers, and can be removed in a follow-up.

**Alternative considered:** Pass `String status` directly. Rejected — stringly-typed parameters cross a module boundary and lose compile-time safety.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| `TransactionWithReversal` projection introduces a new type in the application layer boundary | Type is simple (record wrapping existing domain objects); no serialisation impact |
| Existing `@WebMvcTest` slices mock the ports — signatures change | Tests are updated as part of this change |
| `listClosedPublications()` may still be called from other places (e.g. scheduler) | Grep for usages before removing; keep as deprecated delegate until confirmed unused |
