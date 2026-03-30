---
name: tests-structure-review
description: Reviews structure of tests and marks suspicious tests
disable-model-invocation: true
---

Analyse tests structure:

Pracuj v ramci jednoho aplikacniho modulu ("root" package). Pokud je pozadovano review pro vice modulu, spust pro kazdy modul separatniho subagenta.

1. identifikuj vstupni body do jednotlivych technickych vrstev (domena, infrastructure, service)
2. seznam se strukturou testu v danem modulu - predevsim druh testu (Mockito, Spring slice test, SpringBootTest), jaka trida je testovana, jaka metoda je testovana, jake veci jsou v jednolivych testech asertovany a jake zavislosti jsou v testu mockovany.
3. Projdi vsechny testy ktere pouzivaji v nejake podobne spring kontext (SpringBootTest, ApplicationModuleTest, slice tests) a oznac jako podezrele tyto testy:
   - testy ktere testuji jine metody nez ktere jsou vstupnimi body do jejich konkretni vrstvy (jsou verejne)
   - testy ktere mockuji jine tridy nez @PrimaryPort / @SecondaryPort 
   - testy ktere mockuji tridy z vlastniho modulu oznac jako podezrele
   - testy ktere testuji business logiku a invarianty a ktere nejsou ciste Mockito testy oznac jako podezrele 
   - testy ktere mockuji datove objekty / domenove objekty / DTO
   - testy ktere mockuji vice nez 3 tridy
5. zobraz statistiky - pocet Mockito unit testu, pocet Spring slice testu, pocet application module testu a pocet SpringBootTest testu. 
4. over s uzivatelem kazdy podezrely test. Grill session pro zjisteni co s takovym testem delat. Zobraz uzivately detaily testu, popis co presne je v testu podezreleho, navrhni 1-3 zlepseni a nech uzivatele rozhodnout popr. navrhnout co s takovym testem dale delat. Sam nic nerozhoduj. Ptej se po jedne otazce. Posbirej zpetnou vazbu. 
5. Zobraz souhrn informaci od uzivatele a pozadej o zaverecne potvrzeni. 
6. po potvrzeni vytvor queue task pro upravy podezrelych testu podle uzivatelovych odpovedi. 