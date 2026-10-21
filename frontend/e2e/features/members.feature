@spec:members
Feature: Members
  Club members are registered and maintained by member administrators, every member keeps their own profile up to date.
  Jan is a member administrator, Eva is a club member.

  Rule: The system processes member registration by creating a user account and a member profile in a single flow

    Scenario: Admin registers a new adult member
      When Jan registers an adult member with last name "Novak"
      Then the member list contains member "Novak"

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

  Rule: Membership is suspended with a reason and the suspension stays visible on the member

    Scenario: Admin suspends an active member
      Given member "Prochazka" is registered
      When Jan suspends member "Prochazka"
      Then member "Prochazka" is suspended

    Scenario: Suspended member detail shows suspension info
      Given member "Kratochvil" is suspended because of "Transfer to Brno"
      When Jan opens the detail of member "Kratochvil"
      Then the detail of member "Kratochvil" shows the suspension with note "Transfer to Brno"
