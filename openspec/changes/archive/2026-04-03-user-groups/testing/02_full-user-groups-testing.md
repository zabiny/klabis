# User Groups — Kompletní QA Testing

Testuje všechny features z propozice `user-groups` (Slices 1–6) a `user-group-invitation-only-membership`.
Slice 2 (tréninkové skupiny základní) byl otestován v `01_training-groups-testing.md` — zde jen regrese.

## Scenarios

### Volné skupiny — CRUD (FREE)
- [ ] **FREE-1**: Autentizovaný člen vidí sekci "Skupiny" nebo navigační položku pro skupiny
- [ ] **FREE-2**: Člen může vytvořit volnou skupinu
- [ ] **FREE-3**: Detail volné skupiny zobrazuje název, vlastníky, členy
- [ ] **FREE-4**: Vlastník může přejmenovat skupinu
- [ ] **FREE-5**: Vlastník může smazat skupinu
- [ ] **FREE-6**: Ne-vlastník nevidí tlačítka pro úpravu/smazání

### Pozvánky do volné skupiny (INVITE)
- [ ] **INVITE-1**: Vlastník vidí akci "Pozvat člena" (affordance `inviteMember`)
- [ ] **INVITE-2**: Vlastník nevidí akci "Přidat člena přímo" (affordance `addGroupMember` chybí pro volné skupiny)
- [ ] **INVITE-3**: Vlastník může odeslat pozvánku členovi
- [ ] **INVITE-4**: Pozvaný člen vidí pending pozvánku v seznamu skupin nebo na profilu
- [ ] **INVITE-5**: Pozvaný člen může pozvánku přijmout
- [ ] **INVITE-6**: Pozvaný člen může pozvánku odmítnout
- [ ] **INVITE-7**: Po přijetí pozvánky se člen zobrazí v detailu skupiny

### Správa členů volné skupiny (MEMBERS)
- [ ] **MEMBERS-1**: Vlastník může odebrat člena ze skupiny
- [ ] **MEMBERS-2**: Člen (ne-vlastník) nevidí tlačítko pro odebrání jiného člena

### API — Omezení přímého přidání (API)
- [ ] **API-1**: `POST /api/groups/{id}/members` pro volnou skupinu vrátí HTTP 422
- [ ] **API-2**: `GET /api/groups/{id}` pro volnou skupinu neobsahuje `addGroupMember` v `_templates`

### Tréninkové skupiny — Automatizace (TRAIN)
- [ ] **TRAIN-1**: Nový člen s věkem v rozsahu skupiny je automaticky přiřazen (viditelné v detailu skupiny nebo na profilu člena)
- [ ] **TRAIN-2**: Profil člena zobrazuje jeho tréninkovou skupinu
- [ ] **TRAIN-3**: Tréninkové skupiny stále mají affordance `addGroupMember` (regrese)

### Rodinné skupiny (FAMILY)
- [ ] **FAMILY-1**: Admin může vytvořit rodinnou skupinu ze sekce členů
- [ ] **FAMILY-2**: Rodinná skupina se zobrazí v detailu/profilu člena
- [ ] **FAMILY-3**: Člen nemůže být v 2 rodinných skupinách (exclusive membership)
- [ ] **FAMILY-4**: Profil člena zobrazuje jeho rodinnou skupinu

### Správa vlastníků (OWNER)
- [ ] **OWNER-1**: Vlastník může přidat dalšího vlastníka
- [ ] **OWNER-2**: Vlastník může odebrat jiného vlastníka (ne sám sebe, pokud je poslední)
- [ ] **OWNER-3**: Poslední vlastník nemůže být odebrán (validace)
- [ ] **OWNER-4**: Upozornění při deaktivaci člena, který je posledním vlastníkem skupiny

