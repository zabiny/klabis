## Why

Today the globally configured overdraft limit blocks *every* charge recorded by a finance manager (and every automatic membership-fee charge), which is too restrictive: legitimate administrative deductions and yearly fee charges must always go through regardless of a member's balance. The limit is only meaningful as a guard against members spending money they don't have on event registrations — a self-service flow that does not exist in the code yet. This change repositions the overdraft limit so it protects only registration payments, while administrative and automatic charges are unconstrained.

## What Changes

- **BREAKING** (behavioral): The existing charge operation (used by manual finance-manager charges and by the automatic yearly membership-fee charge) **no longer enforces** the overdraft limit. These charges always succeed and may push the balance arbitrarily below the limit.
- Introduce a separate, limit-enforcing charge path intended for **event registration payments**. A charge made through this path is rejected if it would push the balance below the configured overdraft limit (current behavior, now scoped to registrations only).
- The overdraft limit configuration itself (single, global, non-positive amount) is unchanged.
- `reverse()` is unchanged — reversals remain exempt from the limit.
- **Scope guard:** This change only introduces the rule and the domain method/path in the `member-accounts` capability. The actual event-registration integration (who calls the limit-enforcing charge, and when) is a future change — no registration code is added here, so the new path has no production caller yet.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `member-accounts`: The "Configurable Overdraft Limit" and "Recording a Charge" requirements change. Manual/administrative charges (and automatic membership-fee charges) are no longer limited; the overdraft limit applies only to charges originating from event registration payments. A distinct limit-enforcing charge path is specified alongside the existing unlimited charge.

## Impact

- **Domain** (`finance.domain.MemberAccount`): `charge()` drops the `OverdraftPolicy` check; a new limit-enforcing method (e.g. `chargeForRegistration`) is added.
- **Application** (`finance.application`): unchanged ports `ChargePort`/`ChargeService` keep delegating to the now-unlimited `charge()`. A new port/method for the limit-enforcing path may be added but stays uncalled until the future registration change.
- **Callers**: `membershipfees.application.CampaignProcessor.chargeYearlyFees()` and the manual `POST .../transactions/charge` endpoint both continue using the unlimited `charge()` — behavior change is that they can now exceed the limit.
- **REST API** (`MemberAccountController`): no new endpoint required by this change; the existing charge endpoint stays unlimited. Authorization (`FINANCE:MANAGE`) unchanged.
- **Tests**: `MemberAccountTest` overdraft assertions move from `charge()` to the new method; `CampaignProcessor` / membership-fee tests must tolerate charges below the limit.
- No frontend, persistence schema, or configuration changes.
