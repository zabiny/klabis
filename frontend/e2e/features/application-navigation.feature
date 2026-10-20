@spec:application-navigation
Feature: Application navigation
  The main menu is derived from the destinations the signed-in user is authorized for.
  Jan administers the club, Eva is a club member without administration permissions.

  Rule: The main menu offers exactly the destinations the user is authorized for

    Scenario: User sees only menu items they are authorized for
      When Eva opens the application
      Then the main menu offers:
        | Kalendář |
        | Akce     |
        | Finance  |
        | Skupiny  |
        | Členové  |
      And the main menu does not offer:
        | Přehled administrace |
        | Synchronizace        |

    Scenario: Administrator also sees the administration destinations
      When Jan opens the application
      Then the main menu offers:
        | Kalendář             |
        | Akce                 |
        | Finance              |
        | Skupiny              |
        | Členové              |
        | Přehled administrace |
        | Synchronizace        |
