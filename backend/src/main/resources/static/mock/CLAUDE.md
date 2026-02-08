# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the **static UI mockup** in this
directory.

## Overview

This folder contains a **static HTML/CSS/JavaScript UI mockup** for the Klabis membership management system. It
demonstrates the frontend user interface and integrates with the Klabis backend API via **OAuth2 Authorization Code**
flow.

**Purpose**: Frontend reference implementation and UI/UX prototype for the Klabis system.

**Language**: Czech (česky) - all UI text is in Czech to match target users.

## Running the UI Mockup

### Start the UI

The UI mockup is served directly by Spring Boot - **no separate frontend server needed**.

```bash
# Start the Spring Boot backend (serves both API and static UI files)
cd klabis-backend
BOOTSTRAP_ADMIN_USERNAME='admin' \
BOOTSTRAP_ADMIN_PASSWORD='admin123' \
OAUTH2_CLIENT_SECRET='test-secret-123' \
./gradlew bootRun
```

Then open your browser: **https://localhost:8443/mock-login.html**

### Prerequisites

1. **Backend running**: With environment variables set (above)
2. **UI accessible**: https://localhost:8443/mock-login.html
3. **OAuth2 Client**: `mock-web` with secret `test-secret-123`

**API documentation** can be found [here](../../../../docs/API.md)

### Environment Variables

**Required environment variables for development:**

| Variable                   | Value             | Purpose                                           |
|----------------------------|-------------------|---------------------------------------------------|
| `BOOTSTRAP_ADMIN_USERNAME` | `admin`           | Admin user login                                  |
| `BOOTSTRAP_ADMIN_PASSWORD` | `admin123`        | Admin user password                               |
| `OAUTH2_CLIENT_SECRET`     | `test-secret-123` | OAuth2 client secret (must match `api-client.js`) |

**⚠️ IMPORTANT**: Always set these variables when starting the server.

**If not set**, backend generates random credentials on each startup:

- You must check logs for new credentials
- Update `api-client.js` with new secret
- Clear browser cache and restart

