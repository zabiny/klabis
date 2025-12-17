# Klabis Best practises

## HAL+FORMS

- `_links` obsahuje relace na ostatni objekty (provazani mezi ruznymi GET resources). `_templates` (affordances)
  obsahuji AKCE (PUT, POST, DELETE) ktere lze v danem miste provest. To znamena ze `_templates` ma vyznam jak v Item
  resource, tak i
  Collection resource (= pridani noveho zaznamu, napr. registrace noveho uzivatele pokud nema URL s GET metodou).
- Pokud resource nema zadny "form" ktery by mohl zobrazit pro aktualni URL, tak nesmi vracet HAL+FORMS content type. 
- affordances jsou definovany v `RepresentationModelProcessor` umistenem u Controlleru na ktery dana affordance
  odkazuje. 
- Pri zpracovani ResourceModel kolekce (Collection/Page) jsou affordance z jednotlivych polozek v kolekci vyhozeny.
  Zustavaji pouze affordance na samotne kolekci. Pokud je potreba jinak, dejte vedet David P.
    - jak toto pouzit u collections a create item? Bude to stacit kdyz create bude mit "stejnou" strukturu jako polozky
      kolekce?
- struktura objektu s affordance by mela odpovidat strukture GET endpointu na stejne URL (= mel by to byt stejny DTO,
  max nejaka policka nejsou videt popr. jsou (vyjimecne) pridany.

Tedy reseni Hal+Forms affordances:

### Hal+Forms Affordances logika

1. Pokud affordance ma stejnou URL jako aktualni resource (popr. nema zadnou), tak musi mit stejnou strukturu jako
   objekty v danem resource (popr. kolekci resources). Take neni potreba delat fetch:
    2. pokud mam affordanci ktera je na kolekci, tak se predpoklada ze bud vytvarim novy objekt nebo zpracovavam vsechny
       v kolekci (popr. podle UI napr. vybrane polozky).
    3. pokud mam affordanci ktera je na Item, tak pro formular pouzit hodnoty z daneho GET.
2. Pokud affordance ma jinou URL nez je aktualni resource, tak se pred zobrazenim formulare zkontroluje jestli existuje
   GET s danym URL a pokud ano, tak se response toho GET pouzije pro populaci formulare

## EventSourcing

- WriteModel - zpracovani "write" operaci v podstate spociva v kontrole zda je mozne operaci provest a pokud ano, tak
  ulozeni (obvykle) jednoho eventu do EventStore. Tato kontrola se ale musi proti spolehlivemu domenovemu modelu (= musi
  byt up-to-date). Tedy pro tuto kontrolu nestaci jednoduchy CRUD jako v pripade ReadModelu.
    - nemely by existovat event sourcing eventy ktere ovlivnuji dva agregaty (na domenove strane ale takove eventy byt
      muzou!). Napr. TransferMoney z jednoho uctu na druhy: jako domenovy event ok, ale jako event sourcing event to
      budou dva eventy pro kazdy ucet zvlast. Jinymi slovy - **jeden event-sourcing event = 1 aggregate root**.
- ReadModel je pak v podstate CRUD ktery je aktualizovan z EventListeners prislusnych eventu (SpringDataREST ??).
  ReadModel je postaven tak aby odpovidal pozadovanym resources ktere jsou ulozene do DB jako jednoduche tabulky (zadna
  normalizace) ktere je pak mozne snadno sortovat a filtrovat. 
