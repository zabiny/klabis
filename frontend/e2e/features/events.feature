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

  Scenario: Manager updates a DRAFT event
    Given I am logged in as "admin"
    And a draft event "{prefix}Autumn race" exists
    When I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}Autumn race"
    And I open the list row "{prefix}Autumn race"
    And I click "Upravit"
    And I fill "Místo" with "Brno"
    And I click "Uložit změny"
    Then I see "Brno"
