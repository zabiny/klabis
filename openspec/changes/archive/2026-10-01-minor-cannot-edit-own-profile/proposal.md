## Why

Nezletilý člen má dnes na vlastním profilu stejné možnosti úprav jako dospělý. Údaje nezletilého spravují zákonní zástupci a administrátor, dítě by je měnit nemělo.

## What Changes

- Nezletilý člen (do 18 let) nemůže upravovat svůj vlastní profil: na detailu se mu nezobrazí akce „Upravit“ a backend jeho pokus o úpravu odmítne.
- Úpravy údajů nezletilého provádí pouze uživatel s oprávněním MEMBERS:UPDATE (administrátor).
- Dospělí členové se nemění.
- Po dosažení 18 let se vlastní úprava profilu automaticky obnoví.

## Capabilities

### New Capabilities

### Modified Capabilities
- `members`: požadavek „Member Update“ – vlastní úprava profilu se vztahuje jen na dospělé členy; nezletilého upravuje pouze administrátor.

## Impact

- Backend: `MemberController` (affordance `updateMember` na detailu), `ManagementService.updateMember` (odmítnutí vlastní úpravy nezletilého), případně `x-klabis-owner-visible` na `updateMember` ve specifikaci `docs/openapi/spec/members.yaml`.
- Frontend: akce „Upravit“ se řídí HAL-FORMS affordance, kód by se měl změnit minimálně.
- Testy: controller testy a testy služby pro úpravu vlastního profilu nezletilého.
