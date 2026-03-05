# HATEOAS and HAL+FORMS Guide

## Overview

This API implements HATEOAS (Hypermedia as the Engine of Application State) using the HAL+FORMS media type (
`application/prs.hal-forms+json`). This approach provides self-documenting, discoverable REST APIs where clients
navigate through resources using links rather than hardcoded URLs.

## What is HATEOAS?

HATEOAS is a constraint of REST architecture that says a client should interact with a REST API entirely through
hypermedia provided dynamically by application servers. The client doesn't need prior knowledge of the API structure.

## HAL+FORMS Media Type

HAL+FORMS extends HAL (Hypertext Application Language) with form templates that describe how to construct requests to
modify resources.

### Media Type Header

Always include the media type in your requests:

```http
Accept: application/prs.hal-forms+json
Content-Type: application/json
```

## Response Structure

A typical HAL+FORMS response includes:

1. **Resource data** - The actual resource properties
2. **`_links`** - Hypermedia links to related resources
3. **`_templates`** - Form templates for modifying the resource (future)

### Example Response

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "Jan",
  "lastName": "Novák",
  "_links": {
    "self": {
      "href": "https://localhost:8443/api/members/123e4567-e89b-12d3-a456-426614174000"
    },
    "edit": {
      "href": "https://localhost:8443/api/members/123e4567-e89b-12d3-a456-426614174000"
    },
    "collection": {
      "href": "https://localhost:8443/api/members"
    }
  },
  "_templates": {
    "update": {
      "method": "PUT",
      "contentType": "application/json",
      "properties": [
        {
          "name": "firstName",
          "required": true,
          "type": "text"
        },
        {
          "name": "lastName",
          "required": true,
          "type": "text"
        }
      ]
    }
  }
}
```

## Link Relations

The API uses standard link relations:

- **`self`** - The canonical URL of the current resource
- **`edit`** - URL to modify the resource (requires appropriate permissions)
- **`collection`** - URL to the collection this resource belongs to
- **`deactivate`** - URL to deactivate the member (future feature)
- **`activate`** - URL to activate a deactivated member (future feature)

## Using HATEOAS in Client Applications

### 1. Start at the API Root

Instead of hardcoding URLs, start at the API root:

```javascript
// Good - discoverable
const response = await fetch('https://localhost:8443/api');
const links = response._links;
const membersUrl = links.members.href;

// Bad - hardcoded
const membersUrl = 'https://localhost:8443/api/members';
```

### 2. Follow Links

Navigate through the API by following links:

```javascript
// Create a member
const createResponse = await fetch(membersUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer <token>'
  },
  body: JSON.stringify(memberData)
});

const member = await createResponse.json();

// Use the self link to access the member later
const memberUrl = member._links.self.href;
```

### 3. Use Templates for Forms

Generate UI forms dynamically from templates:

```javascript
const template = member._templates.update;

// Generate form fields from template properties
template.properties.forEach(prop => {
  createFormField(prop.name, prop.type, prop.required);
});

// Submit using template metadata
await fetch(memberUrl, {
  method: template.method,
  headers: {
    'Content-Type': template.contentType,
    'Authorization': 'Bearer <token>'
  },
  body: JSON.stringify(formData)
});
```

## Benefits

1. **Decoupling** - Clients don't need to know URL structures
2. **Evolvability** - Server can change URLs without breaking clients
3. **Discoverability** - API is self-documenting
4. **State Transitions** - Links represent valid state transitions
5. **Dynamic UI** - Forms can be generated from templates

## Member Registration Example

### Request

```http
POST /api/members HTTPS/1.1
Host: localhost:8443
Content-Type: application/json
Accept: application/prs.hal-forms+json
Authorization: Bearer <access_token>

{
  "firstName": "Jan",
  "lastName": "Novák",
  "dateOfBirth": "2005-05-15",
  "nationality": "CZ",
  "gender": "MALE",
  "emails": ["jan.novak@example.com"],
  "phones": ["+420777123456"]
}
```

### Response

```http
HTTPS/1.1 201 Created
Location: https://localhost:8443/api/members/123e4567-e89b-12d3-a456-426614174000
Content-Type: application/prs.hal-forms+json

