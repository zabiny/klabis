@spec:membership-fees
# Spec scenario intentionally not automated:
# - "Editing is rejected after the first surcharge": needs a surcharge calculated for a finished event registration.
Feature: Membership fee selection campaigns
  A fee selection campaign publishes a snapshot of selected catalog types for one calendar year
  and lets members choose among them until a single voting deadline. Only one campaign can be active.
  Jan is a membership administrator, Eva a club member.

  Rule: Starting a campaign publishes a complete snapshot of each selected type

    Scenario: Administrator starts a fee selection campaign
      Given a catalog fee type "Campaign" with yearly fee 1500 CZK exists
      And catalog type "Campaign" includes a 50 % rule for races of ranking "Rank"
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering type "Campaign" with the voting deadline in 30 days
      Then the published type "Campaign" has yearly fee 1500 CZK
      And the published type "Campaign" has a 50 % rule for races of ranking "Rank"
      And Eva can choose type "Campaign"

  Rule: The voting deadline of a new campaign must be in the future

    Scenario: Voting deadline must be in the future
      Given a catalog fee type "Deadline" exists
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering type "Deadline" with the voting deadline today
      Then the voting deadline field shows the message "Termín pro výběr musí být v budoucnosti."
      And the membership fees page shows no active campaign

  Rule: Only one campaign can be active at a time

    Scenario: Only one active campaign at a time
      Given an active fee selection campaign offers type "Only one"
      When Jan opens the membership fees page
      Then Jan is not offered to start a fee selection campaign

  Rule: One voting deadline governs every type of the campaign

    Scenario: Voting deadline applies to the whole campaign
      Given a catalog fee type "Basic" exists
      And a catalog fee type "Plus" exists
      And no fee selection campaign is active
      When Jan starts a fee selection campaign offering types "Basic" and "Plus" with the voting deadline in 30 days
      Then the active campaign has the voting deadline in 30 days
      And the active campaign offers type "Basic"
      And the active campaign offers type "Plus"

  Rule: The voting deadline of an active campaign can be moved, but not into the past

    Scenario: Administrator extends the voting deadline
      Given an active fee selection campaign offers type "Extend" with the voting deadline in 30 days
      When Jan changes the voting deadline to 60 days from today
      Then the active campaign has the voting deadline 60 days from today

    Scenario: Administrator sets deadline to today
      Given an active fee selection campaign offers type "Today" with the voting deadline in 30 days
      When Jan changes the voting deadline to today
      Then the active campaign has the voting deadline today

    Scenario: Deadline cannot be moved to the past
      Given an active fee selection campaign offers type "Past" with the voting deadline in 30 days
      When Jan changes the voting deadline to yesterday
      Then the voting deadline field shows the message "Termín pro výběr nesmí být v minulosti."
      And the active campaign has the voting deadline in 30 days

    Scenario: Closed campaign deadline cannot be changed
      Given a closed fee selection campaign that offered type "Closed"
      When Jan opens the closed campaign
      Then Jan is not offered to change the voting deadline

  Rule: A published type can be edited until the first surcharge is calculated

    Scenario: Administrator edits a published type before any surcharge
      Given an active fee selection campaign offers type "Editable" with yearly fee 1000 CZK
      When Jan changes the yearly fee of the published type "Editable" to 2500 CZK
      Then the published type "Editable" has yearly fee 2500 CZK
