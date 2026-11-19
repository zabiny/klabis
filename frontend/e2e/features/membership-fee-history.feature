@spec:membership-fees
# Spec scenarios intentionally not automated (covered by backend and frontend unit tests):
# - "History entry shows year, type, price and how it was assigned" (the own choice and the price are checked in the first scenario)
# - "Type assigned by an administrator is marked as such"
# - "Year without an assignment is not listed", "Fee page of an inactive member is not offered"
# - "Member without any assignment sees an empty history", "Returning from the fee page"
Feature: Member fee type history
  The "Členské příspěvky" page opened from a member's detail lists the fee types the member had in past years.
  Only the member, a membership administrator and a legal guardian of the member can open it.
  Jan is a membership administrator. Eva is a club member and the legal guardian of the minor member Sofie Svobodová.

  Rule: Members, administrators and legal guardians of the member can open the fee type history

    Scenario: Member opens their fee type history from the profile
      Given an active fee selection campaign offers type "Basic" with yearly fee 1500 CZK
      And Eva has chosen type "Basic"
      When Eva opens the fee type history from the profile
      Then the history shows type "Basic" for the campaign year as the member's own choice
      And the history shows the price 1500 CZK for the campaign year

    Scenario: History keeps the published price after the catalog price changes
      Given an active fee selection campaign offers type "Basic" with yearly fee 1500 CZK
      And Eva has chosen type "Basic"
      And the fee selection campaign is closed
      And the catalog price of type "Basic" is changed to 1800 CZK
      When Eva opens the fee type history from the profile
      Then the history shows the price 1500 CZK for the campaign year

    Scenario: Legal guardian sees the fee type history of their minor
      Given an active fee selection campaign offers type "Basic"
      And Jan has assigned member "Sofie Svobodová" to type "Basic"
      And the fee selection campaign is closed
      When Eva opens the fee type history of member "Sofie Svobodová"
      Then the history shows type "Basic" for the campaign year

  Rule: Other members cannot open the fee type history

    Scenario: Member cannot open another member's fee page
      When Eva opens the detail of club member "Tomáš Král"
      Then Eva is not offered the fee type history of member "Tomáš Král"

    Scenario: Legal guardian cannot open the fee page of a member who is not their minor
      When Eva opens the detail of club member "Petra Nováčková"
      Then Eva is not offered the fee type history of member "Petra Nováčková"

  Rule: Administrators see the history of a member but cannot choose a type for them

    Scenario: Administrator sees only the history of another member
      Given an active fee selection campaign offers type "Basic"
      And Jan has assigned member "Sofie Svobodová" to type "Basic"
      When Jan opens the fee type history of member "Sofie Svobodová"
      Then the history shows type "Basic" for the campaign year
      And Jan does not see the fee section
