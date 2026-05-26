## 1. Finance Module Skeleton (Vertical Slice: Account Exists)

- [x] 1.1 Create Spring Modulith module `com.klabis.finance` with `domain`, `application`, `infrastructure` packages
- [x] 1.2 Add architectural test verifying `finance` module exposes only `application` to other modules
- [x] 1.3 Write failing test: `MemberAccount` factory creates a new account with zero balance for a given `MemberId`
- [x] 1.4 Implement `MemberAccount` aggregate root with `id = MemberId` and initial zero `Money` balance
- [x] 1.5 Implement `Money` value object (BigDecimal + Currency, CZK fixed in v1) with arithmetic invariants
- [x] 1.6 Write failing repository test using JdbcAggregateTemplate that persists and reloads `MemberAccount`
- [x] 1.7 Implement `MemberAccountMemento` + `MemberAccountJdbcRepository` and Flyway migration creating `member_account` table
- [x] 1.8 Write failing test: listener on `MemberRegisteredEvent` creates an account
- [x] 1.9 Implement `CreateAccountOnMemberRegistered` listener in `finance.application`
- [x] 1.10 Integration test (end-to-end): registering a member via members API results in an existing finance account

## 2. Vertical Slice: Deposit

- [ ] 2.1 Write failing domain test: `MemberAccount.deposit(positive amount, note, occurredAt, recordedBy)` appends a DEPOSIT transaction and increases balance
- [ ] 2.2 Write failing domain test: `deposit` with zero or negative amount throws domain exception
- [ ] 2.3 Implement `Transaction` entity (type, signed amount, note, recordedAt, occurredAt, recordedBy, reversesTransactionId nullable)
- [ ] 2.4 Implement `MemberAccount.deposit(...)` enforcing DEPOSIT.amount > 0 and balance update
- [ ] 2.5 Add Flyway migration creating `finance_transaction` table with partial unique index on `reverses_transaction_id`
- [ ] 2.6 Persistence test: persisting a `MemberAccount` with one deposit reloads identically
- [ ] 2.7 Add `FINANCE:MANAGE` authority to authority catalog and seed in test data
- [ ] 2.8 Write failing REST test: `POST /api/members/{id}/account/transactions` with deposit body requires FINANCE:MANAGE
- [ ] 2.9 Implement `MemberAccountController` with deposit endpoint (HAL+FORMS afford from account resource)
- [ ] 2.10 Write failing test: deposit endpoint returns `201 Created` with `Location` to the new transaction
- [ ] 2.11 HAL+FORMS test: `GET /api/members/{id}/account` for FINANCE:MANAGE includes `deposit` affordance; for plain member viewing own account does not

## 3. Vertical Slice: Charge with Overdraft Limit

- [ ] 3.1 Add `klabis.finance.overdraft-limit` to `application.yml` and bind via `FinanceProperties (@ConfigurationProperties)`
- [ ] 3.2 Write failing domain test: `charge(amount)` decreases balance and stores OTHER (negative) transaction when within overdraft limit
- [ ] 3.3 Write failing domain test: `charge` is rejected when resulting balance would fall below overdraft limit
- [ ] 3.4 Implement `MemberAccount.charge(...)` consuming `OverdraftPolicy` value derived from properties
- [ ] 3.5 Write failing REST test: `POST .../transactions` with charge body within limit succeeds
- [ ] 3.6 Write failing REST test: charge that would exceed overdraft returns problem detail with `OVERDRAFT_LIMIT_EXCEEDED`
- [ ] 3.7 Implement controller logic + exception handler for overdraft case
- [ ] 3.8 HAL+FORMS test: `charge` affordance present only for FINANCE:MANAGE

## 4. Vertical Slice: Reverse (Storno)

- [ ] 4.1 Write failing domain test: `reverse(transactionId)` of a deposit appends opposite-sign transaction referencing the original
- [ ] 4.2 Write failing domain test: `reverse` of a charge appends opposite-sign transaction referencing the original
- [ ] 4.3 Write failing domain test: `reverse` of an already-reversed transaction throws `TransactionAlreadyReversed`
- [ ] 4.4 Write failing domain test: reverse of a reversal is permitted (reversal chain)
- [ ] 4.5 Write failing domain test: reverse bypasses overdraft limit
- [ ] 4.6 Implement `MemberAccount.reverse(...)` with all invariants above
- [ ] 4.7 Write failing REST test: `POST .../transactions/{txId}/reverse` requires FINANCE:MANAGE
- [ ] 4.8 Implement reverse endpoint with HAL+FORMS afford on each not-yet-reversed transaction
- [ ] 4.9 Persistence test: partial unique DB index prevents two simultaneous reversals of the same transaction

## 5. Vertical Slice: Read Account and History

