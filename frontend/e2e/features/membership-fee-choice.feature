@spec:membership-fees
Feature: Member's fee level choice
  Members choose one of the published fee levels for the campaign year before the voting deadline
  and see their current level on their profile. After the deadline the choice is locked.
  Eva is a club member, Jan a membership administrator.

  Rule: Until the deadline a member can choose, change or withdraw the level, also one assigned by an administrator

    Scenario: Member chooses a level
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      When Eva chooses level "Plus"
      Then Eva's profile shows fee level "Plus"

    Scenario: Member changes the choice before the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Basic"
      When Eva changes the fee level choice to "Plus"
      Then Eva's profile shows fee level "Plus"

    Scenario: Member withdraws the choice before the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Basic"
      When Eva withdraws the fee level choice
      Then Eva's profile prompts to choose a fee level

    Scenario: Member withdraws a level assigned by an administrator
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Jan has assigned Eva to level "Basic"
      When Eva withdraws the fee level choice
      Then Eva's profile prompts to choose a fee level

  Rule: The previous year's level is offered as a non-binding default

    Scenario: Previous year's level is offered as a default
      Given Eva had level "Plus" in the previous fee selection campaign
      And an active fee selection campaign offers levels "Basic" and "Plus"
      When Eva opens the fee level choice form
      Then level "Plus" is pre-selected
      And Eva's profile prompts to choose a fee level

  Rule: After the deadline the choice is locked

    Scenario: Member cannot change the choice after the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Basic"
      And the fee selection campaign is closed
      When Eva views the profile
      Then Eva's profile shows fee level "Basic" as locked
      And Eva is not offered to change the fee level choice

    Scenario: Member cannot withdraw the choice after the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Basic"
      And the fee selection campaign is closed
      When Eva opens the fee level choice form
      Then the choice form shows level "Basic" as locked
      And Eva is not offered to withdraw the fee level choice

  Rule: The profile shows the member's level for the relevant year

    Scenario: Member sees the assigned level on the profile
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Plus"
      When Eva views the profile
      Then Eva's profile shows fee level "Plus"

    Scenario: Member with no assigned level sees a prompt to choose
      Given an active fee selection campaign offers level "Basic"
      When Eva views the profile
      Then Eva's profile prompts to choose a fee level

    Scenario: Member still sees the chosen level after the campaign is closed
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Eva has chosen level "Plus"
      And the fee selection campaign is closed
      When Eva views the profile
      Then Eva's profile shows fee level "Plus" as locked
