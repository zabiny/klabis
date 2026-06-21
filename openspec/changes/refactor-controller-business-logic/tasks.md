## 1. TransactionQueryPort — single-transaction lookup

- [x] 1.1 Add `findTransaction(MemberId, TransactionId): Transaction` to `TransactionQueryPort` with a failing test in `TransactionQueryServiceTest`
- [x] 1.2 Implement `findTransaction` in `TransactionQueryService` (load account, filter by id, throw `TransactionNotFoundException` if absent)
- [x] 1.3 Replace `memberAccountRepository.findById()` + `stream().filter()` in `MemberAccountController.getTransaction()` with `transactionQueryPort.findTransaction()`
- [x] 1.4 Run `MemberAccountControllerTest` — confirm no behaviour change

## 2. TransactionQueryPort — reversal map included in page result

- [x] 2.1 Introduce `record TransactionWithReversal(Transaction transaction, Optional<TransactionId> reversedBy)` in the `finance.application` package
- [x] 2.2 Add `findTransactionsWithReversals(TransactionQuery): Page<TransactionWithReversal>` to `TransactionQueryPort` with a failing test
- [x] 2.3 Implement the method in `TransactionQueryService`: delegate to existing `findTransactions`, then call `memberAccountRepository.findReversalsOf()` to enrich results
- [x] 2.4 Replace the two-call sequence in `MemberAccountController.listTransactions()` with a single `findTransactionsWithReversals()` call
- [x] 2.5 Remove `MemberAccountRepository` injection from `MemberAccountController`; confirm compile passes (repository still needed for getAccount/getTransaction — injection kept)
- [x] 2.6 Run `MemberAccountControllerTest` — confirm no behaviour change

## 3. FeeSelectionCampaignManagementPort — status-aware list

- [x] 3.1 Add enum `CampaignStatusFilter { ALL, CLOSED }` to `membershipfees.application`
- [x] 3.2 Add `listPublications(CampaignStatusFilter): List<FeeSelectionCampaign>` to `FeeSelectionCampaignManagementPort` with a failing test in `FeeSelectionCampaignManagementServiceTest`
- [x] 3.3 Implement in `FeeSelectionCampaignManagementService`: delegate to existing `listPublications()` / `listClosedPublications()` based on filter value
- [x] 3.4 Replace `"closed".equals(status) ? … : …` branch in `FeeSelectionCampaignController.listPublications()` with `managementPort.listPublications(filter)`
- [x] 3.5 Run `FeeSelectionCampaignControllerTest` — confirm no behaviour change

## 4. Cleanup

- [x] 4.1 Check remaining usages of `listClosedPublications()` and `listPublications()` (no-arg); deprecate or remove if unused outside the service (deprecated — only used as internal delegates in service)
- [x] 4.2 Run full `finance` and `membershipfees` test suites — all green (3115/3115)
