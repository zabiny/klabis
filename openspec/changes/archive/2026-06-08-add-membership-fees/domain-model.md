## Domain Model — membership-fees

**Legenda:**
- `[N]` — nový prvek zaváděný tímto changem (`membership-fees`)
- `[X]` — externí reference z jiných modulů (`finance`, `members`, `events`)

```mermaid
classDiagram
    direction TB

    namespace membership-fees {
        class MembershipFeeLevel["MembershipFeeLevel [N]"] {
            MembershipFeeLevelId id
            String name
            Money yearlyFee
            create(name, yearlyFee, rules)
            editName(name)
            editYearlyFee(fee)
            addRule(rule)
            replaceRules(rules)
        }
        class MembershipPaymentRule["MembershipPaymentRule [N]"] {
            EventTypeId eventTypeId
            String ranking
            RuleValue value
        }
        class RuleValue["RuleValue [N]"] {
            <<value object>>
            PERCENTAGE : int percent
            FIXED_SURCHARGE : Money amount
        }
        class FeeYearPublication["FeeYearPublication [N]"] {
            FeeYearPublicationId id
            int year
            LocalDate votingDeadline
            Instant deadlineProcessedAt
            publish(year, deadline, levels)
            isClosed(today) bool
            markProcessed(at)
        }
        class MembershipFeeGroup["MembershipFeeGroup [N]"] {
            MembershipFeeGroupId id
            MembershipFeeLevelId sourceLevelId
            int year
            Money yearlyFeeSnapshot
            PublishedLevelStatus status
            editSnapshot(yearlyFee, rules)
            freeze()
            memberCount() int
        }
        class PublishedLevelStatus["PublishedLevelStatus [N]"] {
            <<enumeration>>
            EDITABLE
            FROZEN
        }
        class MembershipPaymentRuleSnapshot["MembershipPaymentRuleSnapshot [N]"] {
            EventTypeId eventTypeId
            String ranking
            RuleValue value
        }
        class AssignmentSource["AssignmentSource [N]"] {
            <<enumeration>>
            MEMBER_CHOICE
            ADMIN_ASSIGNMENT
        }
        class MemberMissedFeeSelectionEvent["MemberMissedFeeSelectionEvent [N]"] {
            <<domain event>>
            MemberId memberId
            int year
        }
        class MemberFeeSelectionResolvedEvent["MemberFeeSelectionResolvedEvent [N]"] {
            <<domain event>>
            MemberId memberId
            int year
        }
        class FeeGroupMembership["FeeGroupMembership [N]"] {
            <<value object>>
            MemberId memberId
            LocalDate joinedAt
            AssignmentSource source
            MemberId assignedBy
        }
    }

    namespace finance {
        class ChargePort["ChargePort [X]"] {
            <<port>>
            charge(memberId, amount, description)
        }
    }

    namespace members {
        class MemberId["MemberId [X]"] {
            <<value object>>
        }
        class AllMembersPort["AllMembersPort [X]"] {
            <<port>>
            findAll() Set~MemberId~
        }
    }

    namespace events {
        class EventTypeId["EventTypeId [X]"] {
            <<value object>>
        }
    }

    %% ─── membership-fees internal relations ─────────────────────────────────
    MembershipFeeLevel "1" *-- "0..*" MembershipPaymentRule : rules
    MembershipPaymentRule --> RuleValue : value
    FeeYearPublication "1" --> "1..*" MembershipFeeGroup : publishedLevelIds (ref)
    MembershipFeeGroup --> PublishedLevelStatus : status
    MembershipFeeGroup "1" *-- "0..*" MembershipPaymentRuleSnapshot : rulesSnapshot
    MembershipFeeGroup --> MembershipFeeLevel : sourceLevelId (ref)
    MembershipFeeGroup "1" *-- "0..*" FeeGroupMembership : memberships
    FeeGroupMembership --> AssignmentSource : source
    MembershipPaymentRuleSnapshot --> RuleValue : value

    %% ─── domain events ───────────────────────────────────────────────────────
    MembershipFeeGroup ..> MemberMissedFeeSelectionEvent : publishes (D5)
    MembershipFeeGroup ..> MemberFeeSelectionResolvedEvent : publishes (D9)

    %% ─── cross-module references ─────────────────────────────────────────────
    FeeYearPublication ..> ChargePort : yearly fee via scheduler (D6)
    FeeYearPublication ..> AllMembersPort : find members without choice (D5)
    FeeGroupMembership --> MemberId
    MembershipPaymentRule --> EventTypeId
    MembershipPaymentRuleSnapshot --> EventTypeId
```

### Poznámky k modelu

| Prvek | Modul | Poznámka |
|---|---|---|
| `MembershipFeeLevel` | `membership-fees` | Aggregate root — katalog šablon, editovatelný kdykoli |
| `MembershipPaymentRule` | `membership-fees` | Value object uvnitř šablony; klíč `(EventTypeId, ranking)` musí být unikátní |
| `FeeYearPublication` | `membership-fees` | Aggregate root — vypsání úrovní pro rok + jedna uzávěrka voleb |
| `MembershipFeeGroup` | `membership-fees` | Aggregate root — snapshot úrovně + členství; analogie `traininggroup`/`familygroup`/`freegroup` |
| `MembershipPaymentRuleSnapshot` | `membership-fees` | Kopie pravidel uložená při vypsání; nezávislá na katalogu |
| `FeeGroupMembership` | `membership-fees` | Value object záznamu členství; drží `AssignmentSource` pro rozlišení vlastní volby vs. nouzového přiřazení |
| `ChargePort` | `finance` | Port pro zaúčtování ročního poplatku; volán schedulerem den po uzávěrce |
