@spec:event-registrations
Feature: Event registrations
  Members register for open events and cancel their registration again. Event registration managers
  can cancel the registration of any member. Eva is a club member, Jan a registration manager.

  Background:
    Given an active fee selection campaign offers level "Basic"
    And Jan has assigned Eva to level "Basic"

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

    Scenario: User with EVENTS:REGISTRATIONS unregisters another member
      Given Eva is registered for the open event "Relay"
      When Jan unregisters Eva from event "Relay"
      Then Eva's registration for event "Relay" is cancelled
