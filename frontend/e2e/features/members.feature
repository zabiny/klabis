# Not automated here:
# - "Adult registered with guardians is rejected": the registration form hides the guardian section of an adult, so only the API can send it (backend integration test).
# - "A registration number already in use is refused": the registration form and API take no registration number, only the ORIS import does.
# - "Minor registered without guardians is rejected": the rule is asserted by the backend `RegistrationServiceTest.shouldRejectMinorWithoutGuardian`.
# - "Minor cannot edit own profile": a registered minor has no password, so no session of a minor can be opened.
# - "Authorized user views active member detail": the sections and values shown on the detail are asserted by `MemberDetailPage.test` (sections, address, contact, badge), the fields each viewer receives by the backend `BirthNumberAuditMatchesSerializationApiTest`; every other journey opens the detail.
# - "Member cannot change their own nationality": the read-only reserved fields of the edit form are asserted by `MemberDetailPage.test` ("self-edit template with reserved fields marked readOnly", "nationality marked readOnly or absent in the edit template"), and the backend rejects them in `UpdateMemberApiTest`.
# - "Member cannot edit another member's profile": the "Upravit profil" button is shown iff the updateMember template is offered (`MemberDetailPage.test` "other member view (no template)"), and the template is offered only to holders of the edit grant (backend `*RelationshipSource` tests, `UpdateMemberApiTest`).
# - "Member enters a country outside the common list": the country picker reveals the code input and emits the entered code (`HalFormsCountry.test` "reveals the code input when other country is chosen", "emits upper-case code typed into the input and shows its name").
# - "Suspended member detail shows suspension info": the deactivation section (reason, note, deactivating user) is asserted by `MemberDetailPage.test` ("deactivation section"), and the reason and note returned on the detail of a suspended member by the backend `MemberControllerApiTest.suspendedMemberShouldReturnUpdateAndResumeAffordances`.
# - Suspension blocked by the last owner of a training group, by a negative balance, by both, and the whole-group suspension blocked by a minor's negative balance: the blockers are asserted by the backend `ManagementServiceTest` (`shouldThrowWhenMemberIsLastGroupOwner`, `shouldThrowWhenMemberHasNegativeBalance`, `shouldReportBothBlockersWhenBothConditionsApply`, `shouldBlockEverybodyWhenMinorHasDebt`), their 409 bodies by `MemberControllerApiTest` (`shouldReturn409WhenMemberIsLastGroupOwner`, `shouldReturn409WhenMemberHasOutstandingDebt`, `shouldReturn409WithDependentMembers`), and the warning dialog with groups, balance and dependent members by the frontend `SuspensionWarningDialog.test` and `useSuspendMemberAction.test`; `MemberDetailPage.groups.test` and `MembersPage.groups.test` assert the wiring.
# - "Admin designates a successor owner of a free group", "Admin dissolves a free group", "A free group without other members can only be dissolved": the actions offered per free group and the dissolving are asserted by `SuspensionWarningDialog.test` ("free group row"); the effect of designating a successor (resolved row, resubmitted suspension) by the `MemberDetailPage.groups` and `MembersPage.groups` flow tests, which share the row logic with training groups; the 409 listing of free groups by the backend `ManagementServiceTest.shouldThrowWhenMemberIsLastGroupOwner` and `MemberControllerApiTest.shouldReturn409WhenMemberIsLastGroupOwner`. The "successor trainer" journey below covers the successor flow through UI and API.
# - "User can search members by last name": the search text filter is asserted by the backend `MemberRepositoryTest` (fulltext filter cases: first name, last name, registration number, case and diacritics, tokens) and its pass-through of the `q` parameter by `MemberControllerApiTest.shouldPassQParamAsFulltextQuery`; the search input and its request by the frontend `MembersFilterBar.test` ("calls onChange with updated q when user types 2+ chars") and `membersQuery.test` ("sends q only with at least 2 characters"). Every journey that registers a member finds it in the list through the same search ("the member list contains member").
@spec:members
Feature: Members
  Club members are registered and maintained by member administrators, every member keeps their own profile up to date.
  Jan is a member administrator, Eva and Tomáš are club members.

  Rule: The system processes member registration by creating a user account and a member profile in a single flow

    Scenario: Admin registers a new adult member
      When Jan registers an adult member with last name "Novak"
      Then the member list contains member "Novak"

  Rule: A minor is registered together with at least one legal guardian

    Scenario: Admin registers a minor with an existing guardian
      Given member "Zastupce" is registered
      When Jan registers a minor member with last name "Dite" and legal guardian "Zastupce"
      Then the member list contains member "Dite"
      And the detail of member "Dite" lists "Zastupce" as legal guardian

    Scenario: Admin registers a minor with a new non-member guardian
      When Jan registers a minor member with last name "Mladsi" and a new legal guardian "Rodic"
      Then the member list contains member "Mladsi"
      And the detail of member "Mladsi" lists "Rodic" as legal guardian

  Rule: An adult member updates their own information

    Scenario: Member updates their own information
      Given the phone number of Eva is "+420 600 000 000"
      When Eva changes the phone number in the profile to "+420 777 123 456"
      Then Eva's profile shows the phone number "+420 777 123 456"

  Rule: Membership is suspended with a reason and the suspension stays visible on the member

    Scenario: Admin suspends an active member
      Given member "Prochazka" is registered
      When Jan suspends member "Prochazka"
      Then member "Prochazka" is suspended

    Scenario: Admin designates a successor trainer and finishes the suspension
      Given member "Trener" is registered
      And member "Trener" is the only trainer of training group "Dorost"
      And member "Nastupce" is registered
      When Jan suspends member "Trener" designating member "Nastupce" as successor of training group "Dorost"
      Then member "Trener" is suspended

  Rule: Suspending the sole legal guardian of minors can suspend the whole legal guardian group

    Scenario: Admin suspends the whole legal guardian group
      Given member "Zastupce" is registered
      And member "Zastupce" is the only legal guardian of minors "Prvni" and "Druhy"
      When Jan suspends member "Zastupce" together with the whole legal guardian group
      Then these members are suspended because of "Odhlášení":
        | Zastupce |
        | Prvni    |
        | Druhy    |
