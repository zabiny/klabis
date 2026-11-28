# Not automated here:
# - "Account is created together with a new member": the account created on member registration is asserted by `MemberAccountCreationIntegrationTest.registeringMemberResultsInFinanceAccountBeingCreated` (listener unit test `CreateAccountOnMemberRegisteredTest`); the registration journey is in `members.feature`.
# - "Overdraft limit blocks a registration payment" and "... permits a registration payment that stays within it": no event registration charges the account yet
#   (chargeForRegistration has no REST path) and the limit is a deployment setting (default -500 CZK) that a scenario cannot change.
# - "Overdraft limit does not block a finance-manager charge": `MemberAccountTest.chargeRecordsTransactionBelowOverdraftLimit` (domain rule).
# - "Member cannot view another member's account": `AccountOwnerAccessIntegrationTest.memberIsDeniedOtherAccount` (403 on account and transactions) and
#   `MemberAccountControllerTest.AccountLinkOnForeignResponses.memberDetailHasNoAccountLinkWithoutFinanceManage` (no account link on the detail), and `MemberDetailPage.test` "Účet without the link".
# - "Member cannot record any transaction on their own account": `MemberAccountControllerTest` (`shouldReturn403WithoutFinanceManage` on deposit, charge and reverse,
#   `shouldReturnAccountWithoutDepositAffordanceForRegularMember` / `...WithoutChargeAffordanceForRegularMember`, `shouldNotIncludeReverseAffordanceForRegularMember`) and
#   `MemberAccountManagePage.test` "Without FINANCE:MANAGE affordances (own account view)" (no "Vložit / Vybrat", no reverse action).
# - "Guardian cannot record any transaction on the child's account": `GuardianAccountAccessIntegrationTest` (read-only minor account: no deposit, charge or reverse template, for member and non-member guardian) and
#   `MemberAccountManagePage.test` "Guardian view of a minor account (read-only)".
# - "Member guardian opens Finance and sees their own account first": `MemberAccountControllerTest.ViewableAccountLinks.shouldListOwnThenChildrenOnOwnAccount` and
#   `MemberAccountManagePage.test` "Account switcher" "lists options in link order with the current account selected".
# - "Non-member guardian switches to the other child's account": `MemberAccountManagePage.test` "Account switcher" "navigates to the chosen account" and
#   `MemberAccountControllerTest.ViewableAccountLinks.shouldListChildrenByNameForExtGuardian`; the switch itself stays journey-tested by "Member guardian switches to a child's account".
# - "Deposit with non-positive amount is rejected" and "Charge with non-positive amount is rejected": `MemberAccountTest` (`depositWithZeroAmountThrows`, `depositWithNegativeAmountThrows`, `chargeWithZeroAmountThrows`, `chargeWithNegativeAmountThrows`),
#   `MemberAccountControllerTest` (`shouldMapIllegalArgumentFromDepositPortTo400`, `shouldMapIllegalArgumentFromChargePortTo400`) and `FinanceTransactionDialog.test` "amount validation" (deposit tab; the check is shared by both tabs: confirm stays disabled for empty, zero and negative amounts).
@spec:member-accounts
Feature: Member financial accounts
  Every member has a prepaid financial account in CZK. Members see their own account,
  finance managers record deposits and charges and reverse transactions on any account.
  Jan is a finance manager, Eva is a club member and the legal guardian of the minor member Sofie Svobodová,
  Ivana is not a member, she is the legal guardian of the minor members Adam Dlouhý and Klára Dlouhá.

  Rule: A member sees their own account and history

    Scenario: Member opens "Finance" from the main menu
      When Eva opens Finance from the main menu
      Then Eva sees the balance and the transaction history

  Rule: A legal guardian views their minor child's account but cannot change it

    Scenario: Guardian opens their child's account from the child's detail page
      When Eva opens the account of the minor member "Sofie Svobodová" from the minor's detail
      Then the account of member "Sofie Svobodová" is shown with the owner's name and registration number
      And Eva sees the balance and the transaction history

  Rule: The Finance page lets a user switch between the accounts they may view

    Scenario: Member guardian switches to a child's account
      Given Eva has Finance open
      When Eva switches to the account of "Sofie Svobodová"
      Then the account of member "Sofie Svobodová" is shown with the owner's name and registration number
      And Eva sees the balance and the transaction history

    Scenario: Non-member guardian opens Finance
      When Ivana opens Finance from the main menu
      Then the account of member "Klára Dlouhá" is shown with the owner's name and registration number
      And the account switcher offers the accounts of "Klára Dlouhá" and "Adam Dlouhý"

    Scenario: Member without minor children sees no account switcher
      When Tomáš opens Finance from the main menu
      Then Tomáš sees no account switcher

  Rule: A finance manager records a deposit that increases the balance

    Scenario: Finance manager records a deposit
      Given member "Horak" is registered
      When Jan records a deposit of 300 CZK for member "Horak"
      Then the account of member "Horak" shows the balance 300 CZK
      And the history of member "Horak" lists the deposit of 300 CZK

  Rule: A finance manager records a charge that decreases the balance

    Scenario: Finance manager records a charge
      Given member "Benes" is registered
      And the account of member "Benes" holds 500 CZK
      When Jan records a charge of 200 CZK for member "Benes"
      Then the account of member "Benes" shows the balance 300 CZK
      And the history of member "Benes" lists the charge of 200 CZK

  Rule: A reversal appends an opposite transaction and keeps the original in the history

    Scenario: Reversing a deposit decreases balance back
      Given member "Marek" is registered
      And the account of member "Marek" holds 300 CZK
      When Jan reverses the deposit of 300 CZK on the account of member "Marek"
      Then the account of member "Marek" shows the balance 0 CZK
      And the history of member "Marek" marks the reversal of the deposit of 300 CZK
