## Why

Frontend member pages (detail, registrace, edit) mají 1-sloupcový layout neodpovídající schváleným Pencil mockupům. Navíc backend vrací pro PATCH endpoint jeden template se všemi poli a frontend (nebo aplikační vrstva) musí detekovat, která pole jsou editovatelná pro daného uživatele — tato logika patří na backend, který ji na základě oprávnění volajícího zohlední přímo v HAL template.

## What Changes

- Backend `GET /api/members/{id}` vrátí PATCH template pouze s poli dostupnými pro volajícího uživatele:
  - admin (MEMBERS:MANAGE): všechna pole
  - vlastní profil (self): email, phone, address, dietaryRestrictions
  - cizí člen: žádný template
- Frontend `MemberDetailPage` zobrazí sekce a pole pouze na základě toho, co je v template a response — žádná detekce role na straně frontendu
- Layout detailní stránky a edit formulářů přejde na 2-sloupcový grid (vlevo osobní/kontakt/adresa, vpravo doplňkové/doklady)
- Akční tlačítka na edit a registrační stránce přesunuty dole (místo nahoře)
- Admin detail dostane akční tlačítka s Lucide ikonami v hlavičce
- Registrační formulář dostane 2-sloupcový layout

## Capabilities

### New Capabilities

- (žádné nové)

### Modified Capabilities

- `members`: Backend PATCH template vrací pole filtrovaná podle oprávnění volajícího; frontend zobrazuje pouze dostupná pole

## Impact

- `MemberController.getMember()` — podmíněné přidání PATCH template podle oprávnění volajícího
- `frontend/src/pages/members/MemberDetailPage.tsx` — layout redesign, odstranění detekce role
- `frontend/src/pages/members/MemberRegistrationPage.tsx` — layout a akce
- Backend testy: `MemberControllerApiTest`, `UpdateMemberApiTest`
- Frontend testy: `MemberDetailPage.test.tsx`, `MemberRegistrationPage.test.tsx`
