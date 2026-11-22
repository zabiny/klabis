@spec:application-navigation
Feature: Application navigation
  The main menu is derived from the destinations the signed-in user is authorized for.
  Jan administers the club, Eva is a club member without administration permissions,
  Ivana is a legal guardian who is not a club member.

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
        | Nastavení akcí       |

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

    Scenario: Non-member guardian sees the Finance entry
      When Ivana opens the application
      Then the main menu offers:
        | Finance |

  Rule: Event settings are offered as one menu entry for event managers

    Scenario: Event manager sees one event settings entry
      When Jan opens the application
      Then the main menu offers:
        | Nastavení akcí |
      And the main menu does not offer:
        | Disciplíny        |
        | Typy akcí         |
        | Šablony kategorií |

    Scenario: Entry opens the event settings page
      When Jan opens "Nastavení akcí" from the main menu
      Then the event settings page is shown