### Integrace s profily členů (PROFILE)
- [ ] **PROFILE-1**: Profil člena zobrazuje jeho skupiny (tréninkovou, rodinnou, volné)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| FREE-1 | PASS | "Skupiny" viditelné v navigaci |
| FREE-2 | PASS | Volná skupina vytvořena, zobrazena v tabulce |
| FREE-3 | PASS | Detail zobrazuje název, SPRÁVCI, ČLENOVÉ |
| FREE-4 | PASS | Přejmenování funguje, toast "Úspěšně uloženo" |
| FREE-5 | SKIP | Přerušeno — oprava issues |
| FREE-6 | PASS | Ne-vlastník (ZBM9500) nevidí Upravit/Smazat tlačítka |
| INVITE-1 | PASS | Vlastník vidí "Pozvat člena" tlačítko |
| INVITE-2 | PASS | Žádné "Přidat člena přímo" tlačítko pro volnou skupinu |
| INVITE-3 | PASS | Pozvánka odeslána, zobrazila se sekce "ČEKAJÍCÍ POZVÁNKY" |
| INVITE-4 | PASS | ZBM9500 vidí pending pozvánku na stránce /groups |
| INVITE-5 | PASS | Pozvánka přijata, skupina se zobrazila v "Moje skupiny" |
| INVITE-6 | SKIP | Přerušeno — oprava issues |
| INVITE-7 | PASS | Po přijetí pozvánky se člen zobrazil v tabulce členů |
| MEMBERS-1 | PASS | Vlastník odebral člena, tabulka se aktualizovala |
| MEMBERS-2 | PASS | Ne-vlastník nevidí tlačítko "Odebrat" u jiných členů |
| API-1 | PASS | POST /api/groups/{id}/members vrátí HTTP 422 pro volnou skupinu |
| API-2 | PASS | GET /api/groups/{id} neobsahuje addGroupMember v _templates |
| TRAIN-3 | PASS | Tréninkové skupiny mají addTrainingGroupMember (UI: "Přidat člena") |
| OWNER-1 | FAIL | UI nezobrazuje tlačítko pro přidání vlastníka; backend affordance addGroupOwner/addTrainingGroupOwner existuje |
| OWNER-2 | SKIP | Přerušeno — oprava issues |
| OWNER-3 | SKIP | Přerušeno — oprava issues |
| OWNER-4 | SKIP | Přerušeno — oprava issues |
| FAMILY-1 | FAIL | /family-groups zobrazuje raw HAL data ("Detaily", "Dostupné formuláře") místo user-friendly UI |
| FAMILY-2 | SKIP | Přerušeno — oprava issues |
| FAMILY-3 | SKIP | Přerušeno — oprava issues |
| FAMILY-4 | SKIP | Přerušeno — oprava issues |
| TRAIN-1 | SKIP | Přerušeno — oprava issues |
| TRAIN-2 | SKIP | Přerušeno — oprava issues |
| PROFILE-1 | SKIP | Přerušeno — oprava issues |

**Problémy nalezené v Iteraci 1 (všechny opraveny):**

1. **ISSUE-1** [Frontend] — Navigační položka "family-groups" bez lokalizace → opraveno: přidán label "Rodinné skupiny" do labels.ts
2. **ISSUE-2** [Frontend] — Stránka /family-groups zobrazovala raw HAL data → opraveno: vytvořeny FamilyGroupsPage a FamilyGroupDetailPage
3. **ISSUE-3** [Frontend] — Chybělo tlačítko pro přidání vlastníka → opraveno: opraveny template names addGroupOwner/addTrainingGroupOwner
4. **ISSUE-4** [Frontend] — HAL+FORMS property typ "UUID" nebyl podporován → opraveno: přidána podpora v KlabisFieldsFactory
5. **ISSUE-5** [Frontend] — `multi: true` se nemapovalo na `multiple` → opraveno: přidána `isMultipleProperty()` helper a `multi` do HalFormsProperty

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| FREE-1 | PASS | "Skupiny" viditelné v navigaci |
| FREE-2 | PASS | Volná skupina vytvořena |
| FREE-3 | PASS | Detail zobrazuje název, SPRÁVCI, ČLENOVÉ |
| FREE-4 | PASS | Přejmenování funguje |
| FREE-5 | SKIP | Přerušeno — oprava PROFILE-1 |
| FREE-6 | PASS | Ne-vlastník nevidí edit/delete |
| INVITE-1 | PASS | Tlačítko "Pozvat člena" viditelné pro vlastníka |
| INVITE-2 | PASS | Žádné "Přidat člena přímo" pro volnou skupinu |
| INVITE-3 | PASS | Pozvánka odeslána |
| INVITE-4 | PASS | ZBM9500 vidí pending pozvánku |
| INVITE-5 | PASS | Přijetí pozvánky funguje |
| INVITE-6 | SKIP | Přerušeno — oprava PROFILE-1 |
| INVITE-7 | PASS | Člen zobrazen po přijetí pozvánky |
| MEMBERS-1 | PASS | Odebrání člena funguje |
| MEMBERS-2 | PASS | Ne-vlastník nevidí "Odebrat" tlačítka |
| API-1 | PASS | POST /api/groups/{id}/members vrátí HTTP 422 pro volnou skupinu |
| API-2 | PASS | addGroupMember není v _templates pro volnou skupinu |
| TRAIN-3 | PASS | Tréninkové skupiny mají "Přidat člena" (addTrainingGroupMember) |
| OWNER-1 | PASS | Tlačítko "Přidat správce" funguje, formulář se správně renderuje |
| OWNER-2 | PASS | Odebrání správce funguje |
| OWNER-3 | PASS | Poslední vlastník nemá tlačítko "Odebrat správce" |
| OWNER-4 | SKIP | Přerušeno — oprava PROFILE-1 |
| FAMILY-1 | PASS | Rodinná skupina vytvořena s multi-select checkboxy |
| FAMILY-2 | PASS | Detail rodinné skupiny zobrazuje správce a členy |
| FAMILY-3 | PASS | Exkluzivní členství — chyba "Member is already in a family group" |
| FAMILY-4 | SKIP | Přerušeno — oprava PROFILE-1 |
| TRAIN-1 | SKIP | Přerušeno — oprava PROFILE-1 |
| TRAIN-2 | SKIP | Přerušeno — oprava PROFILE-1 |
| PROFILE-1 | FAIL | Profil člena nezobrazuje sekci se skupinami (tréninkovou, rodinnou, volnými) |

