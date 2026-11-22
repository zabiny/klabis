@spec:event-registrations
Feature: Event registrations
  Members register for open events and cancel their registration again. Event registration managers
  can cancel the registration of any member. Eva is a club member, Jan a registration manager,
  Ivana the non-member legal guardian of the minor members Adam Dlouhý and Klára Dlouhá.

  Background:
    Given an active fee selection campaign offers type "Basic"
    And Jan has assigned Eva to type "Basic"

  Rule: A member can register for an active event with open registrations

    Scenario: Member registers for an event with categories
      Given event "Open sprint" is open for registration
      When Eva registers for event "Open sprint" in category "M21"
      Then Eva is registered for event "Open sprint" in category "M21"

  Rule: A member can cancel their registration

    Scenario: Member unregisters before event date
      Given Eva is registered for the open event "Night run"
      When Eva unregisters from event "Night run"
      Then Eva's registration for event "Night run" is cancelled

  Rule: A registration manager can cancel the registration of another member

    Scenario: User with EVENTS:REGISTER_MEMBER unregisters another member
      Given Eva is registered for the open event "Relay"
      When Jan unregisters Eva from event "Relay"
      Then Eva's registration for event "Relay" is cancelled

  Rule: A legal guardian manages the registrations of their minor children

    Scenario: Guardian edits their child's registration from the registrations list
      Given event "Family sprint" is open for registration in categories "M10, W10"
      And minor "Adam Dlouhý" is registered for event "Family sprint" in category "M10"
      When Ivana changes the category of minor "Adam Dlouhý" in event "Family sprint" to "W10"
      Then Ivana sees minor "Adam Dlouhý" registered for event "Family sprint" in category "W10"

    Scenario: Guardian unregisters their child
      Given event "Family relay" is open for registration in categories "M10, W10"
      And minor "Klára Dlouhá" is registered for event "Family relay" in category "W10"
      When Ivana unregisters minor "Klára Dlouhá" from event "Family relay"
      Then the registration of minor "Klára Dlouhá" for event "Family relay" is cancelled
