@spec:events
Feature: Events

  Scenario: Manager creates an event with all required fields
    Given I am logged in as "admin"
    When I open the application
    And I click "Akce"
    And I click "Přidat akci"
    And I fill "Název" with "{prefix}Spring race"
    And I fill "Datum konání" with "{date+60}"
    And I fill "Pořadatel" with "ZBM"
    And I select "Nepoužívá se" in "Použití SI čipu"
    And I click "Odeslat"
    Then I see "{prefix}Spring race"
    And I see "Koncept"
    When I click "← Zpět na seznam"
    And I fill "Hledat" with "{prefix}Spring race"
    Then I see "{prefix}Spring race"
