## 1. Backend

- [ ] 1.1 Test (nejdřív): nezletilý bez MEMBERS:MANAGE nedostane na vlastním detailu affordance `updateMember`; dospělý ji dostane; admin ji dostane i u nezletilého
- [ ] 1.2 Test: `PATCH` na vlastní profil nezletilého bez MEMBERS:MANAGE vrací 403 a nic neuloží; admin úspěšně; dospělý úspěšně
- [ ] 1.3 Sdílené pravidlo „vlastní úprava nezletilého je zakázána“ a použití v `MemberSelfLinkSupport` i při úpravě (`MemberController`/`ManagementService`)
- [ ] 1.4 Test: člen, kterému je právě 18, může upravovat sám sebe

## 2. Frontend

- [ ] 2.1 Ověřit, že detail nezletilého bez šablony `updateMember` akci „Upravit“ nezobrazí (bez změny kódu, případně test)

## 3. Dokončení

- [ ] 3.1 Test-runner: backend members modul a frontend, `npm run build`
- [ ] 3.2 QA přes Playwright: ZBM9000 upraví nezletilého, nezletilý s aktivním účtem „Upravit“ nevidí a PATCH dostane 403