{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "Jan",
  "lastName": "Novák",
  "_links": {
    "self": {
      "href": "https://localhost:8443/api/members/123e4567-e89b-12d3-a456-426614174000"
    }
  }
}
```

## Pagination with HATEOAS

The API supports pagination for collection resources (e.g., members list) using HATEOAS links. This allows clients to
navigate through large datasets efficiently while maintaining the discoverability principle.

### Paginated Response Structure

A paginated response includes:

```json
{
  "_embedded": {
    "members": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "firstName": "Jan",
        "lastName": "Novák",
        "registrationNumber": "ABC9801",
        "_links": {
          "self": {
            "href": "/api/members/123e4567-e89b-12d3-a456-426614174000"
          }
        }
      }
    ]
  },
  "_links": {
    "first": {
      "href": "/api/members?page=0&size=10&sort=lastName,asc"
    },
    "self": {
      "href": "/api/members?page=0&size=10&sort=lastName,asc"
    },
    "next": {
      "href": "/api/members?page=1&size=10&sort=lastName,asc"
    },
    "last": {
      "href": "/api/members?page=9&size=10&sort=lastName,asc"
    }
  },
  "page": {
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "number": 0
  }
}
```

### Page Metadata

The `page` object provides information about the current page:

| Field           | Type   | Description                            |
|-----------------|--------|----------------------------------------|
| `size`          | number | Number of items per page               |
| `totalElements` | number | Total number of items across all pages |
| `totalPages`    | number | Total number of pages available        |
| `number`        | number | Current page number (zero-based)       |

### Pagination Links

The API provides the following pagination links in `_links`:

| Link    | Description               | When Present           |
|---------|---------------------------|------------------------|
| `first` | Link to the first page    | Always                 |
| `self`  | Link to the current page  | Always                 |
| `next`  | Link to the next page     | When not on last page  |
| `prev`  | Link to the previous page | When not on first page |
| `last`  | Link to the last page     | Always                 |

### Query Parameters

Pagination is controlled via query parameters:

| Parameter | Type   | Default      | Description                                     |
|-----------|--------|--------------|-------------------------------------------------|
| `page`    | number | 0            | Zero-based page number                          |
| `size`    | number | 10           | Number of items per page (max 100)              |
| `sort`    | string | lastName,asc | Sort field and direction (e.g., `lastName,asc`) |

**Multiple sort fields are supported:**

```
GET /api/members?sort=lastName,asc&sort=firstName,asc
```

### Navigating Paginated Collections

#### 1. Initial Request

Start by requesting the first page:

```javascript
const response = await fetch('/api/members?page=0&size=10', {
  headers: {
    'Accept': 'application/prs.hal-forms+json'
  }
});

const paginatedResponse = await response.json();
const currentPage = paginatedResponse.page.number;      // 0
const totalPages = paginatedResponse.page.totalPages;   // 10
const members = paginatedResponse._embedded.members;     // Array of members
```

#### 2. Follow Links to Navigate Pages

**Good - Use HATEOAS links:**

```javascript
// Extract the next link from the response
const nextLink = paginatedResponse._links.next?.href;

if (nextLink) {
  // Navigate to the next page
  const nextPageResponse = await fetch(nextLink, {
    headers: {
      'Accept': 'application/prs.hal-forms+json'
    }
  });

  const nextPage = await nextPageResponse.json();
  // Process next page...
}

// Navigate to the last page
const lastPageUrl = paginatedResponse._links.last.href;
const lastPage = await fetch(lastPageUrl).then(r => r.json());
```

**Bad - Hardcode pagination:**

```javascript
// DON'T DO THIS - Hardcoded URLs break when API changes
const nextPage = await fetch(`/api/members?page=1&size=10`);
```

#### 3. Check for Link Availability

Not all links are present in every response. Always check for link existence:

```javascript
const links = paginatedResponse._links;

// Check if there's a next page
if (links.next) {
  console.log("More pages available");
} else {
  console.log("You're on the last page");
}

// Check if there's a previous page
if (links.prev) {
  console.log("You can go back");
}

// Build navigation UI dynamically
const navigation = {
  firstPage: links.first?.href,
  prevPage: links.prev?.href,
  currentPage: links.self.href,
  nextPage: links.next?.href,
  lastPage: links.last.href
};
```

#### 4. Implement Pagination Controls

Here's how to implement a complete pagination UI:

```javascript
class PaginationNavigator {
  async loadPage(url) {
    const response = await fetch(url, {
      headers: { 'Accept': 'application/prs.hal-forms+json' }
    });
    return await response.json();
  }

  async goToFirstPage() {
    return await this.loadPage(this.data._links.first.href);
  }

  async goToPreviousPage() {
    const prevLink = this.data._links.prev?.href;
    if (!prevLink) {
      console.log("Already on first page");
      return this.data;
    }
    return await this.loadPage(prevLink);
  }

  async goToNextPage() {
    const nextLink = this.data._links.next?.href;
    if (!nextLink) {
      console.log("Already on last page");
      return this.data;
    }
    return await this.loadPage(nextLink);
  }

  async goToLastPage() {
    return await this.loadPage(this.data._links.last.href);
  }

  hasPreviousPage() {
    return !!this.data._links.prev;
  }

  hasNextPage() {
    return !!this.data._links.next;
  }

  getCurrentPageNumber() {
    return this.data.page.number;
  }

  getTotalPages() {
    return this.data.page.totalPages;
  }

  canNavigate(direction) {
    switch(direction) {
      case 'first': return this.getCurrentPageNumber() > 0;
      case 'prev': return this.hasPreviousPage();
      case 'next': return this.hasNextPage();
      case 'last': return this.getCurrentPageNumber() < this.getTotalPages() - 1;
      default: return false;
    }
  }
}

