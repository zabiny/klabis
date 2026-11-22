## Source

- **GitHub issue:** https://github.com/zabiny/klabis/issues/451 — "Zákonný zástupce vidí finanční účet svého nezletilého dítěte"
- **Milestone:** none
- **Labels:** enhancement, Finance
- **Last synced:** 2026-11-20T22:50:33Z
- **Issue updated_at at sync:** 2026-11-20T22:50:29Z

## Why

A legal guardian usually pays for their minor child's club activities and needs to know how much to top up the child's prepaid account. Today the guardian cannot see the child's balance or transaction history at all, so they must ask the child or a finance manager.

Current state of the specs:

- `member-accounts` — "Member Sees Their Own Account and History" lets a member see only their own account; "Member cannot view another member's account" denies access to anyone without FINANCE:MANAGE.
- `legal-guardians` — "Legal Guardians Edit Their Minors' Data" gives the guardian all of the child's member data through the fixed delegation "Úprava údajů člena". The financial account is not part of it.
- `user-groups` — "Groups Delegate Permissions To Their Owners" already provides the mechanism by which a group's owners hold permissions over its members; today the only delegatable permission is "Úprava údajů člena".
- `application-navigation` — "Main Menu Offers "Finance" Entry for Every Authenticated Member" opens only the user's own account. A non-member legal guardian (EXT login) has no own account, so there is no Finance entry for them at all.

The gap also affects the notification feed proposal (`introduce-notification-feed`, PR #450): it notifies guardians about a child's negative balance, but until this change exists the notification can only point to the child's detail page instead of the child's account.

## What Changes

- **View only.** A legal guardian sees the balance and the transaction history of each of their minor children. They cannot record deposits, charges or reversals, and cannot pay anything from the child's account.
- **New delegatable permission "Zobrazení financí člena".** It lets the holder view a member's financial account. A legal guardian group always delegates it to its guardians over its minors (alongside "Úprava údajů člena"), and this cannot be changed. Free groups are NOT offered this permission in this change.
- **Entry from the child's detail page.** The minor's detail page offers the guardian an action that opens the child's financial account (the same place where a finance manager has the link).
- **Child switcher on the "Finance" page.**
  - A guardian who is a club member opens their own account by default and can switch to the account of any of their minor children.
  - A non-member guardian (EXT) gets the "Finance" entry in the main menu when they have at least one minor child. The switcher offers only their children (they have no own account); the first child's account opens by default.
- **Same history as the owner.** The guardian sees the account owner's identity, balance and full history, including who recorded each transaction.
- **Access ends with guardianship.** The guardian loses access when the child leaves the legal guardian group after turning 18 (daily run), or when the guardian is removed from the child's guardians.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `member-accounts`: legal guardians may view (read-only) the accounts of their minor children; the account page gets a child switcher on "Finance".
- `legal-guardians`: legal guardian groups always delegate "Zobrazení financí člena"; access ends at 18 or on removal.
- `user-groups`: new delegatable permission "Zobrazení financí člena", not offered to free groups.
- `application-navigation`: the "Finance" entry is also shown to a non-member guardian with at least one minor child.

## Impact

**Affected specs:**
- `openspec/specs/member-accounts/spec.md` — "Member Sees Their Own Account and History" and "Member cannot view another member's account" get the guardian exception; "Account Endpoint Is Reachable from the Member Detail" adds the guardian's link from the child's detail; "Account Page Shows Owner Identity" covers the guardian as viewer; new requirement for the child switcher on "Finance".
- `openspec/specs/legal-guardians/spec.md` — new requirement for viewing the minor's account (delegation, loss of access at 18 and on removal, non-member guardian).
- `openspec/specs/user-groups/spec.md` — "Free Group Delegated Permissions Are Chosen At Creation" (still offers only "Úprava údajů člena"); delegated permission list gains "Zobrazení financí člena".
- `openspec/specs/application-navigation/spec.md` — "Main Menu Offers "Finance" Entry for Every Authenticated Member" extended to non-member guardians with minors.

**Affected code (backend, finance + members modules):** new delegatable permission; authorization of the member account resource and its history for holders of the permission; links to the child's account from the minor's detail and to the guardian's children's accounts for the Finance switcher; Finance menu link for non-member guardians.

**Affected code (frontend):** account action on the minor's detail page; child switcher on the "Finance" page; read-only account page for a guardian.

**APIs (REST):** additive — account resources become readable by guardians, new links from the member detail, the Finance entry and the switcher.

**Dependencies:** none blocking. After this change, `introduce-notification-feed` (PR #450) should point the negative-balance notification to the child's account.

**Data:** none expected beyond whatever stores the delegated permissions of a group.

**Out of scope:**
- Recording deposits, charges or reversals by a guardian — remains FINANCE:MANAGE only.
- Paying the child's event registrations from the child's account and delegated registration — separate issue.
- Offering "Zobrazení financí člena" to free groups.
- Online top-up / payment gateway.

## Open Questions

None — all questions were answered on 2026-11-20.
