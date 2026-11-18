# Design

## Context

The feed token API (`/api/me/ical-token`, GET state / POST generate-or-rotate) is already user-scoped. Only its discovery is tied to the member profile: the `ical-token` link is added to the member detail resource when the caller views their own profile, and the frontend renders `CalendarFeedSection` from that link. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Discover and manage the feed from the calendar page, for every authenticated user.
- Keep the existing token API, token format and feed URLs untouched.

**Non-Goals:**
- No change to feed content, token storage or security.
- No new endpoint; no redesign of the section's content (URL, copy, help, regenerate).

## Decisions

### D1: `ical-token` link lives on the calendar items collection

The link (with the `generateToken` affordance) is added by the collection postprocessor of calendar items, next to `next`/`prev`. The frontend shows the "Kalendářový feed" button only when the link is present, which keeps the HAL-driven pattern and drops the "self profile only" check.

Alternatives: a link on the root resource (`/api`) — rejected, the feed is a calendar concern and the calendar page already reads the collection; keeping the link on member detail and linking from the calendar — rejected, it would keep the member coupling and exclude users without a member profile.

### D2: Modal opened from the calendar header

The button sits next to "Můj rozvrh" and "Přidat položku". The dialog reuses the section's content. The query for token state runs only while the dialog is open, so the calendar page load is unchanged.

Alternatives: a section below the grid (permanently takes space, hidden under the grid) and a header dropdown (no precedent in the project) — rejected.

### D3: Regeneration confirmation inline in the dialog

A nested modal inside a modal is avoided: the "Vygenerovat nový token" button switches to a confirm/cancel pair within the dialog. The one-time revealed full URL is held in component state, so closing the dialog discards it, as leaving the profile did before.

### D4: `CalendarFeedSection` moves to `pages/calendar`

It no longer depends on the member profile's `Section` wrapper; it renders as dialog content.

## API (affected parts)

| Aspect | Before | After |
|---|---|---|
| `GET /api/calendar-items` HAL links | `self`, `next`, `prev` | adds `ical-token` (href `/api/me/ical-token`, affordance `generateToken` = POST), for every authenticated caller |
| member detail HAL links | `ical-token` on caller's own detail | `ical-token` removed |
| `/api/me/ical-token` GET / POST | unchanged | unchanged (request/response, template name unchanged) |

Spec updates: `docs/openapi/spec/members.yaml` (drop `ical-token` link description), `docs/openapi/spec/calendar.yaml` (add `ical-token` to `x-hal-links` of `listCalendarItems`); regenerate the bundle and frontend types.

## Glossary

- **Calendar feed**: personal iCalendar subscription URL authenticated by a token, listing the user's "Můj rozvrh" events.
- **Feed token**: secret embedded in the feed URL; only its hash is stored, the full URL is shown once at generation.

## Risks / Trade-offs

- [Front-end tests and E2E referencing the profile section break] → update `MemberDetailPage` and `CalendarFeedSection` tests; no E2E scenario references the section.
- [Link added to every calendar list response, including month navigation] → cheap (static link); no performance concern at the expected load.
- [Users used to the profile section won't find it] → accepted; the profile section is removed, not duplicated.
