# WIP - HAL+FORMS popis

HAL+FORMS je datovy format zalozeny na JSON ktery rozsiruje "data" o dva druhy metadat: reference mezi entitami a akce
ktere lze s entitou provest.

Klabis pouziva lehce prizpusobeny format popsany nize

## Domluvena pravidla pro Klabis:

1. vsechny API endpointy zpracovavane frontendem vraci HAL+FORMS content-type
    - UI explicitne vyzaduje tento format v Accept header
2. Resource muze byt:
   2.1. jeden datovy objekt
    - TBD: jak jej UI muze rozpoznat (obsahuje _self template, obsahuje dalsi atributy vedle _links, _templates, _
      embedded a page)

   2.2. kolekce datovych objektu
    - TBD jak jej UI muze rozpoznat (obsahuje _self template, obsahuje pouze atributy _links, _templates, _embedded a
      page)

   2.3. "formular" / akce - "controller" resource.
    - TBD jak jej UI muze rozpoznat (neobsahuje _self template)
    - pokud neobsahuje zadne policko, muze UI formular rovnou odeslat?

2. kazdy resource ma `_self` link ktery odkazuje na dany resource
    - pro UI ma vyznam predevsim v kolekcich (= odkaz na "detail" polozky z kolekce)

3. kazdy resource ma `_self` template ktera popisuje strukturu aktualniho objektu (vyuzitelne pro zobrazeni daneho
   objektu)
    - u kolekci jde o strukturu polozek kolekce ("tabulka")
    - u polozky jde o strukturu dane polozky ("detail")
    - u formulare neni pritomna (formular se renderuje podle svoji POST/DELETE template)
    - TBD (aktualne neni podporovano). Bude to template na GET endpoint ktere HATEOAS aktualne vyhazuje, to bude potreba
      ohnout.

5. kazdy resource muze obsahovat dalsi odkazy na dalsi resource ke kterym ma nejakou vazbu
    - UI toto zobrazuje jako "link" ktery naviguje na dalsi resource.

6. kazdy resource muze obsahovat dalsi template reprezentujici akce ktere je mozne s danym resource provest
    - UI toto zobrazuje obvykle jako tlacitka ktera oteviraji formular

## References

- [HAL+FORMS RFC draft](http://rwcbook.com/hal-forms/)
- [HAL RFC draft](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-00)