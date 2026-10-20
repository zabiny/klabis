@spec:membership-fees
Feature: Membership fees administration page
  A single page is the membership administrator's entry point: it shows the level catalog,
  the active campaign inline and the list of past campaigns.
  Jan is a membership administrator, Eva a club member without administration permissions.

  Rule: One page holds the catalog, the active campaign and the past campaigns, for administrators only

    Scenario: Administrator opens the membership fees page
      Given a closed fee selection campaign that offered level "Old"
      And an active fee selection campaign offers level "Current"
      When Jan opens the membership fees page
      Then the membership fees page lists catalog level "Old"
      And the membership fees page shows the active campaign of the campaign year
      And the membership fees page lists the closed campaign among the past campaigns

    Scenario: Non-administrator cannot reach the membership fees administration page
      Given an active fee selection campaign offers level "Hidden"
      When Eva opens the membership fees page
      Then the membership fees page shows no active campaign
      And the membership fees page shows no past campaigns

  Rule: The active campaign is shown inline with its deadline, actions and levels

    Scenario: Active campaign is shown inline
      Given an active fee selection campaign offers level "Inline" with the voting deadline in 30 days
      When Jan opens the membership fees page
      Then the membership fees page shows the active campaign of the campaign year
      And the active campaign has the voting deadline in 30 days
      And Jan is offered to change the voting deadline
      And Jan is offered to close the campaign
      And Jan can open the published level "Inline"

    Scenario: No active campaign hides the section
      Given a closed fee selection campaign that offered level "Only past"
      When Jan opens the membership fees page
      Then the membership fees page shows no active campaign

  Rule: Past campaigns are listed without the active one, each with a navigable detail

    Scenario: Past campaigns are listed and navigable
      Given a closed fee selection campaign that offered level "Navigable"
      When Jan opens the membership fees page
      Then the membership fees page lists the closed campaign among the past campaigns
      And Jan can open the closed campaign

    Scenario: Active campaign is not duplicated in the past list
      Given an active fee selection campaign offers level "Not duplicated"
      When Jan opens the membership fees page
      Then the membership fees page does not list the active campaign among the past campaigns

    Scenario: Returning from a campaign detail goes back to the membership fees page
      Given a closed fee selection campaign that offered level "Back"
      And Jan is viewing the closed campaign
      When Jan navigates back
      Then the membership fees page is shown
