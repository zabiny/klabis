## 1. Logout is not overridden by automatic sign-in

- [x] 1.1 Write failing unit test in `frontend/src/contexts/AuthContext2.test.tsx`: when `userUnloaded`/`login()` happens while `logout()` is in progress, `signinRedirect` is not called and `signoutRedirect` is
- [x] 1.2 Write failing unit test: after `signoutRedirect` rejects, `login()` calls `signinRedirect` again
- [x] 1.3 Implement logout-in-progress guard in `AuthProvider` (`frontend/src/contexts/AuthContext2.tsx`), set before push unsubscribe, reset on logout error
- [x] 1.4 Remove explicit `getUser()`/`removeUser()`/`id_token_hint` from `logout()`; update existing tests mechanically (push-unsubscribe-before-signout assertions stay)
- [x] 1.5 Run frontend tests, lint and build; all green

## 2. Verification

- [x] 2.1 Playwright on http://localhost:3000: log in and log out at least 3× as ZBM9000 and 3× as ZBM9500; every logout ends on `https://localhost:8443/login` (no silent re-login)
- [x] 2.2 Confirm each logout reached `/connect/logout` (backend log / network requests) and no `/oauth2/authorize` request superseded it
- [x] 2.3 Code review of the change
- [x] 2.4 Refresh backend static resources (`npm run refresh-backend-server-resources`) and commit
