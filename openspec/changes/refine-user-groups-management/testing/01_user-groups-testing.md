# Refine User Groups Management — QA Testing

## Scenarios

### Family Group — Creation (scalar parent)
- [ ] **FC-1**: Vytvoření rodinné skupiny zobrazí formulář jen s polem "Název" a "Rodič" (ne selektor více rodičů)
- [ ] **FC-2**: Po vytvoření skupiny se stránka přesměruje na detail skupiny
- [ ] **FC-3**: Na detailu je tvůrce zobrazen v sekci "Rodiče", ne v sekci "Děti"

### Family Group — Add/Remove Child
- [ ] **FCC-1**: Na detailu skupiny (jako admin) je tlačítko "Přidat člena"
- [ ] **FCC-2**: Po kliknutí na "Přidat člena" se zobrazí dialog s výběrem role (Rodič / Dítě)
- [ ] **FCC-3**: Výběr "Dítě" otevře formulář pro přidání dítěte
- [ ] **FCC-4**: Po přidání dítěte se dítě zobrazí v sekci "Děti" na detailu skupiny
- [ ] **FCC-5**: U každého dítěte je tlačítko "Odebrat" (admin)
- [ ] **FCC-6**: Po odebrání dítěte zmizí ze sekce "Děti"
- [ ] **FCC-7**: Picker pro přidání dítěte neobsahuje členy, kteří jsou již v skupině (rodiče ani děti)
- [ ] **FCC-8**: Výběr "Rodič" otevře formulář pro přidání rodiče
- [ ] **FCC-9**: Přidání rodiče, který je již dítětem v téže skupině — zobrazí chybu

### Family Group — Authorization
- [ ] **FA-1**: Uživatel bez MEMBERS:MANAGE nevidí tlačítko "Přidat člena"
- [ ] **FA-2**: Člen rodinné skupiny (dítě) vidí detail skupiny bez admin affordancí

### Training Group — Trainee Exclusivity
- [ ] **TG-1**: Manuální přidání člena do tréninkové skupiny projde pokud není v jiné skupině
- [ ] **TG-2**: Manuální přidání člena, který je již tréninkem v jiné skupině, zobrazí chybu
- [ ] **TG-3**: Přidání trenéra do druhé skupiny projde (trenér není tréninkem)

### Free Group (MembersGroup) — Owner Promotion
- [ ] **FG-1**: Povýšení existujícího člena na vlastníka projde
- [ ] **FG-2**: Picker pro "přidat vlastníka" nabízí jen stávající členy skupiny (ne všechny)
- [ ] **FG-3**: Pokus o přidání vlastníka mimo skupinu vrátí chybu 409

### Member Picker Filtering
- [ ] **MP-1**: Picker pro přidání trenéra do tréninkové skupiny nezobrazuje již přidané trenéry
- [ ] **MP-2**: Picker pro přidání člena do tréninkové skupiny nezobrazuje již přidané členy

---

## Issues Found

### ISSUE-1: "Smazat rodinnou skupinu" viditelné pro člena bez MEMBERS:MANAGE
- **Popis:** Eva Svobodová (ZBM9500, dítě skupiny, bez MEMBERS:MANAGE) vidí tlačítko "Smazat rodinnou skupinu" na detailu skupiny.
- **Očekáváno:** Tlačítko mazání se zobrazí jen uživatelům s MEMBERS:MANAGE (backend by neměl emitovat `deleteFamilyGroup` affordanci pro non-admin).
- **Komponenta:** Backend — FamilyGroupController, HAL-Forms affordance pro `deleteFamilyGroup`

---

## Results

### Iteration 1
| Scénář | Výsledek | Poznámka |
|--------|----------|----------|
| FC-1 | PASS | Formulář má jen Název + parent (scalar) |
| FC-2 | PASS | Po vytvoření přesměrování na detail |
| FC-3 | PASS | Tvůrce v sekci RODIČE, DĚTI prázdné |
| FCC-1 | PASS | Tlačítko "Přidat člena" přítomno (admin) |
| FCC-2 | PASS | Dialog s výběrem Rodič/Dítě |
| FCC-3 | PASS | Formulář "Přidat dítě" po výběru role |
| FCC-4 | PASS | Dítě se zobrazí v sekci DĚTI |
| FCC-5 | PASS | Tlačítko "Odebrat" u řádku dítěte |
| FCC-6 | PASS | Po odebrání dítě zmizí ze sekce DĚTI |
| FCC-7 | PASS | Picker pro dítě neobsahuje rodiče ani existující děti |
| FCC-8 | PASS | Formulář "Přidat rodiče" po výběru role Rodič |
| FCC-9 | PASS | Picker pro rodiče neobsahuje existující děti (konflikt řešen v UI) |
| FA-1 | PASS | Navigace pro ZBM9500 neobsahuje "Rodinné skupiny" |
| FA-2 | FAIL | **ISSUE-1**: Eva vidí "Smazat rodinnou skupinu" bez MEMBERS:MANAGE |
| TG-1 | SKIP | přerušeno — oprava ISSUE-1 |
| TG-2 | SKIP | přerušeno |
| TG-3 | SKIP | přerušeno |
| FG-1 | SKIP | přerušeno |
| FG-2 | SKIP | přerušeno |
| FG-3 | SKIP | přerušeno |
| MP-1 | SKIP | přerušeno |
| MP-2 | SKIP | přerušeno |
