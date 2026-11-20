# Tasks

Vertical slices; each slice is TDD (failing test first) and lands its own backend tests, frontend tests and API spec changes. Backend work follows the `backend-patterns` skill, API work the `klabis-api-spec` skill, frontend work the `hal-navigator-patterns` skill, E2E work the `gherkin-bdd` skill.

Slices 1 to 4 set the module up (feed, settings, delivery, minors) and are exercised through a test-only notification created by the service in tests. Slice 5 adds the first real producer.

## 1. Notification feed (module setup)

- [ ] 1.1 Add abstract `Notification` aggregate (`NotificationId`, recipient, `NotificationType`, derived `addressee()`, created/read time, `copyFor`, `targetPath`, `templateAttributes`) with a test subtype, memento with `type` discriminator and JSON `attributes`, repository and the `notifications` table in `V001__initial_schema.sql`; verify with domain tests (`markRead`, `isRead`, `isForGuardian`, `copyFor`) and a repository `@DataJdbcTest` round-tripping the subtype's attributes
- [ ] 1.2 `NotificationService.notify(notification)` stores the notification for the addressee and registers `NotificationCreated`; `NotificationRenderer` builds the template context (common attributes, current names from members, subtype attributes, link) and renders `templates/notifications/{type}/{channel}-title|text` through `TemplateRenderer`; verify with application-service tests (record stored, event with the id only) and renderer tests against test templates
- [ ] 1.3 Feed API in `notifications.yaml` (title and text rendered from the `feed-*` templates when read): `getNotificationFeed`, `getUnreadNotificationCount`, `markNotificationRead`, `markAllNotificationsRead`, root link `notificationFeed`; regenerate bundle and frontend types; implement the controller; verify with `@WebMvcTest` (own notifications only, newest first, another user's notification answers 404, templates present only while unread)
- [ ] 1.4 Frontend: bell with unread count (incl. "99+") in `Layout` and the feed page on `/notifications` (list, unread highlight, click marks read and navigates, "Označit vše jako přečtené", empty state); verify with component tests
- [ ] 1.5 `NotificationRetentionJob` (`@Scheduled`, nightly) deletes notifications older than 90 days; verify with a test using a fixed `Clock` (ADR-006)

## 2. Notification settings (module setup)

- [ ] 2.1 Add `NotificationPreferences` aggregate with overrides over per-type defaults, memento, repository and the `notification_preferences` table in `V001`; verify with domain tests covering defaults and overrides (100 %)
- [ ] 2.2 Settings API: extend `getNotificationSettings` with `pushAvailable`, `managedByGuardians` and `preferences`; add `updateNotificationPreferences` on `/api/notifications/preferences/{memberId}` with `x-klabis-owner-visible` and `memberId` as `x-klabis-target-id: MEMBER` (guardian authority comes in 4.2); root link `settings` (section `main`, "Nastavení") always present; drop the old `notifications` root link; device templates only when push is configured; verify with `@WebMvcTest` (own update ok, someone else's 403, `MEMBERS:MANAGE` holder 403, unknown type 400)
- [ ] 2.3 Frontend: "Nastavení" page on `/settings/notifications` with the "Nastavení notifikací" tab hosting the moved device UI (hidden without push) and the type × channel table; "Nastavení" menu entry in the desktop main section, not in the mobile bottom nav; feed links to settings; home-page "Notifikace" panel removed; test notification target `/settings/notifications`; verify with component tests and updated `Layout`/`HomePage` tests
- [ ] 2.4 E2E: member switches off e-mail for "Záporný zůstatek" and the setting persists after reload; desktop menu shows "Nastavení"; verify the scenarios pass

## 3. Delivery by push and e-mail

- [ ] 3.1 `PushDeliveryListener` (`@ApplicationModuleListener` on `NotificationCreated`) sends push rendered from the `push-*` templates to an adult addressee per their settings; verify with unit tests (push on, push off, no devices)
- [ ] 3.2 `EmailDeliveryListener` sends the e-mail rendered from `email-title.txt`/`email-text.html` (absolute link from the base URL) per the addressee's settings, skips recipients without an e-mail, swallows SMTP failures; verify with unit tests incl. a failing `EmailService` leaving the stored notification intact
- [ ] 3.3 Modulith scenario test: `NotificationService.notify` → stored record → push and e-mail sent after commit; verify the test passes

## 4. Notifications about a minor reach the guardians

- [ ] 4.1 Members public API: `LegalGuardians.guardiansOf(MemberId)` returning `LegalGuardianContactDto` (members and non-members) and `Members.isMinor(MemberId)`; verify with application-service tests and the Modulith module structure test
- [ ] 4.2 ReBAC relation: `Authority.LEGAL_GUARDIAN_OF` (`MEMBER`, `SPECIFIC` only) granted by `LegalGuardianGroupRelationshipSource` over each guarded minor; add it to `updateNotificationPreferences` (`x-klabis-authority: [LEGAL_GUARDIAN_OF]`); verify with relationship-source tests and `@WebMvcTest` (guardian ok, other member 403, `MEMBERS:MANAGE` holder 403, former guardian after the minor left the group 403)
- [ ] 4.3 Service routing: for a minor addressee store `copyFor(guardian)` per guardian; listeners apply design D4 (minor: no push, own e-mail if set; guardians: push and e-mail per the minor's settings; guardians' own settings ignored); a minor updating own settings is rejected; verify with unit tests for each row of the D4 table and for the minor rejection
- [ ] 4.4 Settings API: embedded `minors` with their preferences and the update template per child, `managedByGuardians` read-only view for the minor (no update template); verify with `@WebMvcTest`
- [ ] 4.5 Frontend: per-child section on the settings tab, read-only own settings with the note for a minor; verify with component tests

## 5. Negative balance notification (first producer)

- [ ] 5.1 `MemberAccount` registers `MemberAccountBalanceChanged(eventId, memberId, previousBalance, newBalance, occurredAt)` in deposit, charge, registration charge and reversal; verify with domain unit tests for each operation (100 % of the new branches)
- [ ] 5.2 `NegativeBalanceNotification` (`accountId`, `newBalance`, link to the account for the owner and to the child's detail for a guardian) with its six templates in `templates/notifications/negative-balance/` (push text without amount); `NegativeBalanceListener` creates it for the account owner only when `previous >= 0 && new < 0`; verify with domain tests for `targetPath`, rendering tests for all six templates, verify with unit tests for crossing, already negative, back to zero, exactly zero, and a Modulith scenario test from a finance charge to stored notifications for a minor and both guardians
- [ ] 5.3 E2E in `member-accounts.feature`: finance manager charges a member below zero, member sees the bell count and the "Záporný zůstatek" notification linking to Finance; E2E in `legal-guardians.feature`: a minor's charge below zero shows the notification in the guardian's feed naming the child; verify the scenarios pass

## 6. Finishing

- [ ] 6.1 Add an ADR to `docs/design-decisions.md` for event-based notification integration, fan-out on write and the `LEGAL_GUARDIAN_OF` relation; update the authorization reference table in the `backend-patterns` skill for the new grant; verify both read correctly
- [ ] 6.2 Run the full backend test suite, frontend tests and the E2E suite against a local environment; verify all pass and `openspec validate introduce-notification-feed --strict` is clean
- [ ] 6.3 Add label `BackendCompleted` to [#28](https://github.com/zabiny/klabis/issues/28), [#354](https://github.com/zabiny/klabis/issues/354) and [#27](https://github.com/zabiny/klabis/issues/27) once the backend part is merged

## Workflow follow-up

- Merge the device requirements of `push-notifications` into the `notifications` spec after archive (one spec per module).
- Move `NotificationRetentionJob` to Quartz once `introduce-quartz-scheduler` is implemented.
- Point the guardian's negative balance notification at the minor's account once [#451](https://github.com/zabiny/klabis/issues/451) is done.
- Archive the change after review.
