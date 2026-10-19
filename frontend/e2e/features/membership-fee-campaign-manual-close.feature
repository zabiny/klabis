@spec:membership-fee-campaign-manual-close
Feature: Membership fee campaign manual close

  Scenario: Administrator closes an active campaign
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Close tier"
    When I open the application
    And I click "Členské příspěvky"
    Then I see "Uzavřít kampaň"

  Scenario: Campaign is closed immediately
    Given I am logged in as "admin"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Plus"
    And a published event "{prefix}Close event" with open registration exists
    When I open the application
    And I click "Členské příspěvky"
    And I click "Uzavřít kampaň"
    And I click "Odeslat"
    Then the page does not show section "Aktivní kampaň"
    And the past campaigns section contains the campaign year
    When I continue as "member"
    And I open the application
    And I click "Finance"
    Then the account shows the yearly membership fee of the scenario year
    When I continue as "admin"
    And I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}Close event"
    And I open the list row "{prefix}Close event"
    Then I see "Aktivní"
    And I do not see "Přihlásit se"

  Scenario: Close action is not available on an already closed campaign
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Processed tier"
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    Then I see "Skupiny"
    And I do not see "Uzavřít kampaň"

  Scenario: Close action requires MEMBERS:MANAGE authority
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Authority tier"
    When I open the application
    And I navigate in the application to "/membership-fee-tiers"
    Then I do not see "Uzavřít kampaň"
