@spec:membership-fees
# Spec scenario intentionally not automated:
# - "Editing is rejected after the first surcharge": needs a surcharge calculated for a finished event registration.
Feature: Membership fee selection campaigns
  A fee selection campaign publishes a snapshot of selected catalog levels for one calendar year
  and lets members choose among them until a single voting deadline. Only one campaign can be active.
  Jan is a membership administrator, Eva a club member.

  Rule: Starting a campaign publishes a complete snapshot of each selected level

    Scenario: Administrator starts a fee selection campaign
      Given a catalog fee level "Campaign" with yearly fee 1500 CZK exists
      And catalog level "Campaign" includes a 50 % rule for races of ranking "Rank"
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering level "Campaign" with the voting deadline in 30 days
      Then the published level "Campaign" has yearly fee 1500 CZK
      And the published level "Campaign" has a 50 % rule for races of ranking "Rank"
      And Eva can choose level "Campaign"

  Rule: The voting deadline of a new campaign must be in the future

    Scenario: Voting deadline must be in the future
      Given a catalog fee level "Deadline" exists
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering level "Deadline" with the voting deadline today
      Then the campaign is refused
      And the membership fees page shows no active campaign

  Rule: Only one campaign can be active at a time

    Scenario: Only one active campaign at a time
      Given an active fee selection campaign offers level "Only one"
      When Jan opens the membership fees page
      Then Jan is not offered to start a fee selection campaign

  Rule: One voting deadline governs every level of the campaign

    Scenario: Voting deadline applies to the whole campaign
      Given a catalog fee level "Basic" exists
      And a catalog fee level "Plus" exists
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering levels "Basic" and "Plus" with the voting deadline in 30 days
      Then the active campaign has the voting deadline in 30 days
      And the active campaign offers level "Basic"
      And the active campaign offers level "Plus"

  Rule: The voting deadline of an active campaign can be moved, but not into the past

    Scenario: Administrator extends the voting deadline
      Given an active fee selection campaign offers level "Extend" with the voting deadline in 30 days
      When Jan changes the voting deadline to 60 days from today
      Then the active campaign has the voting deadline 60 days from today

    Scenario: Administrator sets deadline to today
      Given an active fee selection campaign offers level "Today" with the voting deadline in 30 days
      When Jan changes the voting deadline to today
      Then the active campaign has the voting deadline today

    Scenario: Deadline cannot be moved to the past
      Given an active fee selection campaign offers level "Past" with the voting deadline in 30 days
      When Jan changes the voting deadline to yesterday
      Then the deadline change is refused
      And the active campaign has the voting deadline in 30 days

    Scenario: Closed campaign deadline cannot be changed
      Given a closed fee selection campaign that offered level "Closed"
      When Jan opens the closed campaign
      Then Jan is not offered to change the voting deadline

  Rule: A published level can be edited until the first surcharge is calculated

    Scenario: Administrator edits a published level before any surcharge
      Given an active fee selection campaign offers level "Editable" with yearly fee 1000 CZK
      When Jan changes the yearly fee of the published level "Editable" to 2500 CZK
      Then the published level "Editable" has yearly fee 2500 CZK