// Usage
const navigator = new PaginationNavigator();
const firstPage = await navigator.goToFirstPage();
navigator.data = firstPage;

// Display pagination controls
function renderPaginationControls(navigator) {
  const controls = {
    first: navigator.canNavigate('first'),
    prev: navigator.canNavigate('prev'),
    next: navigator.canNavigate('next'),
    last: navigator.canNavigate('last'),
    currentPage: navigator.getCurrentPageNumber() + 1,  // Convert to 1-based for display
    totalPages: navigator.getTotalPages()
  };

  // Render UI based on controls object
  return `
    <button ${!controls.first ? 'disabled' : ''} onclick="navigator.goToFirstPage()">First</button>
    <button ${!controls.prev ? 'disabled' : ''} onclick="navigator.goToPreviousPage()">Previous</button>
    <span>Page ${controls.currentPage} of ${controls.totalPages}</span>
    <button ${!controls.next ? 'disabled' : ''} onclick="navigator.goToNextPage()">Next</button>
    <button ${!controls.last ? 'disabled' : ''} onclick="navigator.goToLastPage()">Last</button>
  `;
}
```

### Sorting with Pagination

Sort parameters are preserved in pagination links, ensuring consistent sorting across all pages:

```javascript
// Request with sorting
const sortedResponse = await fetch('/api/members?sort=lastName,desc&sort=firstName,asc');

// Pagination links will preserve sort parameters
const nextLink = sortedResponse._links.next.href;
// nextLink = "/api/members?page=1&size=10&sort=lastName,desc&sort=firstName,asc"

// Sorting is maintained when navigating pages
const nextPage = await fetch(nextLink);
```

### Edge Cases

#### Empty Pages

Requesting a page beyond available data returns an empty collection:

```json
{
  "_embedded": {
    "members": []
  },
  "_links": {
    "first": { "href": "/api/members?page=0&size=10" },
    "self": { "href": "/api/members?page=999&size=10" },
    "last": { "href": "/api/members?page=9&size=10" }
  },
  "page": {
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "number": 999
  }
}
```

#### Single Page Results

When all items fit on one page, `next` and `prev` links are absent:

```json
{
  "_links": {
    "first": { "href": "/api/members?page=0&size=10" },
    "self": { "href": "/api/members?page=0&size=10" },
    "last": { "href": "/api/members?page=0&size=10" }
  },
  "page": {
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "number": 0
  }
}
```

### Best Practices

1. ✅ **Always use HATEOAS links** - Don't construct pagination URLs manually
2. ✅ **Check for link existence** - Not all links are present in every response
3. ✅ **Preserve sort parameters** - Use the links provided to maintain sorting
4. ✅ **Display progress** - Show "Page X of Y" using page metadata
5. ✅ **Handle edge cases** - Empty pages, single page results
6. ❌ **Don't increment page numbers manually** - Links are safer
7. ❌ **Don't assume total pages** - Use `page.totalPages` instead
8. ❌ **Don't cache page URLs** - They may change with sorting/filtering

## Current Implementation Status

### Implemented

- ✅ HAL+FORMS media type support
- ✅ `_links.self` relation for created members
- ✅ Location header with member URL
- ✅ Spring HATEOAS integration
- ✅ Collection resources with pagination links (first, last, next, prev, self)
- ✅ Page metadata (size, totalElements, totalPages, number)
- ✅ Sort parameter preservation in pagination links
- ✅ Multi-field sorting support

### Future Enhancements

- ⏳ `_templates` for update forms
- ⏳ Additional link relations (edit, deactivate, activate)
- ⏳ Search resource with query templates
- ⏳ Filtering with pagination (filter parameters preserved in links)

## References

- [HAL Specification](http://stateless.co/hal_specification.html)
- [HAL-FORMS Specification](https://rwcbook.github.io/hal-forms/)
- [Spring HATEOAS Documentation](https://docs.spring.io/spring-hateoas/docs/current/reference/html/)
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)

## Testing HATEOAS

Use tools that support HAL+FORMS:

- **cURL**: `curl -k -H "Accept: application/prs.hal-forms+json" https://localhost:8443/api/members`
- **HTTPie**: `http GET https://localhost:8443/api/members Accept:application/prs.hal-forms+json`
- **Postman**: Set Accept header to `application/prs.hal-forms+json`
- **HAL Browser**: Built-in Spring Boot HAL browser (if enabled)

## Common Pitfalls

1. ❌ **Don't hardcode URLs** - Always follow links
2. ❌ **Don't parse link URLs** - Treat them as opaque identifiers
3. ❌ **Don't ignore link relations** - They describe the relationship
4. ✅ **Do check for link existence** - Links represent available actions
5. ✅ **Do use templates** - They describe valid request formats
