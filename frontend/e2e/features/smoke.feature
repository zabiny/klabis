@spec:application-navigation
Feature: Login smoke test

  Scenario Outline: Authenticated user reaches the application
    Given I am logged in as "<role>"

    Examples:
      | role   |
      | admin  |
      | member |
