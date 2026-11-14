@spec:membership-fees
# Spec scenarios intentionally not automated (covered by backend and frontend unit tests):
# - "History entry shows year, level and how it was assigned" (only the member's own choice is checked in the first scenario)
# - "Level assigned by an administrator is marked as such"
# - "Year without an assignment is not listed", "Fee page of an inactive member is not offered"
# - "Member without any assignment sees an empty history", "Returning from the fee page"
Feature: Member fee level history
  The "Členské příspěvky" page opened from a member's detail lists the fee levels the member had in past years.
  Only the member, a membership administrator and a legal guardian of the member can open it.
  Jan is a membership administrator. Eva is a club member and the legal guardian of the minor member Sofie Svobodová.

  Rule: Members, administrators and legal guardians of the member can open the fee level history

    Scenario: Member opens their fee level history from the profile
      Given an active fee selection campaign offers level "Basic"
      And Eva has chosen level "Basic"
      When Eva opens the fee level history from the profile
      Then the history shows level "Basic" for the campaign year as the member's own choice

    Scenario: Legal guardian sees the fee level history of their minor
      Given an active fee selection campaign offers level "Basic"
      And Jan has assigned member "Sofie Svobodová" to level "Basic"
      And the fee selection campaign is closed
      When Eva opens the fee level history of member "Sofie Svobodová"
      Then the history shows level "Basic" for the campaign year

  Rule: Other members cannot open the fee level history

    Scenario: Member cannot open another member's fee page
      When Eva opens the detail of club member "Tomáš Král"
      Then Eva is not offered the fee level history of member "Tomáš Král"

    Scenario: Legal guardian cannot open the fee page of a member who is not their minor
      When Eva opens the detail of club member "Petra Nováčková"
      Then Eva is not offered the fee level history of member "Petra Nováčková"

  Rule: Administrators see the current level of a member but cannot choose it

    Scenario: Administrator sees only the history of another member
      Given an active fee selection campaign offers level "Basic"
      And Jan has assigned member "Sofie Svobodová" to level "Basic"
      When Jan opens the fee level history of member "Sofie Svobodová"
      Then Jan sees fee level "Basic" on the fee page of member "Sofie Svobodová"
      And Jan is not offered to choose or change the fee level of member "Sofie Svobodová"
