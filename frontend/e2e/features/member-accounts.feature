@spec:member-accounts
Feature: Member financial accounts
  Every member has a prepaid financial account in CZK. Members see their own account,
  finance managers record deposits and charges and reverse transactions on any account.
  Jan is a finance manager, Eva is a club member.

  Rule: A member sees their own account and history

    Scenario: Member opens "Finance" from the main menu
      When Eva opens Finance from the main menu
      Then Eva sees the balance and the transaction history

  Rule: A finance manager records a deposit that increases the balance

    Scenario: Finance manager records a deposit
      Given member "Horak" is registered
      When Jan records a deposit of 300 CZK for member "Horak"
      Then the account of member "Horak" shows the balance 300 CZK
      And the history of member "Horak" lists the transaction of 300 CZK

  Rule: A finance manager records a charge that decreases the balance

    Scenario: Finance manager records a charge
      Given member "Benes" is registered
      And the account of member "Benes" holds 500 CZK
      When Jan records a charge of 200 CZK for member "Benes"
      Then the account of member "Benes" shows the balance 300 CZK
      And the history of member "Benes" lists the transaction of 200 CZK

  Rule: A reversal appends an opposite transaction and keeps the original in the history

    Scenario: Reversing a deposit decreases balance back
      Given member "Marek" is registered
      And the account of member "Marek" holds 300 CZK
      When Jan reverses the deposit of 300 CZK on the account of member "Marek"
      Then the account of member "Marek" shows the balance 0 CZK
      And the history of member "Marek" marks the reversal of the deposit of 300 CZK
