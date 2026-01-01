# HalFormButton Component

Komponenta pro zobrazení tlačítka HAL Forms template s možností otevření formuláře jako modal overlay nebo navigace na
novou stránku.

## Použití

### Modal režim (overlay)

```tsx
import { HalFormButton } from './components/HalFormButton';

function MyComponent() {
  return (
    <div>
      <h1>Akce</h1>
      {/* Zobrazí tlačítko, které otevře formulář jako modal */}
      <HalFormButton name="create" modal={true} />
      <HalFormButton name="edit" modal={true} />
    </div>
  );
}
```

### Non-modal režim (navigace na novou stránku)

```tsx
import { HalFormButton } from './components/HalFormButton';

function MyComponent() {
  return (
    <div>
      <h1>Akce</h1>
      {/* Zobrazí tlačítko, které naviguje na novou stránku */}
      <HalFormButton name="create" modal={false} />
    </div>
  );
}
```

## Props

| Prop    | Type      | Popis                                                                                   |
|---------|-----------|-----------------------------------------------------------------------------------------|
| `name`  | `string`  | Název HAL Forms template z `_templates` objektu                                         |
| `modal` | `boolean` | Pokud `true`, formulář se otevře jako overlay. Pokud `false`, naviguje na novou stránku |

## Funkce

- **Automatická kontrola existence template**: Komponenta zkontroluje zda v aktuálním resource existuje template s daným
  jménem. Pokud neexistuje, komponenta nic nevyrenderuje.
- **Konzistentní styling**: Používá sdílený komponent `HalFormTemplateButton` pro jednotný vzhled tlačítek napříč
  aplikací.
- **Target data fetching**: Automaticky fetchuje data z template.target endpointu pokud se liší od aktuálního resource.
- **Form submission**: Plně implementovaný submit formuláře pomocí `submitHalFormsData`:
    - Odešle data na endpoint specifikovaný v `template.target`
    - Použije HTTP metodu z `template.method` (default: POST)
    - Po úspěšném submitu automaticky refetchuje data (`refetch()`)
    - Zavře modal po úspěšném odeslání
- **Error handling**: Zobrazuje chybové stavy při načítání dat nebo při submitu formuláře včetně validačních chyb.
- **Modal overlay**: V modal režimu zobrazí formulář jako overlay s možností zavření.

## Kontext závislosti

Komponenta musí být použita uvnitř:

- `HalRouteProvider` - poskytuje aktuální resource data
- `QueryClientProvider` - pro React Query cache
- `BrowserRouter` nebo jiný router provider - pro navigaci v non-modal režimu

## Příklad v existující aplikaci

```tsx
function MemberDetailPage() {
  return (
    <div>
      <h1>Detail člena</h1>

      {/* Existující zobrazení dat */}
      <MemberDetails />

      {/* HAL Forms akce */}
      <div className="actions">
        <HalFormButton name="edit" modal={true} />
        <HalFormButton name="delete" modal={true} />
        <HalFormButton name="addToEvent" modal={true} />
      </div>
    </div>
  );
}
```

## Jak funguje form submission

Když uživatel vyplní a odešle formulář:

1. **Submit handler** zavolá `submitHalFormsData` s daty z formuláře
2. **HTTP request** je odeslán na endpoint z `template.target` metodou z `template.method`
3. Po **úspěšném submitu**:
    - Zavolá se `refetch()` pro aktualizaci dat na stránce
    - Modal se automaticky zavře
    - Data na stránce se aktualizují (pokud byla změněna)
4. Pokud dojde k **chybě**:
    - Zobrazí se chybová hláška v modalu
    - Pokud server vrátil validační chyby, zobrazí se detail pro každé pole
    - Modal zůstane otevřený, uživatel může opravit chyby a zkusit znovu

### Příklad HAL Forms template ze serveru

```json
{
  "_templates": {
    "edit": {
      "title": "Upravit člena",
      "method": "PATCH",
      "target": "/api/members/123",
      "properties": [
        {
          "name": "firstName",
          "prompt": "Jméno",
          "required": true
        },
        {
          "name": "lastName",
          "prompt": "Příjmení",
          "required": true
        }
      ]
    }
  }
}
```

Když uživatel klikne na `<HalFormButton name="edit" modal={true} />`:

1. Otevře se modal s formulářem
2. Uživatel vyplní jméno a příjmení
3. Po kliknutí na "Odeslat" se data pošlou jako PATCH na `/api/members/123`
4. Po úspěchu se zavře modal a data člena se aktualizují

## Související komponenty

- `HalFormsSection` - Zobrazuje všechny dostupné templates jako tlačítka a inline formulář
- `HalFormTemplateButton` - Sdílený presentational component pro template tlačítka
- `HalFormsForm` - Samotný formulář pro HAL Forms
