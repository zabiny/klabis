# Design

## Context

The registration edit dialog (`EventRegistrationDialog`) is already shared by the events list, the registrations table and the new-registration entry points; it loads the registration by link and picks the mode (`new`/`edit`/`view`) from the affordances in the response. Event summaries and event detail already carry the `editRegistration` affordance on their `self` link when the user is registered and editing is allowed, so no backend work is needed. See proposal.md for motivation.

Reproduced prefill bug (Playwright, dev profile): the dialog builds `resourceData` correctly (including `categoryId` derived from `category.id`), but the form receives the raw server response, which lacks `categoryId` and omits null notes. Cause: `useHalFormData` always calls `useAuthorizedQuery(targetUrl, {enabled: shouldFetch && ...})`. When `shouldFetch` is false (template target equals current resource) the query is disabled, but it shares its cache key (`['authorized', pathname, search]`) with the dialog's own query, so a disabled query still returns the cached raw response. `formData = fetchedData ?? currentResourceData` then prefers it over the caller's prepared `resourceData`.

## Goals / Non-Goals

**Goals:**
- Every entry point (dashboard, events list, event detail) opens the same dialog, driven only by the `editRegistration` affordance.
- Edit form always shows the existing registration values.

**Non-Goals:**
- Backend or API changes.
- Redesigning the dialog or the unregister confirmation flow.
- Changing the events list or registrations table behaviour.

## Decisions

1. **Fix prefill in `useHalFormData`, not in the dialog.** Use fetched data only when `shouldFetch` is true: `formData = shouldFetch && fetchedData ? fetchedData : currentResourceData`. The hook's own contract says that same-resource targets "return the current resource data without any network requests", so the dialog workaround (adding `categoryId`) was already correct and is only being overridden. Alternative — a distinct query key for form data — rejected: it would add a duplicate request and leave the contract violated.
2. **Dashboard reads the affordance from the embedded event.** `useMyUpcomingRegistrations` additionally exposes the `editRegistration` template target per item; `HomePage` renders an edit button when present and opens `EventRegistrationDialog` with that target as link (same approach as `EventsPage`). The button stops click propagation so the row link to the event detail is not followed.
3. **Event detail replaces `HalFormButton name="unregisterFromEvent"` with an edit button** shown from `_templates.editRegistration` of the event resource. Unregister remains reachable via the dialog (`unregisterTemplate` already rendered there). Alternative — keeping both buttons — rejected by the request to reduce clutter.
4. **Refresh after saving**: dialog `onChanged` refetches the page's data (dashboard widget query, event detail) so the displayed state matches.

## Risks / Trade-offs

- [`useHalFormData` is shared by all HAL+FORMS forms; forms that unintentionally relied on cached raw data would lose values] → run the full frontend suite, add a regression test for the same-target case, and manually check member edit and other inline-edit forms.
- [Unregister is one click further away on the event detail] → accepted trade-off; confirmation step already exists in the dialog.
- [Dashboard rows are links; nested button may trigger navigation] → stopPropagation/preventDefault covered by a test.
