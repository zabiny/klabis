# IntelliJ IDEA Setup Guide

Tento projekt obsahuje jak specifikace (OpenSpec) tak backend kód (Gradle Spring Boot projekt).

## Otevření projektu v IntelliJ IDEA

### Varianta 1: Otevřít root directory (Doporučeno)

1. **File → Open** → vyberte `klabisSpecs/` adresář
2. IntelliJ automaticky detekuje Gradle modul `klabis-backend/`
3. Počkejte na Gradle import a indexování

### Varianta 2: Import jako Gradle projekt

1. **File → Open** → vyberte `klabisSpecs/klabis-backend/build.gradle.kts`
2. Vyberte **"Open as Project"**

## Po otevření projektu

### 1. Nastavení JDK

Ujistěte se, že máte nastavený JDK 17+:

1. **File → Project Structure** (Ctrl+Alt+Shift+S)
2. **Project** → **Project SDK** → vyberte JDK 17 nebo vyšší
3. **Project language level** → `17 - Sealed types, always-strict floating-point semantics`

### 2. Enable Annotation Processing

Pro Lombok a MapStruct:

1. **File → Settings** (Ctrl+Alt+S)
2. **Build, Execution, Deployment → Compiler → Annotation Processors**
3. ✅ **Enable annotation processing**
4. **Store generated sources relative to**: `Module content root`
5. **Production sources directory**: `build/generated/sources/annotationProcessor/java/main`
6. **Test sources directory**: `build/generated/sources/annotationProcessor/java/test`

### 3. Lombok Plugin

1. **File → Settings → Plugins**
2. Hledejte "Lombok"
3. Nainstalujte **Lombok Plugin** (pokud není nainstalován)
4. Restart IntelliJ

### 4. jMolecules bytebuddy plugin

> jMolecules bytebuddy plugin je nakonfigurován v `build.gradle.kts` a spouští se automaticky jako součást Gradle buildu. V IntelliJ IDEA by měl fungovat bez dalšího nastavení při použití Gradle delegace.

### 5. Gradle Reload

Pokud Gradle dependencies nejsou stažené:

1. Klikněte pravým tlačítkem na `build.gradle.kts`
2. **Gradle → Reload project**

Nebo použijte Gradle panel:
- **View → Tool Windows → Gradle**
- Klikněte na ikonu "Reload All Gradle Projects" (↻)

## Run Configurations

Projekt obsahuje předpřipravené run configurations:

### KlabisBackendApplication

Spustí Spring Boot aplikaci v **dev** profilu (H2 database):

- **Main class**: `com.klabis.KlabisBackendApplication`
- **Active profiles**: `dev`
- **Environment variables**:
  - `KLABIS_CLUB_CODE=ZBM`
  - `KLABIS_JASYPT_PASSWORD=dev-secret`

**Spuštění**: Toolbar → vyberte "KlabisBackendApplication" → Run (▶️)

### All Tests

Spustí všechny JUnit testy:

- **Test scope**: Celý package
- **Active profiles**: `test`

**Spuštění**: Toolbar → vyberte "All Tests" → Run (▶️)

## Struktura projektu v IntelliJ

```
klabisSpecs/                    (Root modul)
├── openspec/                   (Specifikace)
│   ├── project.md              (Projektová dokumentace)
│   ├── specs/                  (Aktuální specifikace)
│   └── changes/                (Změnové návrhy)
│       └── add-member-registration/
└── klabis-backend/             (Gradle modul)
    ├── src/
    │   ├── main/java/com/klabis/
    │   │   ├── members/        (DDD Bounded Context)
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   ├── infrastructure/
    │   │   │   └── presentation/
    │   │   ├── common/
    │   │   └── config/
    │   ├── main/resources/
    │   │   ├── application.yml
    │   │   └── db/migration/
    │   └── test/java/com/klabis/
    └── build.gradle.kts
```

## Užitečné IntelliJ funkce

### Gradle Panel

**View → Tool Windows → Gradle**

- **Tasks → build**: classes, build, clean
- **Tasks → application**: bootRun

### Database Panel (pro PostgreSQL)

1. **View → Tool Windows → Database**
2. **+ → Data Source → PostgreSQL**
3. Nastavte connection:
   - Host: `localhost`
   - Port: `5432`
   - Database: `klabis`
   - User: `klabis`
   - Password: (z environment variables)

### HTTP Client (pro testování API)

1. Vytvořte soubor `test-api.http` v projektu
2. Použijte IntelliJ HTTP Client pro testování endpointů

Příklad:
```http
### Get API root
GET http://localhost:8080/api
Accept: application/hal+json

### Create member
POST http://localhost:8080/api/members
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "firstName": "Jan",
  "lastName": "Novák",
  "dateOfBirth": "2005-03-15",
  "nationality": "CZ"
}
```

## Profily Spring Boot

### dev (výchozí)
- H2 in-memory database
- H2 Console: http://localhost:8080/h2-console
- SQL logging zapnuté

### test
- Použito při spuštění testů
- H2 in-memory database
- Flyway vypnutý

### prod
- PostgreSQL database
- SQL logging vypnuté
- Flyway migrations zapnuté

## Troubleshooting

### "Cannot resolve symbol" pro Lombok

1. Zkontrolujte, že Lombok plugin je nainstalován
2. **File → Invalidate Caches → Invalidate and Restart**
3. Maven Reload

### "Cannot find ... bean" a trida chybejici bean je anotovana jako jMolecules Service / Repository

1. Zkontrolujte ze je v IDEA nastaveno spusteni bytebuddy plugin (viz step 4 "Po otevreni projektu")

### Gradle dependencies se nestahují

1. **File → Settings → Build Tools → Gradle**
2. Zkontrolujte **Gradle user home** a **Distribution**
3. **Gradle → Reload project**

### Tests neprocházejí

1. Ujistěte se, že používáte profil `test`
2. Zkontrolujte `application-test.yml` konfiguraci
3. Vyčistěte build: `./gradlew clean`

### Port 8080 už je používán

Změňte port v `application.yml`:
```yaml
server:
  port: 8081
```

## Klávesové zkratky

- **Ctrl+Shift+F10**: Run aktuální test/main class
- **Shift+F10**: Run poslední konfiguraci
- **Ctrl+Shift+T**: Přepnout mezi třídou a testem
- **Alt+Insert**: Generate (v třídě → test)
- **Ctrl+Alt+L**: Reformat code

## Další kroky

Po úspěšném otevření projektu:

1. Spusťte `KlabisBackendApplication` a ověřte, že aplikace startuje
2. Spusťte `All Tests` a ověřte, že smoke test prochází
3. Začněte s implementací podle `openspec/changes/add-member-registration/tasks.md`

Happy coding! 🚀
