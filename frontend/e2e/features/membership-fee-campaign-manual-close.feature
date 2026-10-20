@spec:membership-fee-campaign-manual-close
Feature: Membership fee campaign manual close
  A membership administrator can close an active fee selection campaign before its voting deadline.
  Closing runs the same processing as the end of the deadline: yearly fees are posted and members
  without a choice are sanctioned.
  Jan is a membership administrator, Eva a club member without administration permissions.

  Rule: Only a membership administrator can close a campaign, and only while it is active

    Scenario: Administrator closes an active campaign
      Given an active fee selection campaign offers level "Close"
      When Jan opens the active campaign
      Then Jan is offered to close the campaign

    Scenario: Campaign is closed immediately
      Given an active fee selection campaign offers level "Plus" with yearly fee 1200 CZK
      And Eva has chosen level "Plus"
      When Jan closes the active campaign
      Then the membership fees page shows no active campaign
      And the membership fees page lists the closed campaign among the past campaigns
      And Eva's account shows the yearly membership fee of 1200 CZK for the campaign year

    Scenario: Close action is not available on an already closed campaign
      Given a closed fee selection campaign that offered level "Processed"
      When Jan opens the closed campaign
      Then Jan is not offered to close the campaign

    Scenario: Close action requires MEMBERS:MANAGE authority
      Given an active fee selection campaign offers level "Authority"
      When Eva opens the active campaign
      Then Eva is not offered to close the campaign
