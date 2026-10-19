@spec:membership-fees
# Spec scenarios intentionally not automated:
# - "Editing is rejected after the first surcharge": needs a surcharge calculated for a finished event registration.
# - "Yearly fee is not posted twice": needs the fee generation job to run a second time, which only a clock-driven job triggers.
Feature: Membership fees

  Scenario: Administrator creates a fee level
    Given I am logged in as "admin"
    When I open the application
    And I click "Členské příspěvky"
    And I click "Přidat tier"
    And I fill "Název" with "{prefix}Created tier"
    And I fill "Roční poplatek" with "1800"
    And I click "Odeslat"
    Then I see "{prefix}Created tier"
    And I see "1800"

  Scenario: Administrator edits a catalog level
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Edit tier"
    When I open the application
    And I click "Členské příspěvky"
    And I open the catalog tier "{prefix}Edit tier"
    And I click "Upravit"
    And I fill "Název" with "{prefix}Edited tier"
    And I fill "Roční poplatek" with "1234"
    And I click "Uložit změny"
    Then I see "{prefix}Edited tier"
    When I click "Členské příspěvky"
    And I click "{prefix}Edit tier"
    Then I see "1000 CZK"

  Scenario: Non-administrator cannot manage the catalog
    Given I am logged in as "member"
    And a catalog fee tier "{prefix}Member view tier" exists
    When I open the application
    And I navigate in the application to "/membership-fee-tiers"
    Then I see "{prefix}Member view tier"
    And I do not see "Přidat tier"
    When I open the catalog tier "{prefix}Member view tier"
    Then I see "Základní informace"
    And I do not see "Upravit"
    And I do not see "Smazat tier"

  Scenario: Administrator defines a percentage rule
    Given I am logged in as "admin"
    And a catalog fee tier "{prefix}Percent rule tier" exists
    When I open the application
    And I click "Členské příspěvky"
    And I open the catalog tier "{prefix}Percent rule tier"
    And I click "Přidat pravidlo…"
    And I select "Závod" in "Typ závodu"
    And I fill "Žebříček" with "{prefix}Rank"
    And I select "Procento" in "Typ pravidla"
    And I fill "Procento spoluúčasti (%)" with "50"
    And I click "Odeslat"
    Then I see "{prefix}Rank"
    And I see "50 %"

  Scenario: Administrator defines a fixed amount rule
    Given I am logged in as "admin"
    And a catalog fee tier "{prefix}Fixed rule tier" exists
    When I open the application
    And I click "Členské příspěvky"
    And I open the catalog tier "{prefix}Fixed rule tier"
    And I click "Přidat pravidlo…"
    And I select "Závod" in "Typ závodu"
    And I fill "Žebříček" with "{prefix}Rank"
    And I select "Fixní částka" in "Typ pravidla"
    And I fill "Fixní částka" with "300"
    And I fill "Měna fixní částky" with "CZK"
    And I click "Odeslat"
    Then I see "{prefix}Rank"
    And I see "300 CZK"

  Scenario: Rule combination is unique within a level
    Given I am logged in as "admin"
    And a catalog fee tier "{prefix}Duplicate rule tier" with a percentage rule for ranking "{prefix}Rank" exists
    When I open the application
    And I click "Členské příspěvky"
    And I open the catalog tier "{prefix}Duplicate rule tier"
    And I click "Přidat pravidlo…"
    And I select "Závod" in "Typ závodu"
    And I fill "Žebříček" with "{prefix}Rank"
    And I select "Procento" in "Typ pravidla"
    And I fill "Procento spoluúčasti (%)" with "20"
    And I click "Odeslat"
    Then I see an error message

  Scenario: Administrator starts a fee selection campaign
    Given I am logged in as "admin"
    And a catalog fee tier "{prefix}Campaign tier" exists
    And no fee selection campaign is active
    When I open the application
    And I click "Členské příspěvky"
    And I click "Vypsat kampaň"
    And I fill "Rok" with the scenario year
    And I fill "Uzávěrka hlasování" with "{date+30}"
    And I tick "{prefix}Campaign tier" in "Tiery"
    And I click "Odeslat"
    Then the page shows section "Aktivní kampaň"
    And the active campaigns section contains the campaign year
    And the active campaign lists fee group "{prefix}Campaign tier"

  Scenario: Voting deadline must be in the future
    Given I am logged in as "admin"
    And a catalog fee tier "{prefix}Deadline tier" exists
    And no fee selection campaign is active
    When I open the application
    And I click "Členské příspěvky"
    And I click "Vypsat kampaň"
    And I fill "Rok" with the scenario year
    And I fill "Uzávěrka hlasování" with "{date+0}"
    And I tick "{prefix}Deadline tier" in "Tiery"
    And I click "Odeslat"
    Then I see an error message
    And the page does not show section "Aktivní kampaň"

  Scenario: Only one active campaign at a time
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Only one tier"
    When I open the application
    And I click "Členské příspěvky"
    Then the page shows section "Aktivní kampaň"
    And I do not see "Vypsat kampaň"

  Scenario: Voting deadline applies to the whole campaign
    Given I am logged in as "admin"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    When I open the application
    And I click "Členské příspěvky"
    Then I see the date "{date+30}"
    And the active campaign lists fee group "{prefix}Basic"
    And the active campaign lists fee group "{prefix}Plus"

  Scenario: Administrator extends the voting deadline
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Extend tier"
    When I open the application
    And I click "Členské příspěvky"
    And I click "Změnit deadline"
    And I fill "Uzávěrka hlasování" with "{date+60}"
    And I click "Odeslat"
    Then I see the date "{date+60}"

  Scenario: Administrator sets deadline to today
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Today tier"
    When I open the application
    And I click "Členské příspěvky"
    And I click "Změnit deadline"
    And I fill "Uzávěrka hlasování" with "{date+0}"
    And I click "Odeslat"
    Then I see the date "{date+0}"

  Scenario: Deadline cannot be moved to the past
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Past deadline tier"
    When I open the application
    And I click "Členské příspěvky"
    And I click "Změnit deadline"
    And I fill "Uzávěrka hlasování" with "{date-1}"
    And I click "Odeslat"
    Then I see an error message
    And I do not see the date "{date-1}"

  Scenario: Closed campaign deadline cannot be changed
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Closed deadline tier"
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    Then I see "Skupiny"
    And I do not see "Změnit deadline"

  Scenario: Member chooses a level
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    When I open the application
    And I open my profile
    And I click "Zvolit tier"
    And I choose option "{prefix}Plus"
    And I click "Zvolit tier"
    And I click "← Zpět na seznam"
    Then I see "{prefix}Plus"
    And I see "Změnit tier"

  Scenario: Member changes the choice before the deadline
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Basic"
    When I open the application
    And I open my profile
    And I click "Změnit tier"
    And I choose option "{prefix}Plus"
    And I click "Změnit tier"
    And I click "← Zpět na seznam"
    Then I see "{prefix}Plus"
    And I do not see "{prefix}Basic"

  Scenario: Member cannot change the choice after the deadline
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Basic"
    And the fee selection campaign is closed
    When I open the application
    And I open my profile
    Then I see "{prefix}Basic"
    And I see "Volba je uzamčena."
    And I do not see "Změnit tier"

  Scenario: Previous year's level is offered as a default
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Plus"
    And the fee selection campaign is closed
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    When I open the application
    And I open my profile
    And I click "Zvolit tier"
    Then the option "{prefix}Plus" is selected
    And I do not see "Aktuální tier příspěvku"

  Scenario: Administrator assigns a level after the deadline
    Given I am logged in as "admin"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the fee selection campaign is closed
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I click "{prefix}Plus"
    And I click "Přiřadit člena"
    And I select "Eva Svobodová (ZBM9500)" in "Člen"
    And I fill "Rok" with the scenario year
    And I click "Odeslat"
    Then I see "ZBM9500"

  Scenario: Administrator changes a member's level after the deadline
    Given I am logged in as "admin"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the administrator has assigned the member to tier "{prefix}Basic"
    And the fee selection campaign is closed
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I click "{prefix}Plus"
    And I click "Přiřadit člena"
    And I select "Eva Svobodová (ZBM9500)" in "Člen"
    And I fill "Rok" with the scenario year
    And I click "Odeslat"
    Then I see "ZBM9500"
    When I navigate back
    And I click "{prefix}Basic"
    Then I see "Žádní členové ve skupině."
    And I do not see "ZBM9500"

  Scenario: Member who missed the choice is blocked and deregistered
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Basic"
    And the administrator has assigned the member to tier "{prefix}Basic"
    And a published event "{prefix}Sanction event" with open registration exists
    And the member is registered for event "{prefix}Sanction event"
    And the member has cancelled the tier choice
    And the fee selection campaign is closed
    When I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}Sanction event"
    And I open the list row "{prefix}Sanction event"
    Then the event registrations do not contain "Svobodová"
    And I do not see "Přihlásit se"

  Scenario: Member who chose a level is not sanctioned
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Basic"
    And the administrator has assigned the member to tier "{prefix}Basic"
    And a published event "{prefix}No sanction event" with open registration exists
    And the member is registered for event "{prefix}No sanction event"
    And the fee selection campaign is closed
    When I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}No sanction event"
    And I open the list row "{prefix}No sanction event"
    Then the event registrations contain "Svobodová"

  Scenario: Emergency assignment unblocks future registrations
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Basic"
    And a published event "{prefix}Unblock event" with open registration exists
    And the fee selection campaign is closed
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I click "{prefix}Basic"
    And I click "Přiřadit člena"
    And I select "Eva Svobodová (ZBM9500)" in "Člen"
    And I fill "Rok" with the scenario year
    And I click "Odeslat"
    Then I see "ZBM9500"
    When I continue as "member"
    And I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}Unblock event"
    And I open the list row "{prefix}Unblock event"
    Then I see "Přihlásit se"

  Scenario: Deregistered registrations are not restored automatically
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Basic"
    And the administrator has assigned the member to tier "{prefix}Basic"
    And a published event "{prefix}Restore event" with open registration exists
    And the member is registered for event "{prefix}Restore event"
    And the member has cancelled the tier choice
    And the fee selection campaign is closed
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I click "{prefix}Basic"
    And I click "Přiřadit člena"
    And I select "Eva Svobodová (ZBM9500)" in "Člen"
    And I fill "Rok" with the scenario year
    And I click "Odeslat"
    Then I see "ZBM9500"
    When I continue as "member"
    And I open the application
    And I click "Akce"
    And I fill "Hledat" with "{prefix}Restore event"
    And I open the list row "{prefix}Restore event"
    Then I see "Přihlásit se"
    And the event registrations do not contain "Svobodová"

  Scenario: Administrator edits a published level before any surcharge
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Editable level"
    When I open the application
    And I click "Členské příspěvky"
    And I click "{prefix}Editable level"
    And I click "Upravit"
    And I fill "Roční poplatek" with "2500"
    And I click "Uložit změny"
    Then I see "2500 CZK"

  Scenario: Yearly fee is posted the day after the deadline
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Plus"
    And the fee selection campaign is closed
    When I open the application
    And I click "Finance"
    Then the account shows the yearly membership fee of the scenario year

  Scenario: Member without an assigned level receives no yearly fee
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Basic"
    And the fee selection campaign is closed
    When I open the application
    And I click "Finance"
    Then the account does not show the yearly membership fee of the scenario year

  Scenario: Member sees the assigned level on the profile
    Given I am logged in as "member"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    And the member has chosen tier "{prefix}Plus"
    When I open the application
    And I open my profile
    Then I see "ČLENSKÝ PŘÍSPĚVEK"
    And I see "{prefix}Plus"

  Scenario: Member with no assigned level sees a prompt to choose
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Basic"
    When I open the application
    And I open my profile
    Then I see "Probíhá volba tieru příspěvku. Zvolte si svůj tier."

  Scenario: Past assignments remain available
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Basic"
    And the administrator has assigned the member to tier "{prefix}Basic"
    And the fee selection campaign is closed
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I click "{prefix}Basic"
    Then I see "ZBM9500"

  Scenario: Administrator opens the membership fees page
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Old tier"
    And an active fee selection campaign for tier "{prefix}Current tier"
    When I open the application
    And I click "Členské příspěvky"
    Then the page shows section "Katalog tierů"
    And I see "{prefix}Old tier"
    And the page shows section "Aktivní kampaň"
    And the page shows section "Minulé kampaně"
    And the past campaigns section contains the closed campaign year

  Scenario: Non-administrator cannot reach the membership fees administration page
    Given I am logged in as "member"
    And an active fee selection campaign for tier "{prefix}Hidden tier"
    When I open the application
    And I navigate in the application to "/membership-fee-tiers"
    Then I see "{prefix}Hidden tier"
    And I do not see "Aktivní kampaň"
    And I do not see "Minulé kampaně"

  Scenario: Active campaign is shown inline
    Given I am logged in as "admin"
    And an active fee selection campaign for tiers "{prefix}Basic" and "{prefix}Plus"
    When I open the application
    And I click "Členské příspěvky"
    Then the page shows section "Aktivní kampaň"
    And the active campaigns section contains the campaign year
    And I see the date "{date+30}"
    And I see "Změnit deadline"
    And I see "Uzavřít kampaň"
    And the active campaign lists fee group "{prefix}Basic"
    And the active campaign lists fee group "{prefix}Plus"
    When I click "{prefix}Basic"
    Then I see "Základní informace"

  Scenario: No active campaign hides the section
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Only past tier"
    When I open the application
    And I click "Členské příspěvky"
    Then the page shows section "Minulé kampaně"
    And the page does not show section "Aktivní kampaň"

  Scenario: Past campaigns are listed and navigable
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Navigable tier"
    When I open the application
    And I click "Členské příspěvky"
    Then the past campaigns section contains the closed campaign year
    When I open the closed campaign
    Then I see "{prefix}Navigable tier"

  Scenario: Active campaign is not duplicated in the past list
    Given I am logged in as "admin"
    And an active fee selection campaign for tier "{prefix}Not duplicated tier"
    When I open the application
    And I click "Členské příspěvky"
    Then the active campaigns section contains the campaign year
    And the past campaigns section does not contain the campaign year

  Scenario: Returning from a campaign detail goes back to the membership fees page
    Given I am logged in as "admin"
    And a closed fee selection campaign for tier "{prefix}Back tier"
    When I open the application
    And I click "Členské příspěvky"
    And I open the closed campaign
    And I navigate back
    Then the page shows section "Minulé kampaně"
    And the page shows section "Katalog tierů"
