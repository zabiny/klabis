## 1. Backend — Rozšíření MemberSummaryResponse

- [x] 1.1 Napsat testy pro nová pole email a active v member summary response (admin vidí hodnoty, non-admin vidí null)
- [x] 1.2 Přidat pole `email` (String) a `active` (boolean) do `MemberSummaryResponse` s `@HasAuthority(MEMBERS_MANAGE)` field-level security
- [x] 1.3 Rozšířit `MemberMapper` o mapování email a active z Member entity do summary response

## 2. Backend — Affordances na summary items

- [x] 2.1 Napsat testy pro affordances na summary items (admin: suspendMember/resumeMember podle stavu, non-admin: žádné affordances)
- [x] 2.2 Upravit `MemberController.listMembers()` — přidat affordances na self link každého summary itemu (updateMember, suspendMember/resumeMember) podmíněně podle `member.isActive()` a `currentUser.hasAuthority(MEMBERS_MANAGE)`

## 3. Backend — Permissions link na summary items

- [x] 3.1 Napsat testy pro permissions link na summary items (přítomen pro aktivní členy s MEMBERS:PERMISSIONS, nepřítomen pro neaktivní)
- [x] 3.2 Rozšířit `MemberPermissionsLinkProcessor` aby zpracovával i `EntityModel<MemberSummaryResponse>` (přidat `_links.permissions` pro aktivní členy)

## 4. Frontend — Nové sloupce v tabulce členů

- [x] 4.1 Přidat sloupec E-mail do `MembersPage` (zobrazí se jen pokud data přijdou z API, tj. pro admina)
- [x] 4.2 Přidat sloupec Stav s badge komponentou (Aktivní zelený / Neaktivní šedý) podmíněně podle přítomnosti pole `active`

## 5. Frontend — Sloupec Akce s ikonami

- [x] 5.1 Přidat sloupec Akce s ikonou pencil (editace) — navigace na detail člena, zobrazí se pokud existuje `_templates.default`
- [x] 5.2 Přidat ikonu shield (oprávnění) — otevře PermissionsDialog, zobrazí se pokud existuje `_links.permissions`
- [x] 5.3 Přidat ikonu user-x (suspend) — HalFormButton overlay, zobrazí se pokud existuje `_templates.suspendMember`
- [x] 5.4 Přidat ikonu user-check (resume) — HalFormButton overlay, zobrazí se pokud existuje `_templates.resumeMember`
- [x] 5.5 Zajistit stopPropagation na kliknutí ikon (aby se nenavigoval na detail člena)

## 6. Ověření

- [x] 6.1 Spustit backend testy a ověřit úspěšnost
- [x] 6.2 Spustit frontend testy a ověřit úspěšnost