**Example:** See [Start the UI](#start-the-ui) section above for complete command.

## File Structure

### Pages

- **index.html** - Dashboard with statistics and quick actions
- **mock-login.html** - OAuth2 Authorization Code login (redirects to authorization server)
- **callback.html** - OAuth2 callback page (handles authorization code exchange)
- **members.html** - Members list with pagination and sorting (connected to backend)
- **member-details.html** - Single member details view
- **member-form.html** - Create new member form
- **permissions.html** - User permissions management

### Shared Files

- **styles.css** - Common styles (CSS variables, responsive design)
- **app.js** - Shared utilities (auth, formatting, validation, toast notifications)
- **api-client.js** - OAuth2 Authorization Code API client with token management

### Documentation

- **README.md** - UI mockup overview (Czech)
- **BACKEND_INTEGRATION.md** - OAuth2 integration details
- **CLAUDE.md** - This file

## Architecture & Patterns

### Tech Stack

- **Vanilla JavaScript** (no frameworks)
- **Modern CSS** (CSS variables, Flexbox, Grid)
- **HTML5** semantic markup
- **OAuth2 Authorization Code** flow for backend authentication

### API Integration

**OAuth2 Authorization Code Flow:**

```javascript
// api-client.js handles token lifecycle
// Note: UI is served from same origin as backend (https://localhost:8443)
const apiClient = new KlabisApiClient({
    baseUrl: window.location.origin,  // https://localhost:8443
    clientId: 'klabis-web',
    clientSecret: 'test-secret-123',  // Must match OAUTH2_CLIENT_SECRET env variable
    redirectUri: window.location.origin + '/callback.html'
});

// Token is automatically fetched via authorization code flow
// Token auto-refresh when expired
const members = await apiClient.getMembers({ page: 0, size: 10, sort: 'lastName,asc' });
```

**API Response Format (HAL+FORMS):**

```json
{
  "_embedded": {
    "members": [...]
  },
  "_links": {
    "self": { "href": "/api/members?page=0&size=10" },
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

### Authentication Flow

1. User enters Client ID on `mock-login.html`
2. User is redirected to authorization server's login page (`/oauth2/authorize`)
3. User authenticates with username and password (e.g., admin / admin123)
4. Authorization server redirects back to `callback.html` with authorization code
5. Callback page exchanges authorization code for access token via `apiClient.exchangeCodeForToken()`
6. Access token and refresh token stored in `sessionStorage`
7. User redirected to dashboard (`index.html`)
8. Token auto-refreshed when expired using refresh token
9. Logout clears all tokens and session

### Error Handling

```javascript
// Custom ApiError class with helper methods
try {
    const members = await apiClient.getMembers();
} catch (error) {
    if (error.isUnauthorized()) {
        // Redirect to login
    } else if (error.isValidationError()) {
        // Show validation error
    }
    handleApiError(error); // Centralized error handling
}
```

## Coding Conventions

### JavaScript

- **ES6+ syntax**: Classes, async/await, arrow functions
- **JSDoc comments**: Document functions with `@param`, `@returns`
- **Error handling**: Always use try/catch with API calls
- **Event listeners**: Use `DOMContentLoaded` for initialization
- **Global namespace**: Export utilities via `window.KlabisUI`

**Example function pattern:**

```javascript
/**
 * Format date to Czech locale
 * @param {Date|string} date - The date to format
 * @returns {string} Formatted date string
 */
function formatDate(date) {
    if (typeof date === 'string') {
        date = new Date(date);
    }
    return date.toLocaleDateString('cs-CZ');
}
```

### HTML

- **Semantic HTML5**: Use `<nav>`, `<main>`, `<aside>`, `<header>`
- **Czech lang attribute**: `<html lang="cs">`
- **Accessibility**: ARIA labels where needed, semantic markup
- **Consistent structure**: Sidebar navigation + main content area

### CSS

- **CSS variables** for theming (defined in `:root`)
- **Mobile-first** responsive design
- **Flexbox and Grid** for layouts
- **BEM-ish naming**: `.stat-card`, `.stat-card__title`, `.stat-card--active`

**Colors:**

- Primary: `#4F46E5` (indigo)
- Success: `#10B981` (green)
- Warning: `#F59E0B` (orange)
- Danger: `#EF4444` (red)

## Key Features

### 1. Members List (`members.html`)

- **Backend-connected**: Fetches from `GET /api/members`
- **Pagination**: Page size, current page, total pages
- **Sorting**: By lastName, firstName, registrationNumber
- **Loading states**: Spinner during API calls
- **Error handling**: Toast notifications for API errors

### 2. Member Form (`member-form.html`)

- **Conditional fields**:
    - Rodné číslo (birth number): Only for Czech nationality
    - Legal guardian: Auto-shows when age < 18
- **Validation**: Real-time client-side validation
- **Required fields**: At least 1 email + 1 phone

### 3. Authentication

- **Page protection**: `checkAuth()` redirects to login if not authenticated
- **Active nav highlighting**: Auto-highlights current page in sidebar
- **User info display**: Shows admin user in sidebar footer

### 4. Utilities (app.js)

- **Date formatting**: `formatDate()`, `formatDateTime()` (Czech locale)
- **Gender mapping**: `getGenderText('MALE')` → `'Muž'`
- **Phone validation**: Czech phone format validation
- **Email validation**: Standard email regex
- **Birth number validation**: Czech rodné číslo format
- **Debounce utility**: For search inputs
- **Toast notifications**: `showToast(message, type)`

## Backend API Integration Status

| Feature          | Status    | Notes                                      |
|------------------|-----------|--------------------------------------------|
| OAuth2 Login     | ✅ Working | Authorization Code flow with token refresh |
| Members List     | ✅ Working | Pagination, sorting                        |
| Member Details   | ⏳ TODO    | Need to fetch from API                     |
| Create Member    | ⏳ TODO    | Need to POST to API                        |
| User Permissions | ⏳ TODO    | Need to fetch/update from API              |

## Common Tasks

### Adding a New Page

1. Create HTML file following existing structure (sidebar + main content)
2. Include `<script src="app.js"></script>` and `<script src="api-client.js"></script>`
3. Add navigation link to sidebar on all pages
4. Use `checkAuth()` for protected pages
5. Follow Czech language convention

### Connecting a Page to Backend

1. Import api-client.js: `<script src="api-client.js"></script>`
2. Use async/await for API calls
3. Handle errors with try/catch and `handleApiError(error)`
4. Show loading states during API calls
5. Display results in DOM

```javascript
document.addEventListener('DOMContentLoaded', async () => {
    try {
        showLoading();
        const data = await apiClient.getMembers({ page: 0, size: 10 });
        renderData(data);
    } catch (error) {
        handleApiError(error);
    } finally {
        hideLoading();
    }
});
```

### Adding Form Validation

1. Use existing validation utilities: `validateEmail()`, `validateCzechPhone()`
2. Show errors via `showToast(message, 'error')`
3. Prevent submission with `event.preventDefault()` if invalid
4. Use HTML5 validation attributes (`required`, `pattern`)

### Updating Styles

1. Modify CSS variables in `:root` for global changes
2. Use existing utility classes (`.stat-card`, `.btn-link`, etc.)
3. Maintain responsive design with `@media` queries
4. Test on mobile viewport (375px width)

## Differences from Backend Code

This UI mockup is **different** from the backend Java/Spring code:

| Aspect            | UI Mockup                   | Backend                          |
|-------------------|-----------------------------|----------------------------------|
| **Language**      | HTML/CSS/JavaScript         | Java 17+                         |
| **Type**          | Static files                | Spring Boot application          |
| **Testing**       | Manual browser testing      | JUnit 5, integration tests       |
| **Build**         | None (run as-is)            | Gradle (`./gradlew clean build`) |
| **Location**      | `src/main/resources/static` | `src/main/java/com/klabis/`      |
| **Auth**          | OAuth2 Authorization Code   | OAuth2 Authorization Server      |
| **Documentation** | This file + README.md       | CLAUDE.md in backend root        |

**Do NOT use backend CLAUDE.md instructions for this UI code.**

## Testing

### Manual Testing Checklist

- [ ] Login works with correct credentials
- [ ] Invalid credentials show error
- [ ] Members list loads from backend
- [ ] Pagination works
- [ ] Sorting works
- [ ] Loading states display
- [ ] Errors show toast notifications
- [ ] Logout clears session and redirects
- [ ] All pages accessible from sidebar
- [ ] Mobile responsive (375px viewport)

### Backend Integration Testing

```bash
# 1. Start backend with required environment variables
cd klabis-backend
BOOTSTRAP_ADMIN_USERNAME='admin' \
BOOTSTRAP_ADMIN_PASSWORD='admin123' \
OAUTH2_CLIENT_SECRET='test-secret-123' \
./gradlew bootRun

# 2. Open browser to the login page
# Browser: https://localhost:8443/mock-login.html

# 3. Test OAuth2 Authorization Code flow
# - At "Enter Client ID": use `klabis-web`
# - Username: `admin`
# - Password: `admin123`
```

## Security Notes

⚠️ **Development Only - Not Production Ready**

- **Client secret used**: `admin123` is used for token exchange (not exposed to users)
- **Token storage**: In sessionStorage (persisted across page reloads, cleared on tab close)
- **Authorization code flow**: More secure than implicit or client credentials flows
- **Same origin**: UI served from backend (no CORS issues in dev)
- **No CSRF protection**: Static files don't have CSRF tokens

**Production deployment** would require:

- PKCE flow for public clients (mobile/native apps)
- Proper SPA framework (React, Vue, Angular)
- Build process and minification
- CSRF token handling
- Secure token storage (consider httpOnly cookies or secure storage patterns)

## Related Documentation

- **Backend API**: See `klabis-backend/CLAUDE.md` for backend architecture
- **API Documentation**: See `klabis-backend/docs/API.md` for endpoint details
- **OpenSpec Specs**: See `../../openspec/specs/` for feature specifications
- **Integration Details**: See `BACKEND_INTEGRATION.md` in this directory

## Troubleshooting

### "Failed to get access token"

- ✅ Verify backend is running: `curl -k https://localhost:8443/actuator/health`
- ✅ Check OAuth2 credentials in `api-client.js` match environment variables
- ✅ Verify `OAUTH2_CLIENT_SECRET='test-secret-123'` when starting backend

### CORS Errors

- ✅ Not an issue: UI served from same origin as backend (https://localhost:8443)

### Members Not Loading

- Check browser console (F12) for error messages
- Verify backend API is accessible: `curl -k https://localhost:8443/api/members`
- Check OAuth2 token is valid in browser dev tools (Application → sessionStorage)

### Session Expired / Token Invalid

- Token expires after ~12 hours (with 60s buffer before expiry)
- Should auto-refresh on next API call using refresh token
- If not working: logout and login again
