@spec:membership-fees
# Spec scenarios intentionally not automated (covered by backend and frontend unit tests):
# - "Section is locked after the voting deadline", "Member who missed the deadline is told the voting is closed",
#   "Member cannot withdraw the choice after the deadline": a campaign cannot be published with a deadline in the past.
# - "Non-member legal guardian chooses a type for their minor": the seeded non-member guardians cannot sign in.
Feature: Member's fee type choice
  Members choose one of the published fee types in the "Členský příspěvek" section of their "Členské příspěvky" page
  while a campaign is active and see the yearly price of every offered type.
  Eva is a club member and the legal guardian of the minor member Sofie Svobodová, Jan a membership administrator.

  Rule: Until the deadline a member can choose, change or withdraw the type, also one assigned by an administrator

    Scenario: Member chooses a type
      Given an active fee selection campaign offers types "Basic" and "Plus"
      When Eva chooses type "Plus"
      Then Eva's fee section shows fee type "Plus"

    Scenario: Member changes the choice before the deadline
      Given an active fee selection campaign offers types "Basic" and "Plus"
      And Eva has chosen type "Basic"
      When Eva changes the fee type choice to "Plus"
      Then Eva's fee section shows fee type "Plus"

    Scenario: Member withdraws the choice before the deadline
      Given an active fee selection campaign offers types "Basic" and "Plus"
      And Eva has chosen type "Basic"
      When Eva withdraws the fee type choice
      Then Eva's fee section prompts to choose a fee type

    Scenario: Member withdraws a type assigned by an administrator
      Given an active fee selection campaign offers types "Basic" and "Plus"
      And Jan has assigned Eva to type "Basic"
      When Eva withdraws the fee type choice
      Then Eva's fee section prompts to choose a fee type

  Rule: Every offered type shows its yearly price

    Scenario: Offered types show their yearly price
      Given an active fee selection campaign offers type "Plus" with yearly fee 2000 CZK
      When Eva opens the fee page
      Then Eva's fee section offers type "Plus" for 2000 CZK

  Rule: The previous year's type is offered as a non-binding default

    Scenario: Previous year's type is offered as a default
      Given Eva had type "Plus" in the previous fee selection campaign
      And an active fee selection campaign offers types "Basic" and "Plus"
      When Eva opens the fee page
      Then type "Plus" is pre-selected
      And Eva's fee section prompts to choose a fee type

  Rule: The section is shown only during an active campaign

    Scenario: Member with no chosen type is prompted to choose
      Given an active fee selection campaign offers type "Basic"
      When Eva opens the fee page
      Then Eva's fee section prompts to choose a fee type

    Scenario: Section is hidden when no campaign is active
      Given no fee selection campaign is active
      When Eva opens the fee page
      Then Eva does not see the fee section

    Scenario: Section is hidden after the campaign is closed and the history keeps the type
      Given an active fee selection campaign offers types "Basic" and "Plus"
      And Eva has chosen type "Plus"
      And the fee selection campaign is closed
      When Eva opens the fee page
      Then Eva does not see the fee section
      And the history shows type "Plus" for the campaign year

  Rule: The legal guardian of a minor can choose the minor's type on the minor's behalf

    Scenario: Legal guardian chooses a type for their minor
      Given an active fee selection campaign offers types "Basic" and "Plus"
      When Eva chooses type "Plus" for member "Sofie Svobodová"
      Then Eva sees fee type "Plus" in the fee section of member "Sofie Svobodová"
