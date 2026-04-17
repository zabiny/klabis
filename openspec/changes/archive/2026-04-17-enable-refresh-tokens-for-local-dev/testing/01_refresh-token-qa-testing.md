# Refresh Token Local-Dev — QA Testing

## Scenarios

### 7. Manual QA — local-dev refresh token flow

- [ ] **RT-1**: Přihlášení admina na http://localhost:3000 proběhne úspěšně s local-dev profilem
- [ ] **RT-2**: sessionStorage klíč `oidc.user:http://localhost:3000/:klabis-web-local` obsahuje neprázdný `refresh_token`
- [ ] **RT-3**: Manuální `signinSilent()` spustí POST /oauth2/token s `grant_type=refresh_token`, vrátí 200, a aktualizuje sessionStorage s novým access_token
- [ ] **RT-4**: Při signinSilent nevznikne žádný iframe request na `/oauth2/authorize?...&prompt=none`
- [ ] **RT-5**: Žádné console errory týkající se X-Frame-Options nebo silent renewal failures
- [ ] **RT-6**: Logout funguje korektně

---

## Results

### Iteration 1 (oprava: chybějící postLogoutRedirectUri pro klabis-web-local)

| Scenario | Result | Note |
|----------|--------|------|
| RT-1 | PASS | Přihlášení admina na http://localhost:3000 funguje |
| RT-2 | PASS | sessionStorage klíč `oidc.user:http://localhost:3000/:klabis-web-local` obsahuje `refresh_token` (délka 128 znaků) |
| RT-3 | PASS | POST /oauth2/token s `grant_type=refresh_token` vrátil 200, nový `access_token` + `refresh_token` (rotace) |
| RT-4 | PASS | Žádný GET /oauth2/authorize?prompt=none při renewal, pouze POST /oauth2/token |
| RT-5 | PASS | Žádné X-Frame-Options errory; automaticSilentRenew použil refresh_token flow |
| RT-6 | PASS (po fix) | Logout funguje po přidání `postLogoutRedirectUri=http://localhost:3000` pro `klabis-web-local` |

**Bug nalezen a opraven:** `createLocalDevConfidentialClient()` neměl nastavené `postLogoutRedirectUris` → logout endpoint vracel 400 `invalid_request`. Opraveno přidáním `.postLogoutRedirectUris(items -> items.add("http://localhost:3000"))`.
