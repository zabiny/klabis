# Klabis backend

## Spusteni na localhost

Aplikace ma nakonfigurovane prostredi pomoci [TestContainers](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testcontainers.at-development-time). Pro spusteni je potreba mit nainstalovany docker. Spusteni je pak mozne pomoci prikazu 
```shell
gradlew bootTestRun
```


Alternativne lze aplikaci spolecne s TestContainers spustit pomoci java main tridy `club.klabis.KlabisAppWithTestContainers` 

# DevOps

## Autorizacni server

### Vygenerovani noveho klice pro JWT tokeny
`club.klabis.config.authserver.generatejwtkeys.JKWKeyGenerator` 