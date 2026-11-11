@spec:legal-guardians
Feature: Registering a non-member legal guardian as a member
  A non-member legal guardian becomes a club member when a member administrator registers them from their profile.
  The guardian is taken over: they keep their account and their minors, get a registration number.
  Jan is a member administrator.
  Spec scenarios not automated here: pre-filled registration form, change of pre-filled data, cancelling,
  the action missing on the guardian's own profile,
  login with the former login number (backend test OidcAltUsernameLoginTest).

  Rule: A user with member management permission registers a non-member guardian as a member from the guardian's profile

    Scenario: Admin lands on the new member's detail after submitting
      Given non-member guardian "Rodic" of minor "Dite" exists
      When Jan registers guardian "Rodic" as a member from the guardian's profile
      Then the detail of member "Rodic" shows a new registration number
      And the detail of member "Dite" lists "Rodic" as legal guardian
