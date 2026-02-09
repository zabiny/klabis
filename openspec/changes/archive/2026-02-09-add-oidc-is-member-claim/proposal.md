## Why

Frontend potřebuje rozlišit dva typy uživatelů v OIDC userinfo endpointu: plnohodnotné členy (User + Member agregát) a správce (pouze User agregát). Toto rozlišení umožní frontendu zobrazovat uživatelské jméno správně - u členů zobrazí celé jméno a odkaz na detail, u správců pouze username bez odkazu.

## What Changes

- Přidat nový OIDC claim `is_member: boolean` do `/userinfo` response (pouze při scope `profile`)
- Přejmenovat OIDC claim `registrationNumber` na `user_name` (v ID tokenu, access tokenu i userinfo)
- Claim `user_name` vrácen pouze při scope `profile` (konzistentně s ostatními profile claims)
- Detekce členství: kontrola existence Member agregátu pro daný username

## Capabilities

### New Capabilities
- `oidc-member-detection`: Detekce členství uživatele v OIDC userinfo response pomocí nového `is_member` claim

### Modified Capabilities
- `oidc-userinfo`: Změna claim names (`registrationNumber` → `user_name`) a přidání `is_member` claim do response

## Impact

**Kód:**
- `AuthorizationServerConfiguration.java` - úprava `oidcUserInfoMapper()`, `jwtCustomizer()`
- Testy: `OidcUserInfoEndpointTest`, `OidcFlowE2ETest`, `OidcIdTokenGenerationTest`

**API:**
- **BREAKING**: OIDC `/userinfo` endpoint - claim `registrationNumber` přejmenován na `user_name`
- **BREAKING**: ID token a access token - claim `registrationNumber` přejmenován na `user_name`
- Nový claim `is_member: boolean` v userinfo response (pouze s `profile` scope)

**Frontend:**
- Frontend musí aktualizovat parsování OIDC claims (`registrationNumber` → `user_name`)
- Frontend může využít `is_member` claim pro rozhodování o zobrazení UI (odkaz na member detail)

**Dokumentace:**
- Aktualizace `docs/examples/oidc-authentication.http` s novými příklady response
