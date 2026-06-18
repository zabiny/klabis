## 1. Split the existing charge into a limited and an unlimited path

The current `MemberAccount.charge(...)` already enforces the overdraft limit. This work splits it in two — the limit check moves to a new `chargeForRegistration(...)`, and `charge(...)` keeps only the unconditional debit. No new business logic is written from scratch.

- [x] 1.1 Repoint the existing overdraft tests in `MemberAccountTest` (within-limit succeeds, exactly-on-limit succeeds, breach throws `OverdraftLimitExceededException` leaving history + balance unchanged) from `charge(...)` onto `chargeForRegistration(...)` — these assert the behavior being moved, not new behavior
- [x] 1.2 Add `MemberAccount.chargeForRegistration(amount, note, occurredAt, recordedAt, recordedBy, overdraftPolicy)` by copying the current `charge(...)` body (positive-amount assert + `allowsCharge` check + debit), preserving today's limit behavior on this path
- [x] 1.3 Extract the shared debit (create negative-amount `OTHER` transaction, append, subtract from balance) into a private helper used by both methods, so the policy check stays only on `chargeForRegistration` and the unlimited path never references `OverdraftPolicy`

## 2. Make the existing charge path unlimited

- [x] 2.1 Update `MemberAccountTest.charge(...)` tests: assert a finance-manager charge is recorded even when it pushes the balance below the overdraft limit (-400 → -700); remove the now-obsolete "charge breaches limit is rejected" assertions on `charge(...)` (their coverage now lives on `chargeForRegistration`)
- [x] 2.2 Remove the `OverdraftPolicy` parameter and the `allowsCharge` check from `MemberAccount.charge(...)`, leaving the positive-amount assert plus the shared debit helper
- [x] 2.3 Update `finance.application.ChargeService` to call the now-unlimited `charge(...)` (drop the `OverdraftPolicy` argument); keep `ChargePort`/`ChargeCommand` and the `POST .../transactions/charge` endpoint unchanged
- [x] 2.4 Run finance application + web-layer tests green

## 3. Confirm automatic membership-fee charge is unlimited

- [x] 3.1 Add/adjust a test proving `CampaignProcessor.chargeYearlyFees(...)` records the yearly fee even when it pushes a member's balance below the overdraft limit
- [x] 3.2 Verify no membership-fee test still asserts overdraft rejection; run membershipfees module tests green

## 4. Verify and finalize

- [ ] 4.1 Run the full finance + membershipfees test suites; confirm domain logic coverage (100% on the new method, charge, reverse paths)
- [ ] 4.2 Run `openspec validate overdraft-limit-registration-only --strict` and confirm the change is valid
- [ ] 4.3 Code review (code-reviewer agent) before commit
