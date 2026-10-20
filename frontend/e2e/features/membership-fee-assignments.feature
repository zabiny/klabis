@spec:membership-fees
# Spec scenario intentionally not automated:
# - "Yearly fee is not posted twice": needs the fee generation job to run a second time, which only a clock-driven job triggers.
# "The day after the voting deadline" is a clock-driven trigger; the scenarios stand in for it with the campaign ending,
# which runs the same fee posting and sanctions.
Feature: Membership fee assignments
  A membership administrator can assign a member's level even after the voting deadline. When the campaign ends,
  every member assigned to a level is charged its yearly fee, and the assignments stay available for accounting.
  Jan is a membership administrator, Eva a club member.

  Rule: An administrator can assign or change a member's level after the deadline

    Scenario: Administrator assigns a level after the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And the fee selection campaign is closed
      When Jan assigns Eva to level "Plus"
      Then the published level "Plus" lists Eva as a member

    Scenario: Administrator changes a member's level after the deadline
      Given an active fee selection campaign offers levels "Basic" and "Plus"
      And Jan has assigned Eva to level "Basic"
      And the fee selection campaign is closed
      When Jan assigns Eva to level "Plus"
      Then the published level "Plus" lists Eva as a member
      And the published level "Basic" does not list Eva as a member

  Rule: Level assignments of past years are kept for accounting

    Scenario: Past assignments remain available
      Given an active fee selection campaign offers level "Basic"
      And Jan has assigned Eva to level "Basic"
      And the fee selection campaign is closed
      When Jan opens the closed campaign
      Then the published level "Basic" lists Eva as a member

  Rule: When the campaign ends, each member is charged the yearly fee of the assigned level

    Scenario: Yearly fee is posted the day after the deadline
      Given an active fee selection campaign offers level "Plus" with yearly fee 2000 CZK
      And Eva has chosen level "Plus"
      When the fee selection campaign ends
      Then Eva's account shows the yearly membership fee of 2000 CZK for the campaign year

    Scenario: Member without an assigned level receives no yearly fee
      Given an active fee selection campaign offers level "Basic"
      And Eva has not chosen a fee level
      When the fee selection campaign ends
      Then Eva's account shows no yearly membership fee for the campaign year
