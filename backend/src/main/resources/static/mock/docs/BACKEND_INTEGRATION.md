# Klabis UI Mockup - Backend Integration

This document explains the OAuth2 Authorization Code integration with the Klabis backend API.

## ✅ What's Been Updated

### 1. API Client (`api-client.js`)

Complete OAuth2 Authorization Code implementation:

- **Token Management**: Automatic token storage, retrieval and refresh
- **Authorization Code Exchange**: `exchangeCodeForToken(code, redirectUri)` method
- **Token Refresh**: Automatic refresh token handling
- **Token Persistence**: Tokens stored in sessionStorage for page reloads
- **Error Handling**: Custom `ApiError` class for proper error responses
- **API Methods**:
    - `getMembers(page, size, sort)` - Paginated members list
    - `getMember(id)` - Member details
    - `createMember(data)` - Register new member
    - `getUserPermissions(id)` - Get user permissions
    - `updateUserPermissions(id, authorities)` - Update permissions

**Usage Example:**

```javascript
const token = await apiClient.getAccessToken();
const members = await apiClient.getMembers({ page: 0, size: 10, sort: 'lastName,asc' });
```

### 2. Login Page (`mock-login.html`)

Updated to use OAuth2 Authorization Code flow:

- **Backend URL**: `https://localhost:8443` (auto-detected)
- **Client ID**: `mock-web`
- **Redirect URI**: `https://localhost:8443/callback.html`

The login redirects to the authorization server's login page with the authorization code flow parameters.

### 3. Callback Page (`callback.html`)

New page to handle authorization server redirect:

- **Extracts authorization code** from URL query parameters
- **Exchanges code for tokens** via backend token endpoint
- **Stores tokens** in sessionStorage
- **Redirects to dashboard** after successful authentication

### 4. Members Page (`members.html`)

Now queries the real backend API:

- **Endpoint**: `GET /api/members`
- **Features**:
    - Pagination support from backend response
    - Sorting via backend parameters
    - Loading states
    - Error handling with proper user feedback
    - HAL+FORMS response parsing

### 5. App.js (`app.js`)

Updated utilities:

- `checkAuth()` - Verifies OAuth2 token validity, handles callback page
- `handleLogout()` - Clears tokens, session, and redirects to login
- `handleApiError(error)` - Centralized API error handling with token cleanup

## 🔧 Configuration

The API client can be configured via the login page:

```javascript
// UI is served from same origin as backend, so use relative URLs
apiClient.baseUrl = window.location.origin;  // https://localhost:8443
apiClient.apiBaseUrl = window.location.origin + '/api';  // https://localhost:8443/api
apiClient.clientId = 'mock-web';
apiClient.clientSecret = 'admin123';  // Used for token exchange only
apiClient.redirectUri = window.location.origin + '/callback.html';
```

## 📡 OAuth2 Authorization Code Flow

### 1. Authorization Request

User clicks login → Redirect to authorization server:

```http
GET https://localhost:8443/oauth2/authorize?
    response_type=code&
    client_id=mock-web&
    redirect_uri=https://localhost:8443/callback.html&
    scope=MEMBERS:CREATE+MEMBERS:UPDATE+MEMBERS:DELETE+MEMBERS:READ
```

### 2. User Authentication

Authorization server displays login page:

- User enters username and password
- Server validates credentials
- Server redirects back with authorization code

```http
HTTP/1.1 302 Found
Location: https://localhost:8443/callback.html?code=AUTHORIZATION_CODE
```

### 3. Token Exchange

Callback page exchanges code for access token:

```http
POST https://localhost:8443/oauth2/token
Authorization: Basic a2xhYmlzLXdlYjphZG1pbjEyMw==
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=AUTHORIZATION_CODE&
redirect_uri=https://localhost:8443/callback.html
```

