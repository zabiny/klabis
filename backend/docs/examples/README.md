# API Examples

This directory contains practical examples for using the Klabis API.

## Files

### Shell Scripts

#### `complete-workflow.sh`

Complete workflow demonstrating member registration and password setup.

- Authenticates as admin
- Creates a new member
- Lists members
- Gets member details
- Requests password setup
- Refreshes access token

**Usage:**

```bash
./complete-workflow.sh
```

**Requirements:**

- `curl` - HTTP client
- `jq` (optional) - JSON parser (fallback to grep if not available)

#### `bulk-import.sh`

Script to import multiple members from a JSON file.

**Usage:**

```bash
./bulk-import.sh members.json
```

**Requirements:**

- `curl` - HTTP client
- `jq` - Required for JSON parsing (https://stedolan.github.io/jq/)

### JavaScript

#### `rate-limit-client.js`

JavaScript client demonstrating proper rate limit handling.

- Handles 429 Too Many Requests responses
- Implements exponential backoff
- Respects Retry-After headers
- Includes error handling

**Usage:**

```bash
node rate-limit-client.js
```

**Or as a module:**

```javascript
const { requestPasswordSetupToken } = require('./rate-limit-client.js');
await requestPasswordSetupToken('user@example.com');
```

**Requirements:**

- Node.js 14+
- `fetch` API (built-in in Node.js 18+)

### HTTP Files

#### `member-management.http`

Member Management API scenarios for testing CRUD operations.

- Create member with various field combinations
- Validation error tests
- List, get, update, and delete operations
- Pagination and sorting tests

**Usage:**

- Open in IntelliJ IDEA with the HTTP Client plugin
- Or use with other HTTP client tools like VS Code REST Client
- Requires `http-client.env.json` configured in project root
- Uses `ClientCredentials` OAuth2 authentication

#### `password-setup.http`

Password Setup API scenarios for testing the password setup flow.

- Request password setup token
- Validate token
- Complete password setup
- Error scenarios (mismatched passwords, weak passwords, rate limiting)

**Usage:**

- Open in IntelliJ IDEA with the HTTP Client plugin
- Note: These endpoints do NOT require authentication

#### `user-permissions.http`

User Permission Management API scenarios for testing permission management.

- Get user permissions
- Update user permissions
- Error scenarios (invalid authorities, empty authorities, last admin protection)

**Usage:**

- Open in IntelliJ IDEA with the HTTP Client plugin
- Requires `MEMBERS:PERMISSIONS` authority
- Uses `ClientCredentials` OAuth2 authentication

### Data Files

#### `members.json`

Example data file for bulk import script.

Contains 5 sample members with various field combinations:

- Minimal required fields
- Optional fields (birthPlace, notes, nationalIdNumber)
- Different nationalities (CZ, SK)

## Configuration

All examples use the following default configuration:

- **Backend URL:** `https://localhost:8443` (override with `BACKEND_URL` env var)
- **OAuth2 Client:** `klabis-web` / `test-secret-123`
- **Admin User:** `admin` / `admin123`

### Environment Variables

```bash
export BACKEND_URL=https://localhost:8443
export BOOTSTRAP_ADMIN_PASSWORD=admin123
export OAUTH2_CLIENT_SECRET=test-secret-123
```

## API Endpoints Used

- **POST** `/oauth2/token` - Get access token
- **POST** `/api/members` - Create member
- **GET** `/api/members` - List members
- **GET** `/api/members/{registrationNumber}` - Get member
- **PATCH** `/api/members/{registrationNumber}` - Update member
- **POST** `/api/auth/password-setup/request` - Request password setup
- **GET** `/api/auth/password-setup/validate` - Validate token
- **POST** `/api/auth/password-setup/complete` - Complete password setup

## Common Operations

### Quick Test

```bash
# Get token and create member in one command
curl -k -X POST https://localhost:8443/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "klabis-web:test-secret-123" \
  -d "grant_type=client_credentials&scope=MEMBERS:CREATE" \
  | grep -o '"access_token":"[^"]*' | cut -d'"' -f4 \
  | { read TOKEN; curl -k -X POST https://localhost:8443/api/members \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Jan","lastName":"Novak","gender":"MALE","nationality":"CZ","dateOfBirth":"1990-01-15","address":{"street":"Test 123","city":"Praha","zipCode":"11000","country":"CZ"},"contact":{"email":"jan@test.cz","phone":"+420123456789"}}'; }
```

### Bulk Import

```bash
./bulk-import.sh members.json
```

### Complete Workflow

```bash
./complete-workflow.sh
```

## Rate Limiting

The API implements rate limiting on certain endpoints (e.g., password setup).

**For complete rate limiting documentation**, see [../API.md#rate-limiting](../API.md#rate-limiting)

## Error Handling

All errors follow RFC 7807 Problem Details format:

```json
{
  "type": "https://klabis.com/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields"
}
```

## Best Practices

1. **Use HTTPS** in production
2. **Store tokens securely** - never expose in client-side code
3. **Respect rate limits** - implement retry logic
4. **Use pagination** for large collections
5. **Handle errors gracefully** - check status codes
6. **Cache responses** - avoid duplicate requests

## Additional Resources

- [Main API Documentation](../API.md)
- [Rate Limiting Guide](../API.md#rate-limiting)
- [Error Handling](../API.md#error-handling)
- [Authentication Guide](../API.md#authentication)

## Support

For issues or questions:

1. Check the main [API.md](../API.md) documentation
2. Review the example files in this directory
3. Check server logs for detailed error messages
