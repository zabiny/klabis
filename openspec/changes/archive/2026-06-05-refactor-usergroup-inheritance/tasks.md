## 1. MemberGroup a GroupMembership v groups.common.domain

- [x] 1.1 Vytvořit `MemberGroup` v `groups.common.domain` — abstraktní třída rozšiřující `KlabisAggregateRoot`, celé API na `MemberId` (owners, members)
- [x] 1.2 Vytvořit `GroupMembership` v `groups.common.domain` s `MemberId memberId` (místo `UserId userId`)
- [x] 1.3 Přesunout skupinové výjimky (`MemberAlreadyInGroupException`, `MemberNotInGroupException`, `OwnerCannotBeRemovedFromGroupException`, `CannotRemoveLastOwnerException`, `CannotPromoteNonMemberToOwnerException`, `DirectMemberAdditionNotAllowedException`) do `groups.common.domain`
- [x] 1.4 Napsat unit testy pro `MemberGroup` (pokrytí 100% doménové logiky)

## 2. GroupsExceptionHandler a úprava MvcExceptionHandler

- [x] 2.1 Vytvořit `GroupsExceptionHandler` (`@RestControllerAdvice`) v `groups` modulu — přebrat handlery skupinových výjimek z `MvcExceptionHandler`
- [x] 2.2 Odebrat skupinové výjimky z `common.mvc.MvcExceptionHandler`
- [x] 2.3 Ověřit, že exception handling testy pro skupiny stále prochází

## 3. TrainingGroup → extends MemberGroup

- [x] 3.1 Přepsat `TrainingGroup` na `extends MemberGroup` — odebrat `private final UserGroup userGroup`, odebrat delegační metody
- [x] 3.2 Upravit `TrainingGroup.reconstruct()` a `create()` — volání `super()`
- [x] 3.3 Upravit `GroupMemento.fromTrainingGroup()` a `toTrainingGroup()` — importy na nový `GroupMembership`
- [x] 3.4 Spustit testy `TrainingGroup` a opravit kompilační chyby

## 4. FamilyGroup → extends MemberGroup

- [x] 4.1 Přepsat `FamilyGroup` na `extends MemberGroup` — odebrat `private final UserGroup userGroup`, odebrat delegační metody
- [x] 4.2 Upravit `FamilyGroup.reconstruct()` a `create()` — volání `super()`
- [x] 4.3 Upravit `GroupMemento.fromFamilyGroup()` a `toFamilyGroup()` — importy na nový `GroupMembership`
- [x] 4.4 Spustit testy `FamilyGroup` a opravit kompilační chyby

## 5. FreeGroup → extends MemberGroup + přesun WithInvitations

- [x] 5.1 Přesunout `WithInvitations` do `groups.freegroup.domain`, přepsat API z `UserId` na `MemberId`
- [x] 5.2 Přesunout `Invitation` do `groups.freegroup.domain`, veřejné gettery vrátí `MemberId` (interně UUID pro persistence kompatibilitu)
- [x] 5.3 Přepsat `FreeGroup` na `extends MemberGroup` — odebrat `private final UserGroup userGroup`, odebrat delegační metody
- [x] 5.4 Upravit `FreeGroup.reconstruct()` a `create()` — volání `super()`
- [x] 5.5 Upravit `GroupMemento.fromFreeGroup()` a `toFreeGroup()` — importy na nový `GroupMembership` a `Invitation`
- [x] 5.6 Spustit testy `FreeGroup` a opravit kompilační chyby

## 6. Odstranění common.usergroup

- [x] 6.1 Ověřit, že žádný soubor mimo `common.usergroup` neimportuje třídy z tohoto package
- [x] 6.2 Odstranit celý package `common.usergroup`
- [x] 6.3 Spustit celou test suite, ověřit žádná regrese
