## Source

- **GitHub issue:** https://github.com/zabiny/klabis/issues/380 — "Založení člena pro zákonného zástupce"
- **Milestone:** review-5
- **Labels:** none
- **Last synced:** 2026-11-11T00:42:57Z
- **Issue updated_at at sync:** 2026-11-10T23:42:15Z

## Why

When a non-member legal guardian (typically a parent of a junior) decides to join the club, the admin has to register them as a member. The registration already supports this — requirement "Member Registration Flow", scenario "Adult registration takes over an existing non-member guardian" in `openspec/specs/members/spec.md` lets the admin pick an existing non-member guardian, pre-fills the form from the guardian's profile and turns the guardian into a member who keeps their EXT login number and their minors.

The flow is hard to discover. The takeover section of the registration form appears only after the admin has typed a date of birth of an adult (scenario "Registration form switches sections by date of birth"), so an admin who starts the form wanting to register "Eva Svobodová, mother of our junior" does not see the option at all until mid-way through — and by then may already have typed her name and e-mail by hand, which then collide with the guardian's existing e-mail. The guardian's own profile page (requirement "Non-Member Legal Guardian Profile" in `openspec/specs/legal-guardians/spec.md`) offers no way to start the registration either, although it is the natural place where the admin finds the person.

## What Changes

**Registration form — takeover section always visible**
- The takeover section ("Převzetí zákonného zástupce") is shown from the moment the registration form opens, regardless of the date of birth — empty, adult or minor.
- Choosing a guardian for takeover together with a date of birth of a minor is rejected on submit with a validation error that a minor cannot take over the role of a legal guardian; nothing is created.
- The "Zákonní zástupci" section for a minor keeps following the date of birth as today.

**Guardian profile — "Registrovat jako člena" action**
- The non-member legal guardian profile shows a "Registrovat jako člena" button to users with MEMBERS:MANAGE (the permission that guards member registration). The guardian viewing their own profile never sees it.
- The button opens the registration form with this guardian pre-selected for takeover and first name, last name, e-mail and telephone pre-filled from the profile. The pre-filled values stay editable.
- After a successful registration the admin lands on the new member's detail page (they started from a specific person and typically continue with documents or licences, which the registration form does not contain). "Zrušit" returns to the guardian profile.

**Taken-over guardian gets a registration number; the EXT login keeps working**
- A guardian registered as a member through the takeover gets a registration number by the usual rules, as any other new member. The registration number becomes their user name.
- Their former EXT login number stays usable as an alternative user name: on the login page (and in the account activation form) they can enter either the registration number or the EXT login number.
- A user is always presented by their user name (registration number) — in the application, in the identity tokens and in the audit trail — regardless of which login they entered.
- Today the takeover keeps the EXT login number as the only login of the new member (scenario "Adult registration takes over an existing non-member guardian"); this changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `members`: the registration form shows the guardian takeover section from the start, rejects a takeover combined with a minor's date of birth, and can be opened with a guardian already chosen for takeover.
- `legal-guardians`: the non-member legal guardian profile offers "Registrovat jako člena", which opens the registration form with that guardian pre-selected for takeover.
- `users`: a user may have an alternative user name; logging in and requesting an activation link accept the user name or the alternative user name; the user is always presented by the user name.
- `users-authentication`: membership detection of a former guardian who logs in with their former EXT login number.

## Impact

**Affected specs:**
- `openspec/specs/members/spec.md` — "Member Registration Flow": change scenario "Registration form switches sections by date of birth" (takeover section always visible); add scenarios for takeover rejected with a minor's date of birth and for the registration opened from a guardian profile (pre-selection, landing on the new member's detail, cancel back to the profile). "Member Registration Page Layout": takeover section placement.
- `openspec/specs/legal-guardians/spec.md` — new requirement "Registering A Non-Member Guardian As A Member" (the "Registrovat jako člena" action, its visibility, pre-fill, landing and cancel).
- `openspec/specs/members/spec.md` — "Member Registration Flow": the takeover scenario gives the guardian a registration number and keeps the EXT login as an alternative user name.
- `openspec/specs/users/spec.md` — "User Authentication" (alternative user name, presentation by user name), "Password Setup Token Reissuance" (activation request with either login).
- `openspec/specs/users-authentication/spec.md` — "Membership Detection Based on Member Aggregate Existence": former guardian logging in with the EXT login.

**Affected code (backend, members module):**
- Non-member legal guardian profile response — new hypermedia link to the member registration, offered only to MEMBERS:MANAGE holders.
- Registration takeover — the guardian's user gets the new registration number as user name, the EXT login number becomes the alternative user name.

**Affected code (backend, users / authorization server):**
- User account — optional alternative user name.
- Login and activation-link request — find the account by user name or alternative user name; the issued tokens always carry the user name.

**Affected code (frontend):**
- Registration form guardian sections — takeover section no longer depends on the date of birth; selection is not cleared when a minor's date of birth is entered.
- Registration form — accepts a pre-selected guardian for takeover; after submit navigates to the new member's detail when opened from the guardian profile; "Zrušit" returns to the profile.
- Non-member legal guardian profile page — new "Registrovat jako člena" button.

**APIs (REST):** additive only (new link on the guardian profile response). The registration request itself does not change.

**Dependencies:** none.

**Data:** users table gets an optional, unique alternative user name column.

**Out of scope:**
- Changing the rest of the takeover (kept guardian relations, pre-filled fields).
- Showing or editing the alternative user name in the UI; an alternative user name is only ever set by the takeover.
- Registering a guardian who is already a club member (they have no non-member profile).
- A "Registrovat jako člena" action on the legal guardian group detail or on the minor's detail page.
- Self-service: a guardian applying for membership themselves.
- Stale "MEMBERS:CREATE" mentions in existing specs (the authority does not exist; registration is guarded by MEMBERS:MANAGE) — to be cleaned up separately.

## Open Questions

None — all questions were answered in the issue discussion.
