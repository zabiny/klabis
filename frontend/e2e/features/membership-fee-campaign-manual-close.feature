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
    And I navigate in the application to the scenario campaign
    Then I see "Skupiny"
    And I do not see "Uzavřít kampaň"
