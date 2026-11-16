@spec:events
Feature: Event coordinators
  A member without the event manager permission who coordinates an event can see, edit, publish and
  prepare it. Jan is an event manager and Eva is a club member without that permission.

  Rule: Coordinators see their own DRAFT events in the event list

    Scenario: Coordinator sees their own DRAFT event in the list
      Given Eva coordinates the draft event "Coordinated race"
      And a draft event "Foreign race" exists
      When Eva opens the event list
      Then the event list contains "Coordinated race"
      And the event list does not contain "Foreign race"

  Rule: Coordinators edit and publish their own events

    Scenario: Coordinator edits their own DRAFT event
      Given Eva coordinates the draft event "Coordinated race"
      When Eva changes the location of event "Coordinated race" to "Znojmo"
      Then event "Coordinated race" takes place in "Znojmo"

    Scenario: Coordinator publishes their own DRAFT event
      Given Eva coordinates the draft event "Coordinated race"
      When Eva publishes event "Coordinated race"
      Then event "Coordinated race" is active

    Scenario: Coordinator cannot publish an event they do not coordinate
      Given Eva coordinates the draft event "Coordinated race"
      And event "Foreign race" is open for registration
      When Eva opens the detail of event "Foreign race"
      Then Eva is not offered to publish or cancel event "Foreign race"

  Rule: Coordinators apply category presets to their events

    Scenario: Coordinator applies a category preset in the event form
      Given category preset "Youth classes" offers categories "H12, D12"
      And Eva coordinates the draft event "Coordinated race"
      When Eva applies category preset "Youth classes" to event "Coordinated race"
      Then event "Coordinated race" offers categories "H12, D12"

    Scenario: Coordinator opens the event form and the category presets without forbidden requests
      Given category preset "Youth classes" offers categories "H12, D12"
      And Eva coordinates the draft event "Coordinated race"
      When Eva opens the category presets of the edit form of event "Coordinated race"
      Then the application made no forbidden requests

  Rule: The event list can be filtered by coordinator

    Scenario: Member filters the event list by coordinator
      Given Jan coordinates the open event "Jan's race"
      And Eva coordinates the open event "Eva's race"
      When Eva filters the event list by coordinator Jan
      Then the event list contains "Jan's race"
      And the event list does not contain "Eva's race"

    Scenario: Clearing the coordinator filter shows all events again
      Given Jan coordinates the open event "Jan's race"
      And Eva coordinates the open event "Eva's race"
      And Eva has filtered the event list by coordinator Jan
      When Eva clears the coordinator filter
      Then the event list contains "Eva's race"
