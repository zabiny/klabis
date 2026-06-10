## 1. Odstranit redundantní `uuid()` accessor z ID typů

- [ ] 1.1 Odstranit metodu `uuid()` z `MembershipFeeLevelId`, `MembershipFeeGroupId` a `FeeYearPublicationId`
- [ ] 1.2 Sjednotit všechny callery v modulu na `.value()` (grep po `uuid()` v celém membership fees modulu)
- [ ] 1.3 Ověřit že testy prochází

## 2. Injektovat `Clock` do application services

- [ ] 2.1 Přidat `Clock` field do `MemberChoiceService` a `MemberFeeHistoryService` (konstruktor injection)
- [ ] 2.2 Nahradit `LocalDate.now()` za `LocalDate.now(clock)` ve všech call sites obou service tříd
- [ ] 2.3 Napsat unit test pro `MemberChoiceService` ověřující deadline boundary chování pomocí fixního `Clock`
- [ ] 2.4 Ověřit že stávající testy prochází

## 3. Odstranit cross-service coupling v `FeeYearPublicationManagementService`

- [ ] 3.1 Injektovat `MembershipFeeLevelRepository` místo `MembershipFeeLevelManagementPort` do `FeeYearPublicationManagementService`
- [ ] 3.2 Nahradit volání `levelManagementPort.getLevel()` přímým voláním repository
- [ ] 3.3 Ověřit že testy prochází (zejména `FeeYearPublicationManagementServiceTest`)

## 4. Deduplikovat `resolveRecommendedLevel`

- [ ] 4.1 V `MemberFeeHistoryService` injektovat `MemberChoicePort`
- [ ] 4.2 Nahradit privátní `resolveRecommendedLevel` delegací na `MemberChoicePort.getRecommendedLevelForYear()`
- [ ] 4.3 Odstranit nyní nepoužívanou privátní metodu z `MemberFeeHistoryService`
- [ ] 4.4 Ověřit že testy prochází

## 5. Odstranit `MembershipPaymentRuleSnapshot` wrapper typ

- [ ] 5.1 Změnit `MembershipFeeGroup.rulesSnapshot` z `List<MembershipPaymentRuleSnapshot>` na `List<MembershipPaymentRule>`
- [ ] 5.2 Odstranit `MembershipPaymentRuleSnapshot.from(rule)` volání — nahradit přímým použitím `MembershipPaymentRule`
- [ ] 5.3 Upravit `MembershipPaymentRuleSnapshotMemento` aby mapoval `MembershipPaymentRule` (místo `MembershipPaymentRuleSnapshot`) na tabulku `membership_fee_group_rule_snapshot`
- [ ] 5.4 Upravit `MembershipFeeGroupMemento` a `MembershipFeeGroupRepositoryAdapter` pro nový typ `rulesSnapshot`
- [ ] 5.5 Sjednotit duplicitní response record v REST API vrstvě (odstranit `RuleSnapshotResponse`, použít sdílený `PaymentRuleResponse`)
- [ ] 5.6 Odstranit třídu `MembershipPaymentRuleSnapshot` z domain vrstvy
- [ ] 5.7 Ověřit že testy prochází včetně persistence testů

## 6. Přesunout deadline enforcement do agregátu

- [ ] 6.1 Napsat failing test: `MembershipFeeGroup.addMember()` vyhodí `VotingClosedException` po uplynutí `votingDeadline` (TDD red)
- [ ] 6.2 Přidat `LocalDate votingDeadline` field do `MembershipFeeGroup` — nastavit při `publish()` / `reconstruct()`
- [ ] 6.3 Implementovat datum-based guard v `MembershipFeeGroup.addMember()` a `removeMember()` (TDD green)
- [ ] 6.4 Odstranit service-level `isClosed()` check z `MemberChoiceService.chooseFeeLevel()` a `removeFeeChoice()`
- [ ] 6.5 Upravit `MembershipFeeGroupMemento` a `MembershipFeeGroupRepositoryAdapter` aby persistovaly `votingDeadline`
- [ ] 6.6 Napsat DB migraci přidávající sloupec `voting_deadline` do tabulky skupin (pokud chybí)
- [ ] 6.7 Ověřit plný test suite — zejména že status `FROZEN` stále slouží pro UI a scheduler ho nastavuje nezměněně
