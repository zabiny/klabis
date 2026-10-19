@spec:application-navigation
Feature: Application navigation

  Scenario Outline: User sees only menu items they are authorized for
    Given I am logged in as "<role>"
    When I open the application
    Then the main menu shows "Kalendář"
    And the main menu shows "Akce"
    And the main menu shows "Finance"
    And the main menu shows "Skupiny"
    And the main menu shows "Členové"
    And the main menu <visibility> "<item>"

    Examples:
      | role   | visibility    | item                 |
      | admin  | shows         | Přehled administrace |
      | admin  | shows         | Synchronizace        |
      | member | does not show | Přehled administrace |
      | member | does not show | Synchronizace        |
