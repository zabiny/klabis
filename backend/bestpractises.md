# Klabis Best practises

## HAL+FORMS

- `_links` obsahuje relace na ostatni objekty (provazani mezi ruznymi GET resources). `_templates` (affordances)
  obsahuji AKCE (PUT, POST, DELETE) ktere lze v danem miste provest. To znamena ze `_templates` ma vyznam jak v Item
  resource, tak i
  Collection resource (= pridani noveho zaznamu, napr. registrace noveho uzivatele pokud nema URL s GET metodou).
- Pokud resource nema zadny "form" ktery by mohl zobrazit pro aktualni URL, tak nesmi vracet HAL+FORMS content type. 
- affordances jsou definovany v `RepresentationModelProcessor` umistenem u Controlleru na ktery dana affordance
  odkazuje. 