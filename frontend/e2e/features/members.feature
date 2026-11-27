# Not automated here:
# - "Adult registered with guardians is rejected": the registration form hides the guardian section of an adult, so only the API can send it (backend integration test).
# - "A registration number already in use is refused": the registration form and API take no registration number, only the ORIS import does.
# - "Minor registered without guardians is rejected": the rule is asserted by the backend `RegistrationServiceTest.shouldRejectMinorWithoutGuardian`.
# - "Minor cannot edit own profile": a registered minor has no password, so no session of a minor can be opened.
# - "Authorized user views active member detail": the sections and values shown on the detail are asserted by `MemberDetailPage.test` (sections, address, contact, badge), the fields each viewer receives by the backend `BirthNumberAuditMatchesSerializationApiTest`; every other journey opens the detail.
# - "Member cannot change their own nationality": the read-only reserved fields of the edit form are asserted by `MemberDetailPage.test` ("self-edit template with reserved fields marked readOnly", "nationality marked readOnly or absent in the edit template"), and the backend rejects them in `UpdateMemberApiTest`.
# - "Member cannot edit another member's profile": the "Upravit profil" button is shown iff the updateMember template is offered (`MemberDetailPage.test` "other member view (no template)"), and the template is offered only to holders of the edit grant (backend `*RelationshipSource` tests, `UpdateMemberApiTest`).
# - "Member enters a country outside the common list": the country picker reveals the code input and emits the entered code (`HalFormsCountry.test` "reveals the code input when other country is chosen", "emits upper-case code typed into the input and shows its name").
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

  Rule: Users find members in the member list by a search text

    Scenario: User can search members by last name
      Given member "Svoboda" is registered
      When Eva searches the member list for "Svoboda"
      Then only member "Svoboda" is listed

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

    Scenario: Suspended member detail shows suspension info
      Given member "Kratochvil" is suspended because of "Transfer to Brno"
      When Jan opens the detail of member "Kratochvil"
      Then the detail of member "Kratochvil" shows the suspension with note "Transfer to Brno"

    Scenario: Suspension blocked when member is last owner of a training group
      Given member "Trener" is registered
      And member "Trener" is the only trainer of training group "Dorost"
      When Jan suspends member "Trener"
      Then the suspension is blocked because member "Trener" is the last owner of training group "Dorost"
      And member "Trener" is still active

    Scenario: Admin designates a successor trainer and finishes the suspension
      Given member "Trener" is registered
      And member "Trener" is the only trainer of training group "Dorost"
      And member "Nastupce" is registered
      When Jan suspends member "Trener" designating member "Nastupce" as successor of training group "Dorost"
      Then member "Trener" is suspended

    Scenario: Suspension blocked by both a negative balance and group ownership
      Given member "Trener" is registered
      And member "Trener" is the only trainer of training group "Dorost"
      And the account of member "Trener" is overdrawn by 150 CZK
      When Jan suspends member "Trener"
      Then the suspension is blocked by one warning showing the balance -150 CZK and training group "Dorost"
      And member "Trener" is still active

    Scenario: Suspension blocked when member has a negative financial account balance
      Given member "Dluznik" is registered
      And the account of member "Dluznik" is overdrawn by 150 CZK
      When Jan suspends member "Dluznik"
      Then the suspension is blocked because the balance is -150 CZK
      And member "Dluznik" is still active

  Rule: Suspending the last owner of a free group requires resolving the group first

    Scenario: Admin designates a successor owner of a free group from the warning dialog
      Given Tomáš is the only owner of the free group "Pratele"
      And Eva is a member of the free group "Pratele" owned by Tomáš
      When Jan suspends Tomáš designating Eva as successor owner of the free group "Pratele"
      Then Tomáš is suspended

    Scenario: Admin dissolves a free group from the warning dialog
      Given Tomáš is the only owner of the free group "Zanikajici"
      When Jan suspends Tomáš dissolving the free group "Zanikajici"
      Then Tomáš is suspended

    Scenario: A free group without other members can only be dissolved
      Given Tomáš is the only owner of the free group "Samotari"
      When Jan suspends Tomáš
      Then the warning offers to dissolve the free group "Samotari" but not to designate a successor

  Rule: Suspending the sole legal guardian of minors can suspend the whole legal guardian group

    Scenario: Admin suspends the whole legal guardian group
      Given member "Zastupce" is registered
      And member "Zastupce" is the only legal guardian of minors "Prvni" and "Druhy"
      When Jan suspends member "Zastupce" together with the whole legal guardian group
      Then these members are suspended because of "Odhlášení":
        | Zastupce |
        | Prvni    |
        | Druhy    |

    Scenario: Whole-group suspension blocked by a minor's negative balance
      Given member "Zastupce" is registered
      And member "Zastupce" is the only legal guardian of minor "Dluznik"
      And the account of member "Dluznik" is overdrawn by 150 CZK
      When Jan attempts to suspend member "Zastupce" together with the whole legal guardian group
      Then the whole-group suspension is blocked because member "Dluznik" has a negative balance of -150 CZK
      And member "Zastupce" is still active
      And member "Dluznik" is still active
