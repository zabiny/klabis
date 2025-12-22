### Uprava zobrazovani formularu
1. collection page - jako dosud
2. HalGenericPage: pokud ma URL '#' cast, tak se zobrazi _template formular s danym nazvem. Pokud takova _template neexistuje popr. URL nema '#' cast, tak se zobrazi readonly verze daneho resource. (toto krasne vyresi "self" formulare) 

3. pokud uzivatel klikne na akci vytvorenou z template ktera ma jinou URL nez je 'self', tak aplikace presmeruje na cilovou URL + '#' formulare. Pokud item page zobrazuje formular ('#') a GET vrati 404/405 status, tak zobrazi formular z


###  Sloucit akce
- zobraz akce z _links a _templates v jedne sekci. 
- vytvor dve komponentu pro custom stranky ktera jim umozni zobrazovat conditional Link nebo Button podle toho zda takova akce existuje v _links a _templates. Komponenta dostane nazev akce. Pokud takova akce (rel _link nebo nazev _templates) existuje, tak zobrazi Link popr. Button. Pokud neexistuje, tak nezobrazi nic. 


### Backend
- pokud se dela reference na formular pro jiny link nez SELF, tak se vytvori pouze link kde se na konec URL prida '#' cast s nazvem formulare (= metody). 
	- co "Create new member" ?? -> ten muze mit konecne URL s aktualni cestou daneho resource = POST /members. 
	- bude nejaky pripad kdy budeme potrebovat vice "create new" akci pro "prazdny" resource? (= jde o to aby "create new" fungoval bez "GET" endpointu). 
	- >>>> a co takhle na backendu udelat formulare take s '#' v URL? <<<<
		- pry to neni dobry napad: https://share.google/aimode/esUmfPYOnGBWPfLcR 
- pokud se dela reference na formular pro aktualni link, tak se vytvori plna definice affordance. 

### AI 
RESTfull API by melo byt dobre porozumitelne pro AI agenty. Zkusit odemcit Klabis API (zrusit auth) a dat tu URL nejakemu AI a pak se ho zeptat: kolik ma klub clenu? Jake jsou eventy tento tyden? Prihlas mne na zavod .. 
