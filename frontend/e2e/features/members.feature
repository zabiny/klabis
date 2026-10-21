# Not automated here:
# - "Adult registered with guardians is rejected": the registration form hides the guardian section of an adult, so only the API can send it (backend integration test).
# - "A registration number already in use is refused": the registration form and API take no registration number, only the ORIS import does.
# - "Minor cannot edit own profile": a registered minor has no password, so no session of a minor can be opened.
@spec:members
Feature: Members
  Club members are registered and maintained by member administrators, every member keeps their own profile up to date.
  Jan is a member administrator, Eva is a club member.

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

    Scenario: Minor registered without guardians is rejected
      When Jan registers a minor member with last name "Sam" without a legal guardian
      Then the registration is refused
      And member "Sam" is not registered

  Rule: Users find members in the member list by a search text

    Scenario: User can search members by last name
      Given member "Svoboda" is registered
      When Eva searches the member list for "Svoboda"
      Then only member "Svoboda" is listed

  Rule: Authorized users see the member's detail

    Scenario: Authorized user views active member detail
      Given member "Dvorak" is registered
      When Jan opens the detail of member "Dvorak"
      Then the detail shows the personal data and address of member "Dvorak"

  Rule: An adult member updates their own information

    Scenario: Member updates their own information
      Given the phone number of Eva is "+420 600 000 000"
      When Eva changes the phone number in the profile to "+420 777 123 456"
      Then Eva's profile shows the phone number "+420 777 123 456"

  Rule: A member cannot update another member's profile

    Scenario: Member cannot edit another member's profile
      Given member "Soused" is registered
      When Eva opens the detail of member "Soused"
      Then Eva is not offered to edit the profile of member "Soused"

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

    Scenario: Suspension blocked when member has a negative financial account balance
      Given member "Dluznik" is registered
      And the account of member "Dluznik" is overdrawn by 150 CZK
      When Jan suspends member "Dluznik"
      Then the suspension is blocked because the balance is -150 CZK
      And member "Dluznik" is still active
