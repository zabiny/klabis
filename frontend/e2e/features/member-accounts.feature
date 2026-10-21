# Not automated here:
# - "Overdraft limit blocks a registration payment" and "... permits a registration payment that stays within it": no event registration charges the account yet
#   (chargeForRegistration has no REST path) and the limit is a deployment setting (default -500 CZK) that a scenario cannot change.
# - "Overdraft limit does not block a finance-manager charge" runs against that default limit of -500 CZK.
@spec:member-accounts
Feature: Member financial accounts
  Every member has a prepaid financial account in CZK. Members see their own account,
  finance managers record deposits and charges and reverse transactions on any account.
  Jan is a finance manager, Eva is a club member.

  Rule: Every member has a financial account that is created together with the member

    Scenario: Account is created together with a new member
      When Jan registers a minor member with last name "Mlady" and a new legal guardian "Zakonny"
      Then the account of member "Mlady" shows the balance 0 CZK

  Rule: A member sees their own account and history

    Scenario: Member opens "Finance" from the main menu
      When Eva opens Finance from the main menu
      Then Eva sees the balance and the transaction history

    Scenario: Member cannot view another member's account
      Given member "Cizi" is registered
      When Eva opens the detail of member "Cizi"
      Then Eva is not offered to open the account of member "Cizi"

    Scenario: Member cannot record any transaction on their own account
      Given a deposit of 100 CZK is recorded on the account of Eva
      When Eva opens Finance from the main menu
      Then Eva is not offered to record or reverse transactions

  Rule: A finance manager records a deposit that increases the balance

    Scenario: Finance manager records a deposit
      Given member "Horak" is registered
      When Jan records a deposit of 300 CZK for member "Horak"
      Then the account of member "Horak" shows the balance 300 CZK
      And the history of member "Horak" lists the transaction of 300 CZK

    Scenario: Deposit with non-positive amount is rejected
      Given member "Nulovy" is registered
      When Jan attempts to record a deposit of 0 CZK for member "Nulovy"
      Then the transaction is refused
      And the account of member "Nulovy" shows the balance 0 CZK

  Rule: A finance manager records a charge that decreases the balance

    Scenario: Finance manager records a charge
      Given member "Benes" is registered
      And the account of member "Benes" holds 500 CZK
      When Jan records a charge of 200 CZK for member "Benes"
      Then the account of member "Benes" shows the balance 300 CZK
      And the history of member "Benes" lists the transaction of 200 CZK

    Scenario: Charge with non-positive amount is rejected
      Given member "Nulty" is registered
      And the account of member "Nulty" holds 100 CZK
      When Jan attempts to record a charge of 0 CZK for member "Nulty"
      Then the transaction is refused
      And the account of member "Nulty" shows the balance 100 CZK

  Rule: A finance manager charge is not constrained by the overdraft limit

    Scenario: Overdraft limit does not block a finance-manager charge
      Given member "Prekroceny" is registered
      And the account of member "Prekroceny" is overdrawn by 400 CZK
      When Jan records a charge of 200 CZK for member "Prekroceny"
      Then the account of member "Prekroceny" shows the balance -600 CZK

  Rule: A reversal appends an opposite transaction and keeps the original in the history

    Scenario: Reversing a deposit decreases balance back
      Given member "Marek" is registered
      And the account of member "Marek" holds 300 CZK
      When Jan reverses the deposit of 300 CZK on the account of member "Marek"
      Then the account of member "Marek" shows the balance 0 CZK
      And the history of member "Marek" marks the reversal of the deposit of 300 CZK
