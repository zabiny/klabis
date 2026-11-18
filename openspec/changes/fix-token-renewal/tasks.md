## 1. Diagnostika

- [ ] 1.1 Přidat logování do `silentRenewRetry`, `authorizedFetch` a `setup.ts` (expirace uživatele při požadavku, status odpovědi, výsledek každého pokusu o renew)
- [ ] 1.2 Reprodukovat na PROD-like prostředí (same-origin) stale tab a z logů určit příčinu prvního selhání; výsledek zapsat do `design.md` (Decisions)

## 2. Jednotný modul obnovy tokenu

- [ ] 2.1 Napsat testy modulu `authSession` (`ensureFreshToken`, `fetchWithAuth`: expirovaný uživatel, 401 s jedním opakováním, souběžná volání sdílí jeden renew)
- [ ] 2.2 Implementovat modul a přepnout na něj `authorizedFetch` i middleware v `api/setup.ts`
- [ ] 2.3 Odstranit duplicitní logiku renew z obou míst

## 3. Klasifikace chyb a souběh renew

- [ ] 3.1 Testy: `login_required` se neopakuje, timeout a síťová chyba ano, definitivní selhání vyhodí `SessionExpiredError`
- [ ] 3.2 Upravit `silentRenewRetry` podle klasifikace a vypnout `automaticSilentRenew`; renew spouštět na `accessTokenExpiring` a `accessTokenExpired`
- [ ] 3.3 Zjednodušit `addSilentRenewError` v `klabisUserManager.ts`
- [ ] 3.4 Ověřit, že selhání renew vede na login s `returnTo` a `ErrorPage` se nezobrazí (test `AuthContext2`)

## 4. Samoléčení chyb stránky

- [ ] 4.1 Testy: po `userLoaded` se refetchnou authorized dotazy v chybovém stavu; `ErrorPage` nabídne „Zkusit znovu" pro chyby načtení
- [ ] 4.2 Implementovat refetch po `userLoaded` a tlačítko „Zkusit znovu" (lokalizace v `labels.ts`)

## 5. Ověření

- [ ] 5.1 Spustit existující frontend testy a `npm run build`, bez změny asercí mimo mechanické úpravy
- [ ] 5.2 Ručně na `:3000` ověřit: neplatný token → login s návratem na původní URL, doba do přesměrování výrazně pod 15 s
- [ ] 5.3 Přidat E2E scénář obnovy po expiraci tokenu a code review před commitem