- [ ] 5.1 Write failing REST test: `GET /api/members/{id}/account` for the owner returns balance + history link
- [ ] 5.2 Write failing REST test: `GET .../account` for another member without FINANCE:MANAGE returns 403
- [ ] 5.3 Write failing REST test: `GET .../account` for any member with FINANCE:MANAGE returns the account
- [ ] 5.4 Implement read endpoint returning `MemberAccountResource` with HAL+FORMS links
- [ ] 5.5 Write failing REST test: `GET .../account/transactions` is paginated with default page size
- [ ] 5.6 Write failing REST test: sorting by occurredAt asc/desc, type asc/desc, amount asc/desc
- [ ] 5.7 Write failing REST test: filtering by date range and by type
- [ ] 5.8 Implement transactions read endpoint with paging, sorting, and filtering (Spring Data Pageable + criteria)
- [ ] 5.9 HAL+FORMS test: reversed transactions expose a `reversedBy` link to their reversing transaction
- [ ] 5.10 HAL+FORMS test: reversal transactions expose a `reverses` link to the original

## 6. Vertical Slice: Cross-Module Pre-Check on Suspend

- [ ] 6.1 Define `MemberFinancialStatePort` in `members.application` describing what members need to know about finances (e.g., `hasOutstandingDebt(MemberId)` plus minimal balance info for the 409 body)
- [ ] 6.2 Implement the port in `finance.application` as a secondary adapter reading from `MemberAccount` repository (dependency direction: `finance → members`)
- [ ] 6.3 Write failing application-service test in `members`: suspend with negative balance throws `MemberHasOutstandingDebtException`
- [ ] 6.4 Write failing application-service test: suspend with zero or positive balance proceeds (group ownership checks pass)
- [ ] 6.5 Implement balance pre-check in `MemberApplicationService.suspend(...)` analogous to `LastOwnershipCheckerImpl` flow
- [ ] 6.6 Write failing REST test: suspending a member with negative balance returns HTTP 409 with `{ balance, accountLink }` body
- [ ] 6.7 Add `MemberHasOutstandingDebtException` handler in `MembersExceptionHandler` returning 409 with structured body

## 7. Frontend: Own Account Page and Menu Entry

- [ ] 7.1 Add `Finance` link customizer to RootController response so authenticated members get a `account` root link
- [ ] 7.2 Add localization label "Finance" and related labels (Vklad, Strhnutí, Stornovat, Zůstatek, …) to `src/localization/labels.ts`
- [ ] 7.3 Implement `FinanceOwnAccountPage` consuming the root `account` link
- [ ] 7.4 Render balance prominently and the transactions table below
- [ ] 7.5 Implement transactions table with pagination, column sorting (date/type/amount), and filters (date range, type)
- [ ] 7.6 Visually distinguish reversed transactions and reversal transactions; render navigation links between them
- [ ] 7.7 Register `Finance` in main menu (everyday section, desktop sidebar + mobile bottom nav)

## 8. Frontend: Manager Access to Any Account

- [ ] 8.1 Add member-detail HAL link processor exposing `account` link for FINANCE:MANAGE
- [ ] 8.2 Add member-list row action (icon) opening the member's financial account, gated by FINANCE:MANAGE
- [ ] 8.3 Implement `MemberAccountManagePage` rendering balance + history with action affordances when present
- [ ] 8.4 Implement Deposit modal form (driven by HAL+FORMS `deposit` affordance)
- [ ] 8.5 Implement Charge modal form (driven by HAL+FORMS `charge` affordance), display overdraft error via problem detail
- [ ] 8.6 Implement Reverse action per-row (driven by HAL+FORMS `reverse` affordance) with confirmation modal
- [ ] 8.7 Hide all action affordances when not present in HAL response (member viewing own account)

## 9. Frontend: Suspend Dialog Negative-Balance Handling

- [ ] 9.1 Extend suspend dialog to render a balance-warning state when API returns 409 with negative-balance shape
- [ ] 9.2 Display current balance and a link to the member's financial account
- [ ] 9.3 Do not render any override toggle in the dialog
- [ ] 9.4 After the admin resolves the balance (closes dialog, opens account, deposits), re-attempting suspend succeeds

## 10. Permissions Management

- [ ] 10.1 Add `FINANCE:MANAGE` to the authority registry and permission management dialog
- [ ] 10.2 E2E test: a user granted `FINANCE:MANAGE` sees deposit/charge/reverse controls on any member's account
- [ ] 10.3 E2E test: a user without `FINANCE:MANAGE` sees only their own account and no action controls

## 11. Documentation and Wrap-Up

- [ ] 11.1 Update developer manual section on cross-module ports with the `MemberAccountQueryPort` example
- [ ] 11.2 Update OpenAPI spec snapshot tests
- [ ] 11.3 Run `openspec validate gh-273-member-accounts --strict`
- [ ] 11.4 Add label `BackendCompleted` to GitHub issue #273 once backend implementation is finished
