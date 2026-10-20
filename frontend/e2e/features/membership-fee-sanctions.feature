@spec:membership-fees
Feature: Sanction for a missing fee level choice
  A member who has no fee level when the campaign ends cannot register for events and loses
  the registrations of events that are still open. An emergency assignment lifts the block,
  but does not restore the lost registrations.
  Eva is a club member, Jan a membership administrator.

  Rule: A member without a level is blocked and deregistered when the campaign ends

    Scenario: Member who missed the choice is blocked and deregistered
      Given an active fee selection campaign offers level "Basic"
      And Eva is registered for the open event "Sanction"
      And Eva has not chosen a fee level
      When the fee selection campaign ends
      Then Eva's registration for event "Sanction" is cancelled
      And Eva is not offered to register for event "Sanction"

    Scenario: Member who chose a level is not sanctioned
      Given an active fee selection campaign offers level "Basic"
      And Eva is registered for the open event "No sanction"
      And Eva has chosen level "Basic"
      When the fee selection campaign ends
      Then Eva remains registered for event "No sanction"

  Rule: An emergency assignment lifts the block, but cancelled registrations stay cancelled

    Scenario: Emergency assignment unblocks future registrations
      Given an active fee selection campaign offers level "Basic"
      And event "Unblock" is open for registration
      And Eva is blocked for missing the fee level choice
      When Jan assigns Eva to level "Basic"
      Then Eva is offered to register for event "Unblock"

    Scenario: Deregistered registrations are not restored automatically
      Given an active fee selection campaign offers level "Basic"
      And Eva is registered for the open event "Restore"
      And Eva is blocked for missing the fee level choice
      When Jan assigns Eva to level "Basic"
      Then Eva's registration for event "Restore" is cancelled
      And Eva is offered to register for event "Restore"
