@spec:users
Feature: Return to the requested page after sign-in
  A user who is not signed in and opens a page of the application sees that page once sign-in completes,
  both when the login form is filled in and when the authorization server signs the user in without it.
  Eva is a club member without administration permissions.
  Spec scenarios not automated here: search and filter settings are kept, return after session expiry,
  opening the application without a specific page, rejection of foreign targets, next sign-in after logout.

  Rule: After sign-in the user sees the originally requested page, not the home page

    Scenario: Member opens a bookmarked page and signs in on the login form
      Given event "Bookmarked" is open for registration
      And nobody is signed in
      When Eva opens the bookmarked link to event "Bookmarked"
      And Eva signs in on the login page
      Then the event detail of "Bookmarked" is shown

    Scenario: Member opens a shared link while still signed in at the login service
      Given Eva signed in earlier but the application session has ended
      When the shared link to the membership fee tiers page is opened
      Then the membership fee tiers page is shown
      And the sign-in completed without the login page
