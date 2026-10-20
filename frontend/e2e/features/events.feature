@spec:events
Feature: Events
  Event managers create and maintain the club's events. Jan is an event manager.

  Rule: A new event starts as a draft and is listed among the events

    Scenario: Manager creates an event with all required fields
      When Jan creates event "Spring race" filling in only the required fields
      Then event "Spring race" is a draft
      And the event list contains "Spring race"

  Rule: A draft event can be edited

    Scenario: Manager updates a DRAFT event
      Given a draft event "Autumn race" exists
      When Jan changes the location of event "Autumn race" to "Brno"
      Then event "Autumn race" takes place in "Brno"
