# Klabis backend

## Spusteni na localhost

Aplikace ma nakonfigurovane prostredi pomoci [TestContainers](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testcontainers.at-development-time). Pro spusteni je potreba mit nainstalovany docker. Spusteni je pak mozne pomoci prikazu 
```shell
gradlew bootTestRun
```

Alternativne lze aplikaci spolecne s TestContainers spustit pomoci java main tridy `club.klabis.KlabisAppWithTestContainers` 

Aplikace je po spusteni dostupna na URL: https://localhost:8443 (URL pro Swagger API dokumentaci je https://localhost:8443/swagger-ui/index.html, po prihlaseni pomoci "Autorize" tlacitka je mozne testovat API pod danym uzivatelem - ponechejte predvyplnene ClientID a ClientSecret)

### IntelliJ IDEA

Po importu repozitare to IntelliJ IDEA budou pridany i Run configurations:
- `Klabis Backend (TestContainers)` - doporuceny zpusob spusteni: spusteni aplikace spolu s Test containers (vyzaduje nainstalovany docker).
- `Klabis Backend` - spusteni aplikace BEZ test containers
- `Run Klabis Docker image` - spusteni aplikace z docker image (musi byt jiz vybuildovan)

Pro spusteni aplikace pomoci techto predpripravenych konfiguraci je potreba pripravit soubor `local.env`. Zkopirujte `local.env.example` a upravte pokud je potreba provest nejake zmeny

## DevOps

### Konfigurace - promenne prostredi (environment variables)
- `GOOGLE_CLIENT_ID`: OAuth2 google client ID pro prihlaseni pres Google
- `GOOGLE_CLIENT_SECRET`: OAuth2 google client secret pro prihlaseni pres Google

### Autorizacni server

#### Vygenerovani noveho klice pro JWT tokeny
`club.klabis.shared.config.generatejwtkeys.authserver.config.JKWKeyGenerator`

#### Import certifikatu pro HTTPS
Certifikat v repozitari je self-signed. 

Pro import jineho certifikatu ze souboru (napr. `myCertificate.crt`) je mozne pouzit tento prikaz : 
```shell
keytool -import -alias klabisSSL -file myCertificate.crt -keystore backend/src/main/resources/https/keystore.p12 -storepass secret
```

Additional info for example [here](https://www.thomasvitale.com/https-spring-boot-ssl-certificate/)

#### Konverze Let's Encryt certificatu na `.p12`

Pro konverzi  `.pem` certifikatu vygenerovanych let's encrypt certbotem do `.p12` souboru pro backend by melo byt mozne pouzit nasledujici prikaz:  

```shell
openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out backend/src/main/resources/https/keystore.p12 -name klabisSSL -CAfile chain.pem -caname root -password secret
```