**Problémy nalezené v Iteraci 2 (opraveny):**

1. **ISSUE-6** [Backend+Frontend] — Profil člena nezobrazoval skupiny → opraveno: přidán FamilyGroupProvider, FamilyGroupResponse do MemberDetailsResponse, frontend oprava fieldName name→groupName

### Iteration 3
| Scenario | Result | Note |
|----------|--------|------|
| FREE-1 | PASS | "Skupiny" viditelné v navigaci |
| FREE-2 | PASS | Volná skupina vytvořena |
| FREE-3 | PASS | Detail zobrazuje název, SPRÁVCI, ČLENOVÉ |
| FREE-4 | PASS | Přejmenování funguje |
| FREE-5 | PASS | Smazání skupiny funguje, redirect na seznam |
| FREE-6 | PASS | Ne-vlastník nevidí edit/delete |
| INVITE-1 | PASS | Tlačítko "Pozvat člena" viditelné pro vlastníka |
| INVITE-2 | PASS | Žádné "Přidat člena přímo" pro volnou skupinu |
| INVITE-3 | PASS | Pozvánka odeslána, sekce ČEKAJÍCÍ POZVÁNKY se zobrazila |
| INVITE-4 | PASS | ZBM9500 vidí pending pozvánku |
| INVITE-5 | PASS | Přijetí pozvánky funguje |
| INVITE-6 | PASS | Odmítnutí pozvánky funguje, pozvánka zmizela |
| INVITE-7 | PASS | Člen zobrazen po přijetí pozvánky |
| MEMBERS-1 | PASS | Odebrání člena funguje |
| MEMBERS-2 | PASS | Ne-vlastník nevidí "Odebrat" tlačítka |
| API-1 | PASS | POST /api/groups/{id}/members vrátí HTTP 422 pro volnou skupinu |
| API-2 | PASS | addGroupMember není v _templates pro volnou skupinu |
| TRAIN-3 | PASS | Tréninkové skupiny mají "Přidat člena" (addTrainingGroupMember) |
| OWNER-1 | PASS | Tlačítko "Přidat správce" funguje |
| OWNER-2 | PASS | Odebrání správce funguje |
| OWNER-3 | PASS | Poslední vlastník nemá tlačítko "Odebrat správce" |
| OWNER-4 | FAIL | Dialog ukončení členství nezobrazuje upozornění o skupinách kde je člen posledním vlastníkem; nadpis dialogu "suspendMember" není lokalizován |
| FAMILY-1 | PASS | Rodinná skupina vytvořena s multi-select checkboxy |
| FAMILY-2 | PASS | Detail rodinné skupiny zobrazuje správce a členy |
| FAMILY-3 | PASS | Exkluzivní členství — chyba "Member is already in a family group" |
| FAMILY-4 | PASS | Profil člena zobrazuje rodinnou skupinu "Svobodovi" |
| TRAIN-1 | FAIL | Vytvoření tréninkové skupiny nepřiřazuje automaticky existující členy ve věkovém rozsahu (spec: "automatically assigns all active members whose age falls within the range") |
| TRAIN-2 | PASS | Profil člena zobrazuje tréninkovou skupinu "Dospělí" po ručním přidání |
| PROFILE-1 | PASS | Profil člena zobrazuje sekce TRÉNINKOVÁ SKUPINA a RODINNÁ SKUPINA s názvem a správci |

**Problémy nalezené v Iteraci 3:**

1. **ISSUE-7** [Backend] — Při vytvoření tréninkové skupiny se nepřiřazují automaticky existující členové ve věkovém rozsahu
2. **ISSUE-8** [Frontend+Backend] — Dialog "Ukončit členství" nezobrazuje upozornění o skupinách kde je člen posledním vlastníkem; nadpis "suspendMember" není lokalizován
