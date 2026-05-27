# Iteration 7: Frontend — Own Account Page and Menu Entry - QA Testing

## Scenarios

### NAV — Finance menu entry
- [ ] **NAV-1**: Authenticated regular member (ZBM9500) sees "Finance" entry in desktop sidebar under the main (everyday) section
- [ ] **NAV-2**: "Finance" entry appears in mobile bottom navigation bar
- [ ] **NAV-3**: "Finance" entry is NOT in the Administrace (admin) section on desktop

### ACCOUNT — Own account page
- [ ] **ACCOUNT-1**: Clicking "Finance" in the menu opens the member's own financial account page
- [ ] **ACCOUNT-2**: The page heading "Finance" is displayed
- [ ] **ACCOUNT-3**: Current balance (Zůstatek) is displayed prominently with currency (Kč)
- [ ] **ACCOUNT-4**: Positive balance displays in green color styling
- [ ] **ACCOUNT-5**: "Historie transakcí" section heading is displayed below the balance

### TABLE — Transactions table
- [ ] **TABLE-1**: Transactions table renders with column headers: Datum, Typ, Částka, Popis
- [ ] **TABLE-2**: Transaction rows are displayed (for member with transactions)
- [ ] **TABLE-3**: DEPOSIT transactions show green color and upward arrow icon
- [ ] **TABLE-4**: OTHER (charge) transactions show red color and downward arrow icon
- [ ] **TABLE-5**: Table shows empty message "Žádné transakce" when no transactions exist

### FILTER — Transaction filters
- [ ] **FILTER-1**: Filter bar shows type filter pills: "Vše", "Vklad", "Výdaj"
- [ ] **FILTER-2**: Date range filter inputs (Datum od, Datum do) are present
- [ ] **FILTER-3**: Selecting "Vklad" pill filters table to show only DEPOSIT transactions

### REVERSAL — Visual distinction for reversed/reversal transactions
- [ ] **REVERSAL-1**: Reversed transactions are visually marked (strikethrough or muted)
- [ ] **REVERSAL-2**: Reversal transactions show "Storno" label/icon

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
