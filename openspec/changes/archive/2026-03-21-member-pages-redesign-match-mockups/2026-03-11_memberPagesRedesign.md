# Team Coordination File — member-pages-redesign-match-mockups

## Cíl
Implementovat redesign member stránek dle Pencil mockupů:
- Backend: podmíněný PATCH template podle oprávnění volajícího
- Frontend: 2-sloupcový layout, HAL-driven rendering, action bar dole

## Stav tasků (viz tasks.md)
- Sekce 1 (Příprava — mockupy): [ ] vše otevřeno
- Sekce 2 (Backend): [ ] vše otevřeno
- Sekce 3 (Frontend MemberDetailPage): [ ] vše otevřeno
- Sekce 4 (Frontend edit mód): [ ] vše otevřeno
- Sekce 5 (Frontend MemberRegistrationPage): [ ] vše otevřeno
- Sekce 6 (Testy a validace): [ ] vše otevřeno

## Iterace

### Iterace 1 — Backend (dokončeno)
Implementovat sekci 2: podmíněný PATCH template v getMember()

**Změny:**
- Nový record `SelfUpdateMemberRequest` se 4 poli (email, phone, address, dietaryRestrictions)
- Nový endpoint `PATCH /api/members/{id}/profile` (`updateMemberSelf`) pro self-update s omezeným typem requestu
- `getMember()` přijímá `@CurrentUser CurrentUserData currentUser` místo `UserId`
- Podmíněná logika affordancí: MEMBERS:MANAGE → plná affordance na `/{id}`, self → omezená affordance na `/{id}/profile`, jinak → žádná PATCH affordance
- Nové testy: `ownProfileShouldReturnSelfUpdateAffordanceWithLimitedFields`, `otherMemberProfileShouldNotReturnAnyPatchAffordance`
- Všechny testy prošly: 49 passed, 1 skipped (pre-existující @Disabled test)

### Iterace 2 — Frontend MemberDetailPage (dokončeno)
Implementovat sekce 3 a 4: redesign MemberDetailPage — layout, view mody, edit mód.

**Změny:**
- Přidán helper `resolveViewMode(template)` → vrací `'other' | 'self' | 'admin'` na základě přítomnosti a obsahu `_templates.default` (HAL-driven, žádný if(isAdmin))
  - `other`: template chybí → zobrazit jen Kontakt + Adresa, žádné akce
  - `self`: template bez pole `firstName` → 2-sloupcový layout, tlačítka Členské příspěvky + Upravit profil
  - `admin`: template s polem `firstName` → 2-sloupcový layout, 4 tlačítka (Upravit profil, Vložit/Vybrat, Oprávnění podmíněně, HalFormButton terminate)
- 2-sloupcový grid `grid grid-cols-1 lg:grid-cols-2 gap-10` pro self/admin view
- Admin badge (žlutý, `ShieldCheckIcon`) a self badge (zelený) v headeru při edit módu
- Edit action bar (Uložit změny + Zrušit) přesunut na konec stránky
- Label submit tlačítka změněn na "Uložit změny" (byl "Uložit")
- Ikony z `@heroicons/react/24/outline`: `PencilIcon`, `BanknotesIcon`, `ShieldCheckIcon`
- Testy: 34 testů MemberDetailPage, 719 celkem frontend testů — vše prošlo

**Rozhodnutí:**
- `HalFormButton` nepodporuje custom className/children → terminate použit bez custom styling
- `lucide-react` není v projektu → použit stávající `@heroicons/react`
- `enrichTemplateWithReadOnlyFields` funguje správně pro self-edit read-only pole

### Iterace 2b — Oprava TS chyb (dokončeno)

Cíl: Ověřit a opravit TypeScript chyby v MemberDetailPage.tsx (lucide-react, HalFormButton children, nepoužité importy).

**Zjištění:**
Všechny tři hlášené chyby byly již opraveny v Iteraci 2:
- `lucide-react` → soubor již používá `@heroicons/react/24/outline` (PencilIcon, BanknotesIcon, ShieldCheckIcon)
- `HalFormButton` → volán pouze s `name`, `modal`, `label` props — bez `children`
- Nepoužité importy → žádné nezůstaly

