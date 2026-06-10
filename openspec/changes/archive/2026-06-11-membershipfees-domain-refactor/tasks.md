## 1. Odstranit redundantní `uuid()` accessor z ID typů

- [x] 1.1 Odstranit metodu `uuid()` z `MembershipFeeLevelId`, `MembershipFeeGroupId` a `FeeYearPublicationId`
- [x] 1.2 Sjednotit všechny callery v modulu na `.value()` (grep po `uuid()` v celém membership fees modulu)
- [x] 1.3 Ověřit že testy prochází

## 2. Injektovat `Clock` do application services

- [x] 2.1 Přidat `Clock` field do `MemberChoiceService` a `MemberFeeHistoryService` (konstruktor injection)
- [x] 2.2 Nahradit `LocalDate.now()` za `LocalDate.now(clock)` ve všech call sites obou service tříd
- [x] 2.3 Napsat unit test pro `MemberChoiceService` ověřující deadline boundary chování pomocí fixního `Clock`
- [x] 2.4 Ověřit že stávající testy prochází

## 3. Odstranit cross-service coupling v `FeeYearPublicationManagementService`

- [x] 3.1 Injektovat `MembershipFeeLevelRepository` místo `MembershipFeeLevelManagementPort` do `FeeYearPublicationManagementService`
- [x] 3.2 Nahradit volání `levelManagementPort.getLevel()` přímým voláním repository
- [x] 3.3 Ověřit že testy prochází (zejména `FeeYearPublicationManagementServiceTest`)

## 4. Deduplikovat `resolveRecommendedLevel`

- [x] 4.1 V `MemberFeeHistoryService` injektovat `MemberChoicePort`
- [x] 4.2 Nahradit privátní `resolveRecommendedLevel` delegací na `MemberChoicePort.getRecommendedLevelForYear()`
- [x] 4.3 Odstranit nyní nepoužívanou privátní metodu z `MemberFeeHistoryService`
- [x] 4.4 Ověřit že testy prochází

## 5. Odstranit `MembershipPaymentRuleSnapshot` wrapper typ

- [x] 5.1 Změnit `MembershipFeeGroup.rulesSnapshot` z `List<MembershipPaymentRuleSnapshot>` na `List<MembershipPaymentRule>`
- [x] 5.2 Odstranit `MembershipPaymentRuleSnapshot.from(rule)` volání — nahradit přímým použitím `MembershipPaymentRule`
- [x] 5.3 Upravit `MembershipPaymentRuleSnapshotMemento` aby mapoval `MembershipPaymentRule` (místo `MembershipPaymentRuleSnapshot`) na tabulku `membership_fee_group_rule_snapshot`
- [x] 5.4 Upravit `MembershipFeeGroupMemento` a `MembershipFeeGroupRepositoryAdapter` pro nový typ `rulesSnapshot`
- [x] 5.5 Sjednotit duplicitní response record v REST API vrstvě (odstranit `RuleSnapshotResponse`, použít sdílený `PaymentRuleResponse`)
- [x] 5.6 Odstranit třídu `MembershipPaymentRuleSnapshot` z domain vrstvy
- [x] 5.7 Ověřit že testy prochází včetně persistence testů

## 6. Přesunout deadline enforcement do agregátu

- [x] 6.1 Napsat failing test: `MembershipFeeGroup.addMember()` vyhodí `VotingClosedException` po uplynutí `votingDeadline` (TDD red)
- [x] 6.2 Přidat `LocalDate votingDeadline` field do `MembershipFeeGroup` — nastavit při `publish()` / `reconstruct()`
- [x] 6.3 Implementovat datum-based guard v `MembershipFeeGroup.addMember()` a `removeMember()` (TDD green)
- [x] 6.4 Odstranit service-level `isClosed()` check z `MemberChoiceService.chooseFeeLevel()` a `removeFeeChoice()`
- [x] 6.5 Upravit `MembershipFeeGroupMemento` a `MembershipFeeGroupRepositoryAdapter` aby persistovaly `votingDeadline`
- [x] 6.6 Napsat DB migraci přidávající sloupec `voting_deadline` do tabulky skupin (pokud chybí)
- [x] 6.7 Ověřit plný test suite — zejména že status `FROZEN` stále slouží pro UI a scheduler ho nastavuje nezměněně
