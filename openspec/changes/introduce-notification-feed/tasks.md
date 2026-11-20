# Tasks

Vertical slices; each slice is TDD (failing test first) and lands its own backend tests, frontend tests and API spec changes. Backend work follows the `backend-patterns` skill, API work the `klabis-api-spec` skill, frontend work the `hal-navigator-patterns` skill.

## 1. Negative balance reaches the member's feed

- [ ] 1.1 `MemberAccount` registers `MemberAccountBalanceChanged(memberId, previousBalance, newBalance)` in deposit, charge, registration charge and reversal; verify with domain unit tests for each operation (100 % of the new branches)
- [ ] 1.2 Add `Notification` aggregate, `NotificationId`, `NotificationType.NEGATIVE_BALANCE`, memento, repository and the `notifications` table in `V001__initial_schema.sql`; verify with domain tests (`markRead`, `isRead`) and a repository `@DataJdbcTest`
- [ ] 1.3 `NegativeBalanceListener` (`@ApplicationModuleListener`) creates a notification for the account owner only when `previous >= 0 && new < 0`; verify with unit tests for crossing, already negative, back to zero, exactly zero, and a Modulith scenario test from a finance charge to a stored notification
- [ ] 1.4 Feed API in `notifications.yaml`: `getNotificationFeed`, `getUnreadNotificationCount`, `markNotificationRead`, `markAllNotificationsRead`, root link `notificationFeed`; implement controller and verify with `@WebMvcTest` (own notifications only, another user's notification answers 404, templates present only while unread)
- [ ] 1.5 Frontend: bell with unread count (incl. "99+") in `Layout` and the feed page on `/notifications` (list, unread highlight, click marks read and navigates, "Označit vše jako přečtené", empty state); verify with component tests
- [ ] 1.6 E2E scenario in `member-accounts.feature`: finance manager charges a member below zero, member sees the bell count and the "Záporný zůstatek" notification linking to Finance; verify `npm run test:e2e` for the feature passes

## 2. Push and e-mail follow the member's settings

- [ ] 2.1 Add `NotificationPreferences` aggregate with overrides over per-type defaults (adult and minor), memento, repository and `notification_preferences` table in `V001`; verify with domain tests covering defaults, overrides and minor/adult fallback (100 %)
- [ ] 2.2 `NotificationService` delivers push (text without amount) and e-mail (generic `notification.html`/`.txt` template, absolute link from base URL) per preferences, skips recipients without e-mail, swallows delivery failures; verify with unit tests incl. SMTP failure not affecting the stored notification
- [ ] 2.3 Settings API: extend `getNotificationSettings` with `pushAvailable`, `managedByGuardians`, `preferences`; add `updateNotificationPreferences`; root link `settings` (section `main`, "Nastavení") always present; drop the old `notifications` root link; device templates only when push is configured; verify with `@WebMvcTest`
- [ ] 2.4 Frontend: "Nastavení" page on `/settings/notifications` with the "Nastavení notifikací" tab hosting the moved device UI (hidden without push) and the type × channel table; "Nastavení" menu entry in the desktop main section, not in the mobile bottom nav; feed links to settings; home-page "Notifikace" panel removed; test notification target `/settings/notifications`; verify with component tests and updated `Layout`/`HomePage` tests
- [ ] 2.5 E2E: member switches off e-mail for "Záporný zůstatek" and the setting persists after reload; desktop menu shows "Nastavení"; verify the scenarios pass

## 3. Notifications about a minor reach the guardians

- [ ] 3.1 Members public API: `LegalGuardians.guardiansOf(MemberId)` returning `LegalGuardianContactDto` (members and non-members) and `Members.isMinor(MemberId)`; verify with application-service tests and the Modulith module verification test
- [ ] 3.2 `NotificationService` routing for a minor: feed for the minor and every guardian with the guardian text and target (minor's detail page), e-mail to all guardians and the minor's own e-mail per the minor's settings, push only to the minor; guardians' own settings ignored; verify with unit tests for each row of the routing table in design D4
- [ ] 3.3 Settings API: embedded `minors` with their preferences, `updateMinorNotificationPreferences` allowed only to the minor's current guardians, `managedByGuardians` read-only view for the minor; verify with `@WebMvcTest` (guardian ok, other user 403, unknown member 404, minor cannot update own preferences)
- [ ] 3.4 Frontend: per-child section on the settings tab, read-only own settings with the note for a minor; verify with component tests
- [ ] 3.5 E2E in `legal-guardians.feature`: a minor's charge below zero shows the notification in the guardian's feed naming the child; guardian changes the child's e-mail setting; verify the scenarios pass

## 4. Retention and finishing

- [ ] 4.1 `NotificationRetentionJob` (`@Scheduled`, nightly) deletes notifications older than 90 days; verify with a test using a fixed `Clock`
- [ ] 4.2 Update `docs/design-decisions.md` with an ADR for event-based notification integration and fan-out on write; verify the ADR is linked from the notifications module section
- [ ] 4.3 Run the full backend test suite, frontend tests and the E2E suite against a local environment; verify all pass and `openspec validate introduce-notification-feed --strict` is clean
- [ ] 4.4 Add label `BackendCompleted` to [#28](https://github.com/zabiny/klabis/issues/28), [#354](https://github.com/zabiny/klabis/issues/354) and [#27](https://github.com/zabiny/klabis/issues/27) once the backend part is merged

## Workflow follow-up

- Merge the device requirements of `push-notifications` into the `notifications` spec after archive (one spec per module).
- Move `NotificationRetentionJob` to Quartz once `introduce-quartz-scheduler` is implemented.
- Archive the change after review.
