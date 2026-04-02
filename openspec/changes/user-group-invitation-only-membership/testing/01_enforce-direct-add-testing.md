# Enforce Direct Member Add Restriction — QA Testing

## Scenarios

### UI — Free Group affordances

- [ ] **UI-1**: Owner volné skupiny nevidí tlačítko pro přímé přidání člena (žádná `addGroupMember` affordance)
- [ ] **UI-2**: Owner volné skupiny vidí tlačítko "Pozvat člena" (`inviteMember` affordance přítomna)

### UI — Training Group affordances (regression)

- [ ] **UI-3**: Owner tréninkové skupiny stále vidí tlačítko "Přidat člena" (affordance není odstraněna pro non-WithInvitations skupiny)

### API — Direct add blocked

- [ ] **API-1**: `POST /api/groups/{id}/members` pro volnou skupinu vrátí HTTP 422
- [ ] **API-2**: `GET /api/groups/{id}` pro volnou skupinu neobsahuje `addGroupMember` v `_templates`

---

## Results

### Iteration 1

| Scenario | Result | Note |
|----------|--------|------|