`npx tsc --noEmit` projde bez chyb.

**Testy:**
- MemberDetailPage: 34/34 passed
- Celkem frontend: 719/719 passed

### Iterace 3 — Frontend MemberRegistrationPage (dokončeno)
Implementovat sekce 5.1 a 5.2: redesign MemberRegistrationPage — 2-sloupcový layout, action bar dole.

**Změny:**
- Import `UserPlusIcon` z `@heroicons/react/24/outline`
- 2-sloupcový grid `grid grid-cols-1 lg:grid-cols-2 gap-10`:
  - Levý sloupec: Osobní údaje + Kontakt + Adresa
  - Pravý sloupec: pouze Doplňkové informace (bez dokladů) + Zákonný zástupce
- Doklady a licence zůstávají pod gridem (celá šířka) pokud jsou přítomny v template
- Action bar přesunut na konec stránky (za obsah, nad ním border): "Zrušit" (outline Link) + "Registrovat člena" (primary button, `UserPlusIcon`)
- Label submit tlačítka změněn na "Registrovat člena" (byl "Registrovat")
- `renderField('submit')` odstraněn — nahrazen vlastním `<button type="submit">` v action baru
- Labels v Doplňkových informacích upraven: "Číslo čipu" → "Číslo člena", "Číslo bankovního účtu (nepovinné)" → "Bankovní účet"

**Testy:**
- MemberRegistrationPage: 18/18 passed (existující testy prošly bez změn — `/registrovat/i` regex matchuje "Registrovat člena")
- Celkem frontend: 719/719 passed

### Iterace 4 — Opravy z code review (dokončeno)

Oprava dvou bugů identifikovaných v code review: C1 (critical security) a M2 (medium).

**Bug C1 — CRITICAL: PATCH /{id} self-cesta mapovala admin-only pole**

Příčina: `UpdateMemberRequestMapper.toSelfUpdateCommand(UpdateMemberRequest)` mapovala všechna pole ze
`UpdateMemberRequest` (včetně `chipNumber`, `nationality`, `bankAccountNumber`, `identityCard`,
`drivingLicenseGroup`, `medicalCourse`, `trainerLicense`, `guardian`) do `Member.SelfUpdate` commandu.

Oprava:
- `UpdateMemberRequestMapper.toSelfUpdateCommand(UpdateMemberRequest)` přepsána tak, aby mapovala POUZE
  4 povolená pole: `email`, `phone`, `address`, `dietaryRestrictions` — stejně jako overload
  `toSelfUpdateCommand(SelfUpdateMemberRequest)`. Admin-only pole jsou nyní ignorována.
- Nový test `SelfUpdateSecurityTests.selfUpdateShouldIgnoreAdminOnlyFields` v `MemberControllerApiTest`
  ověřuje, že `managementService.updateMember()` je voláno s `Member.SelfUpdate` příkazem, jehož
  `chipNumber` je `null`, i když byl zaslán validní `chipNumber` v requestu.
- Integrační test `UpdateMemberIntegrationTest.shouldUpdateExtendedSelfEditFields` byl přepsán —
  původně testoval buggy chování (self-user mohl aktualizovat admin-only pole). Nový test
  `adminOnlyFieldsShouldBeIgnoredInSelfUpdate` ověřuje, že admin-only pole jsou ignorována
  a pouze povolená pole (email) jsou uložena.

**Bug M2 — MEDIUM: name="reactivate" vs. name="resumeMember"**

Příčina: `MemberDetailPage.tsx` obsahoval `<HalFormButton name="reactivate" .../>`, ale backend
affordance se jmenuje `resumeMember` (metoda `resumeMember` v `MemberController`).

Oprava: `name="reactivate"` změněno na `name="resumeMember"` v `MemberDetailPage.tsx`.

**Výsledky testů:**
- Backend: 1453/1453 passed, 9 skipped — BUILD SUCCESSFUL
- Frontend: 719/719 passed — BUILD SUCCESSFUL

### Záznamy agentů
<!-- Agenti zde zapisují výsledky své práce -->
