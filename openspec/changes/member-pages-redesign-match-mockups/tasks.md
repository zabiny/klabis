## 1. Příprava — přečíst mockupy

- [ ] 1.1 Načíst screenshoty všech 6 mockup variant přes Pencil MCP (`get_screenshot` pro node IDs: `xmQFI`, `7Q3Kh`, `kVhxA`, `P5n4i`, `LdRj4`, `NIoA4` v souboru `pencil/klabis-members.pen`)
- [ ] 1.2 Načíst detaily action baru Admin detailu přes Pencil MCP (`batch_get` node `tzX7R`, depth 4)
- [ ] 1.3 Načíst detaily Registration action baru přes Pencil MCP (`batch_get` node `ZkiRw`, depth 3)

## 2. Backend — podmíněný PATCH template v getMember

- [ ] 2.1 Přidat `@CurrentUser CurrentUserData currentUser` parametr do `getMember()` v `MemberController`
- [ ] 2.2 Implementovat logiku: pokud `currentUser.hasAuthority(MEMBERS_MANAGE)` → přidat affordanci se všemi poli; pokud `currentUser.memberId().uuid().equals(id)` → přidat affordanci pouze s poli `email`, `phone`, `address`, `dietaryRestrictions`; jinak žádná affordance
- [ ] 2.3 Aktualizovat nebo přidat testy v `MemberControllerApiTest` pro všechny tři varianty (admin, self, other)

## 3. Frontend — MemberDetailPage layout redesign

- [ ] 3.1 Nahradit 1-sloupcový layout 2-sloupcovým gridem (`grid grid-cols-1 lg:grid-cols-2 gap-10`): levý sloupec (osobní/kontakt/adresa), pravý sloupec (doplňkové/doklady)
- [ ] 3.2 Implementovat "other member" view: pokud `_templates.default` chybí, zobrazit pouze kontakt + adresu bez akčních tlačítek
- [ ] 3.3 Implementovat "own profile" action bar: tlačítka "Členské příspěvky" a "Upravit profil" — viditelné když template existuje ale neobsahuje admin pole (firstName)
- [ ] 3.4 Implementovat "admin" action bar s Lucide ikonami: "Upravit profil" (`pencil`), "Vložit / Vybrat" (`banknote`), "Oprávnění" (`shield`, podmíněně), "Ukončit členství" (`user-x`, červená) — viditelné když template obsahuje admin pole

## 4. Frontend — MemberDetailPage edit mód

- [ ] 4.1 Přesunout action bar (Zrušit + Uložit změny) na konec obsahu stránky
- [ ] 4.2 Přidat admin badge "Admin — editace všech polí" (žlutý, ikona `shield`) do hlavičky při admin edit módu
- [ ] 4.3 Ověřit že self-edit zobrazuje read-only pole pro pole která nejsou v template (existující `enrichTemplateWithReadOnlyFields` logika)

## 5. Frontend — MemberRegistrationPage layout redesign

- [ ] 5.1 Implementovat 2-sloupcový layout: levý sloupec (osobní/kontakt/adresa), pravý sloupec (pouze doplňkové informace — bez dokladů)
- [ ] 5.2 Přesunout action bar na konec stránky: "Zrušit" (outline) + "Registrovat člena" (primary, ikona `user-plus`)

## 6. Testy a validace

- [ ] 6.1 Spustit backend testy: `./gradlew test`
- [ ] 6.2 Spustit frontend testy: `npm run test`
- [ ] 6.3 Ověřit přes Playwright na `localhost:3000` — přihlásit se jako admin a porovnat detail s mockupem `kVhxA`
- [ ] 6.4 Ověřit přes Playwright na `localhost:3000` — přihlásit se jako člen a ověřit vlastní profil vs mockup `7Q3Kh`
- [ ] 6.5 Ověřit přes Playwright na `localhost:3000` — přihlásit se jako člen a ověřit cizí profil vs mockup `xmQFI`
- [ ] 6.6 Ověřit registrační formulář přes Playwright vs mockup `NIoA4`
