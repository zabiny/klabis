## Why

Skupiny s pozvákovým systémem (volné skupiny) momentálně nezakazují přímé přidání člena bez pozvánky na úrovni API, čímž se obchází zamýšlený tok: pozvánka → akceptace. Pravidlo musí být vynuceno na backend vrstvě, aby bylo garantováno bez ohledu na frontend.

## What Changes

- Přímé přidání člena do volné skupiny je **zakázáno** — člen může být do volné skupiny přidán pouze prostřednictvím přijaté pozvánky
- API endpoint pro přímé přidání člena do skupiny bude buď odstraněn, nebo vrátí chybovou odpověď pro skupiny s pozvánkovým systémem
- Backend validace zajistí, že `WithInvitations` skupiny neumožňují přímé přidání přes doménovou vrstvu

## Capabilities

### New Capabilities

_(žádné nové capability — jde o zpřísnění existujícího chování)_

### Modified Capabilities

- `user-groups`: Požadavek na členství ve volné skupině výhradně přes pozvánkový systém — přímé přidání člena musí být zamítnuto

## Impact

- Backend modul `user-groups`: úprava doménové logiky pro `FreeGroup` / `WithInvitations`
- API: odstranění nebo zamítnutí endpointu pro přímé přidání člena do skupiny s pozvákovým systémem
- Testy: přidání scénáře ověřujícího odmítnutí přímého přidání člena
