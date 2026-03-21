## 1. PermissionsDialog komponenta

- [x] 1.1 Vytvořit `frontend/src/components/members/PermissionsDialog.tsx` — modální dialog s toggle přepínači
- [x] 1.2 Definovat statický mapping oprávnění na české popisky a podnadpisy (MEMBERS:CREATE → "Vytváření členů", apod.)
- [x] 1.3 Implementovat načtení aktuálních oprávnění přes `useAuthorizedQuery` na URL z props
- [x] 1.4 Implementovat odeslání změn přes `useAuthorizedMutation` (PUT na URL z HATEOAS affordance)
- [x] 1.5 Zobrazit loading spinner dokud GET neskončí; disabled tlačítko "Uložit oprávnění" při načítání/ukládání
- [x] 1.6 Po úspěšném PUT: zavřít dialog a zavolat `addToast('Oprávnění uložena', 'success')`
- [x] 1.7 Při chybě PUT: zobrazit chybovou hlášku uvnitř dialogu (dialog zůstane otevřený)
- [x] 1.8 Tlačítko "Zrušit" zavře dialog bez API volání

## 2. Integrace do MemberDetailPage

- [x] 2.1 V `MemberDetailPage.tsx` nahradit `<Link>` tlačítko "Správa oprávnění" za `<button>` ovládající stav `isPermissionsDialogOpen`
- [x] 2.2 Přidat `<PermissionsDialog>` do JSX stránky s předáním URL z `route.getResourceLink('permissions')?.href` a jména člena
