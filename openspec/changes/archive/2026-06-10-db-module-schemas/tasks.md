## 1. SQL migrace — CREATE SCHEMA a přesun tabulek

- [x] 1.1 Přidat `CREATE SCHEMA` pro všechna doménová schémata na začátek `V001__initial_schema.sql` (members, common, events, calendar, groups, finance, membershipfees)
- [x] 1.2 Přesunout tabulky `members` modulu do schématu `members` (`members`, `birth_number_audit_log`)
- [x] 1.3 Přesunout tabulky `common` modulu do schématu `common` (`users`, `user_permissions`, `password_setup_tokens`)
- [x] 1.4 Přesunout tabulky `events` modulu do schématu `events` (`events`, `event_types`, `event_type_oris_disciplines`, `event_registrations`, `category_presets`, `member_registration_block`)
- [x] 1.5 Přesunout tabulky `calendar` modulu do schématu `calendar` (`calendar_items`, `calendar_feed_token`)
- [x] 1.6 Přesunout tabulky `groups` modulu do schématu `groups` (`user_groups`, `user_group_owners`, `user_group_members`, `user_group_invitations`)
- [x] 1.7 Přesunout tabulky `finance` modulu do schématu `finance` (`member_account`, `finance_transaction`)
- [x] 1.8 Přesunout tabulky `membershipfees` modulu do schématu `membershipfees` (`membership_fee_level`, `membership_payment_rule`, `fee_year_publication`, `fee_year_publication_level`, `membership_fee_group`, `membership_fee_group_rule_snapshot`, `fee_group_membership`, `yearly_fee_charge_marker`)
- [x] 1.9 Upravit cross-schema FK syntaxi na `schema.table` formát (`events.events → members.members`, `events.event_registrations → members.members`)

## 2. Java @Table anotace

- [x] 2.1 Přidat `schema = "members"` do `MemberMemento` a `BirthNumberAuditLogMemento`
- [x] 2.2 Přidat `schema = "common"` do `UserMemento`, `UserPermissionsMemento`, `PasswordSetupTokenMemento`
- [x] 2.3 Přidat `schema = "events"` do `EventMemento`, `EventTypeMemento`, `OrisDisciplineMemento`, `EventRegistrationMemento`, `CategoryPresetMemento`, `MemberRegistrationBlockMemento`
- [x] 2.4 Přidat `schema = "calendar"` do `CalendarMemento`, `CalendarFeedTokenMemento`
- [x] 2.5 Přidat `schema = "groups"` do `GroupMemento`, `GroupOwnerMemento`, `GroupMemberMemento`, `GroupInvitationMemento`
- [x] 2.6 Přidat `schema = "finance"` do `MemberAccountMemento`, `TransactionMemento`
- [x] 2.7 Přidat `schema = "membershipfees"` do `MembershipFeeLevelMemento`, `MembershipPaymentRuleMemento`, `FeeYearPublicationMemento`, `PublishedLevelRefMemento`, `MembershipFeeGroupMemento`, `MembershipPaymentRuleSnapshotMemento`, `FeeGroupMembershipMemento`, a `YearlyFeeChargeMarkerMemento` (pokud existuje)

## 3. Test SQL soubory

- [x] 3.1 Přidat schema prefix do `test-members-setup.sql`, `test-members-filter-setup.sql`, `test-member-lifecycle-setup.sql`
- [x] 3.2 Přidat schema prefix do `test-past-event-with-registration.sql`, `test-past-deadline-event-with-registration.sql`
- [x] 3.3 Přidat schema prefix do `test-data/member-with-email.sql`, `test-data/member-without-email.sql`
- [x] 3.4 Přidat schema prefix do `db/cleanup.sql`

## 4. Ověření

- [x] 4.1 Spustit celou backend test suite — všechny testy musí projít bez funkčních změn
- [ ] 4.2 Ověřit, že H2 console (dev profil) zobrazuje tabulky ve správných schématech