### 4. Token Response

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 43199,
  "scope": "MEMBERS:CREATE MEMBERS:UPDATE MEMBERS:DELETE MEMBERS:READ"
}
```

### 5. Authenticated API Call

```http
GET https://localhost:8443/api/members?page=0&size=10&sort=lastName,asc
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/prs.hal-forms+json
```

### 6. Token Refresh (Automatic)

When access token expires, API client automatically refreshes:

```http
POST https://localhost:8443/oauth2/token
Authorization: Basic a2xhYmlzLXdlYjphZG1pbjEyMw==
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&
refresh_token=REFRESH_TOKEN
```

## 🔐 Security Notes

1. **Client Secret**: Used only for token exchange on backend
    - Client secret is sent via Basic Auth to token endpoint
    - Frontend never exposes secret to users during authentication
    - Authorization server validates client identity during token exchange

2. **Token Storage**: Tokens stored in sessionStorage
    - Automatically cleared when browser tab closes
    - Persisted across page reloads within same session
    - Manual logout clears all tokens

3. **Authorization Code Security**:
    - Short-lived authorization code (single-use, expires quickly)
    - Code can only be used by authenticated client (with client secret)
    - Prevents interception attacks compared to implicit flow

4. **PKCE Consideration**:
    - This implementation uses client secret for code exchange
    - For public clients (mobile/native apps), use PKCE (Proof Key for Code Exchange)
    - Current approach is suitable for confidential clients served from same origin

## 📋 Backend API Endpoints Used

| Method | Endpoint                      | Purpose                  | Status        |
|--------|-------------------------------|--------------------------|---------------|
| GET    | `/oauth2/authorize`           | Authorization endpoint   | ✅ Redirect    |
| POST   | `/oauth2/token`               | Get/refresh access token | ✅ Implemented |
| GET    | `/api/members`                | List members (paginated) | ✅ Implemented |
| GET    | `/api/members/{id}`           | Get member details       | ⏳ TODO        |
| POST   | `/api/members`                | Create member            | ⏳ TODO        |
| GET    | `/api/users/{id}/permissions` | Get permissions          | ⏳ TODO        |
| PUT    | `/api/users/{id}/permissions` | Update permissions       | ⏳ TODO        |

## 🧪 Testing

### Prerequisites

1. Start the Klabis backend (also serves the static UI):
   ```bash
   cd klabis-backend
   ./gradlew bootRun
   ```

2. Open: https://localhost:8443/mock-login.html

### Test Flow

1. **Login**: Enter Client ID (mock-web) and click login
2. **Authorization**: You'll be redirected to authorization server login page (default: `/login`)
3. **Authenticate**: Enter user credentials (admin / admin123)
4. **Callback**: Authorization server redirects back with authorization code
5. **Token Exchange**: Callback page exchanges code for tokens
6. **Dashboard**: Redirected to members list with real data

### Default Credentials

- **OAuth2 Client**: mock-web / test-secret-123
- **Admin User**: admin / admin123

## 🐛 Troubleshooting

### "Failed to exchange authorization code"

- ✅ Check backend is running on port 8443
- ✅ Verify OAuth2 client exists in database (V002 migration)
- ✅ Check redirect URI matches registered redirect URI
- ✅ Verify client ID and client secret are correct

### "401 Unauthorized"

- Token expired - should auto-refresh
- Refresh token expired - user must login again
- Client credentials incorrect - check clientId/clientSecret

### "403 Forbidden"

- User lacks required permissions
- Check OAuth2 scopes in token response
- Verify user has appropriate roles/authorities

### "Redirect URI mismatch"

- Ensure redirect URI matches exactly what's registered
- Check for trailing slashes, http vs https
- Default redirect URI: `https://localhost:8443/callback.html`

### CORS Errors

✅ **Not an issue** - UI is served from same origin as backend (https://localhost:8443), so no CORS configuration needed.

## 📚 Token Response Format

### Access Token Response

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 43199,
  "scope": "MEMBERS:CREATE MEMBERS:UPDATE MEMBERS:DELETE MEMBERS:READ"
}
```

### Members List (HAL+FORMS)

```json
{
  "_embedded": {
    "members": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "firstName": "Jan",
        "lastName": "Novák",
        "registrationNumber": "ZBM0501",
        "dateOfBirth": "2005-05-15",
        "active": true
      }
    ]
  },
  "_links": {
    "self": { "href": "/api/members?page=0&size=10" },
    "first": { "href": "/api/members?page=0&size=10" },
    "next": { "href": "/api/members?page=1&size=10" }
  },
  "page": {
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "number": 0
  }
}
```

## 🎯 Next Steps

1. ✅ **DONE**: API client with OAuth2 Authorization Code
2. ✅ **DONE**: Login page with authorization redirect
3. ✅ **DONE**: Callback page with code exchange
4. ✅ **DONE**: Token storage and refresh
5. ✅ **DONE**: Members list with real data
6. ⏳ **TODO**: Member form (create member via API)
7. ⏳ **TODO**: Member details (fetch from API)
8. ⏳ **TODO**: Permissions management (fetch/update users)
9. ⏳ **TODO**: Dashboard stats (calculate from real data)

The pattern is established - follow the same approach for remaining pages!
