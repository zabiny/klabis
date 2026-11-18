@spec:event-settings
Feature: Event settings
  Disciplines, event types and category presets are administered on one page with a tab for each.
  Jan administers the club, Eva is a club member without event management permissions.

  # Not automated: "Tab the user cannot access is hidden" needs a user authorized for only some of the tabs,
  # which the seeded users are not.

  Rule: The page shows the tabs of event settings

    Scenario: Manager opens event settings
      When Jan opens "Nastavení akcí" from the main menu
      Then the event settings page shows the tabs:
        | Disciplíny        |
        | Typy akcí         |
        | Šablony kategorií |
      And the tab "Disciplíny" is selected

    Scenario: Manager switches between tabs
      Given Jan has opened event settings
      When Jan selects the tab "Typy akcí"
      Then the tab "Typy akcí" is selected
      And the tab "Typy akcí" is open at its own address

    Scenario: Selected tab survives page reload
      Given Jan has opened the tab "Šablony kategorií" of event settings
      When Jan reloads the page
      Then the tab "Šablony kategorií" is selected

    Scenario: Tab content works as before
      Given Jan has opened the tab "Typy akcí" of event settings
      When Jan creates event type "Sprint" on the tab
      Then the tab "Typy akcí" lists event type "Sprint"

  Rule: Event settings follow authorization

    Scenario: User without EVENTS:MANAGE has no event settings
      When Eva opens the application
      Then the main menu does not offer:
        | Nastavení akcí |

    Scenario: Event settings address is denied to a user without EVENTS:MANAGE
      When Eva opens the event settings address directly
      Then access is denied

  Rule: Previous administration addresses lead to event settings

    Scenario Outline: Old bookmark opens the matching tab
      When Jan opens the saved link to the former "<tab>" page
      Then the tab "<tab>" is selected

      Examples:
        | tab               |
        | Disciplíny        |
        | Typy akcí         |
        | Šablony kategorií |
