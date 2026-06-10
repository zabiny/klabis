## Why

Domain vrstva membership fees modulu obsahuje několik maintainability problémů: business invarianty jsou vynucovány mimo agregát, kód je duplicitní mezi service třídami, a pomocné typy přidávají komplexitu bez přidané hodnoty. Refaktoring konsoliduje logiku do správných vrstev a odstraňuje zbytečnou duplicitu.

## What Changes

- **Deadline enforcement přesunout do agregátu:** `MembershipFeeGroup` ponese `LocalDate votingDeadline` a bude ho vynucovat přímo v `addMember`/`removeMember`. Service-level `isClosed()` check v `MemberChoiceService` se odstraní — agregát je single source of truth. Scheduler freeze status (`FROZEN`) slouží nadále jen pro UI zobrazení.
- **Odstranit duplicitní `resolveRecommendedLevel`:** `MemberFeeHistoryService` deleguje na `MemberChoicePort.getRecommendedLevelForYear()` místo aby re-implementoval stejnou query logiku.
- **Odstranit `MembershipPaymentRuleSnapshot` jako zbytečný wrapper typ:** `MembershipFeeGroup.rulesSnapshot` bude `List<MembershipPaymentRule>`. DB tabulka `membership_fee_group_rule_snapshot` zůstane beze změny, ale mapuje ji `MembershipPaymentRuleSnapshotMemento` adaptovaný na `MembershipPaymentRule`. Duplicitní response record (`RuleSnapshotResponse`) se odstraní.
- **Injektovat `java.time.Clock`:** `MemberChoiceService` a `MemberFeeHistoryService` dostanou `Clock` přes konstruktor a použijí `LocalDate.now(clock)` — stejný pattern jako scheduler. Umožňuje deterministické unit testy boundary chování.
- **Odstranit cross-service coupling v `FeeYearPublicationManagementService`:** Přímá injekce `MembershipFeeLevelRepository` místo `MembershipFeeLevelManagementPort`. Načtení levelů proběhne v jedné transakci se save publikace.
- **Odstranit redundantní `uuid()` accessor z ID typů:** `MembershipFeeLevelId`, `MembershipFeeGroupId`, `FeeYearPublicationId` — odstraní se `uuid()`, callery se sjednotí na record komponent `.value()`.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/membership-fees/spec.md` — neovlivněno; všechny scénáře (volba levelu, deadline enforcement, emergency assignment, sankce, roční poplatek) zůstávají funkčně identické. Deadline enforcement se přesune z application vrstvy do agregátu, ale uživatelsky pozorovatelné chování (zamítnutí po deadline) je stejné.

**Why no spec update is needed:**
Všechny změny jsou čistě interní: přesun logiky mezi vrstvami, odstranění duplicit, sjednocení typů a zpřístupnění závislostí pro testování. Žádná změna API kontraktu, response shape, status kódů, autorizačních pravidel ani business outcomes. Uživatel ani API consumer nemůže změny detekovat.

## Impact

- `backend/src/main/java/com/klabis/membershipfees/domain/` — změna `MembershipFeeGroup`, odstranění `MembershipPaymentRuleSnapshot`
- `backend/src/main/java/com/klabis/membershipfees/application/` — změna `MemberChoiceService`, `MemberFeeHistoryService`, `FeeYearPublicationManagementService`
- `backend/src/main/java/com/klabis/membershipfees/infrastructure/jdbc/` — změna `MembershipPaymentRuleSnapshotMemento`, `MembershipFeeGroupMemento`
- `backend/src/main/java/com/klabis/membershipfees/infrastructure/restapi/` — sjednocení response records
- `backend/src/main/java/com/klabis/membershipfees/` — ID typy (`MembershipFeeLevelId`, `MembershipFeeGroupId`, `FeeYearPublicationId`)
- Testy v `backend/src/test/java/com/klabis/membershipfees/` — existující testy zůstanou, přidají se testy pro Clock injection a agregátový deadline guard
