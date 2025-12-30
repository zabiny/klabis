# API Data Fetching Guide

> Guide to API data fetching patterns in the Klabis frontend application.
>
> **Status**: Production-ready (Phase 4 standardization complete)

## Quick Reference

| Use Case                           | Hook                      | Purpose                                   | Caching      | Retry  |
|------------------------------------|---------------------------|-------------------------------------------|--------------|--------|
| OpenAPI endpoints with types       | `useKlabisApiQuery()`     | Type-safe queries for known API endpoints | Varies       | Varies |
| Custom GET requests                | `useAuthorizedQuery()`    | Generic authorized GET requests           | App-specific | No     |
| Custom mutations (POST/PUT/DELETE) | `useAuthorizedMutation()` | Generic authorized mutations              | N/A          | No     |

**Note**: Page-level data fetching and form data/options are handled automatically by specialized internal components.
You typically won't call `useHalRoute`, `useHalFormData`, or `useHalFormOptions` directly.

---

## 1. Type-Safe OpenAPI Queries

### Overview

Use the generated OpenAPI client for type-safe queries and mutations with automatic request/response validation.

**Use this for:**

- Endpoints defined in OpenAPI spec (*.d.ts)
- Maximum type safety and autocomplete
- Automatic request serialization
- Expected standardized error responses

### Implementation

```typescript
import {useKlabisApiQuery, useKlabisApiMutation} from '../api';

function MemberDetails({memberId}) {
    // Type-safe GET request with autocomplete
    const {data, isLoading, error} = useKlabisApiQuery(
        'get',
        '/members/{id}',
        {params: {path: {id: memberId}}}
    );

    if (isLoading) return <Spinner / >;
    if (error) return <Alert>{error.message} < /Alert>;

    return <MemberCard data = {data}
    />;
}

function UpdateMember() {
    const {mutate, isPending} = useKlabisApiMutation(
        'put',
        '/members/{id}'
    );

    const handleUpdate = (id: string, updates: any) => {
        mutate(
            {params: {path: {id}}, body: updates},
            {
                onSuccess: (data) => console.log('Updated:', data),
                onError: (error) => console.error('Error:', error),
            }
        );
    };

    return <button onClick = {()
=>
    handleUpdate('123', {...})
}
    disabled = {isPending} >
        Update
        < /button>;
}
```

### Type Safety

- Autocomplete for endpoints: `'/members/{id}'` with validated path parameters
- Request body validation: TypeScript ensures correct format
- Response typing: Automatic inference of response type
- Error type: Standardized error responses from API

### Query Configuration

- **Caching**: Configured per endpoint in OpenAPI spec
- **Retry**: Varies by endpoint (configured in spec)
- **Default Behavior**: Follows React Query defaults

---

## 2. Generic Authorized Requests

### Overview

When you need React Query features with `authorizedFetch()` but don't have an OpenAPI spec for the endpoint.

**Use this for:**

- Custom endpoints not in OpenAPI spec
- Legacy API endpoints
- External APIs
- Dynamic endpoint discovery

### Implementation - Query (GET)

```typescript
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';

function CustomDataFetch() {
    const {data, isLoading, error} = useAuthorizedQuery('/api/custom-endpoint', {
        enabled: shouldFetch,                           // Conditionally enable
        staleTime: 10 * 60 * 1000,                     // 10 minutes
        select: (data) => data.filtered.results,       // Transform response
        headers: {'X-Custom-Header': 'value'},       // Custom headers
    });

    if (isLoading) return <Spinner / >;
    if (error) return <Alert>{error.message} < /Alert>;

    return <div>{JSON.stringify(data)} < /div>;
}
```

### Implementation - Mutation (POST/PUT/DELETE)

```typescript
import {useAuthorizedMutation} from '../hooks/useAuthorizedFetch';

function CustomDataMutation() {
    const {mutate, isPending, error} = useAuthorizedMutation({
        method: 'POST',
        headers: {'X-Custom-Header': 'value'},
        onSuccess: (data) => console.log('Success:', data),
        onError: (error) => console.error('Error:', error),
        onSettled: () => console.log('Done'),
    });

    const handleSubmit = (formData) => {
        mutate({
            url: '/api/custom-endpoint',
            data: formData,
        });
    };

    return (
        <form onSubmit = {(e)
=>
    {
        e.preventDefault();
        handleSubmit({});
    }
}>
    <button type = "submit"
    disabled = {isPending} > Submit < /button>
    {
        error && <Alert>{error.message} < /Alert>}
        < /form>
    )
        ;
    }
```

### Options - useAuthorizedQuery

```typescript
interface UseAuthorizedQueryOptions<T> {
    // Custom headers for this request
    headers?: Record<string, string>;

    // Enable/disable the query
    enabled?: boolean;

    // Time until data is considered stale
    staleTime?: number;

    // Transform function for response data
    select?: (data: unknown) => T;
}
```

### Options - useAuthorizedMutation

```typescript
interface UseAuthorizedMutationOptions {
    // HTTP method (POST, PUT, DELETE, etc.)
    method: string;

    // Custom headers for all mutations
    headers?: Record<string, string>;

    // Called on success
    onSuccess?: (data: unknown) => void;

    // Called on error
    onError?: (error: Error) => void;

    // Called when mutation settles (success or error)
    onSettled?: () => void;
}
```

### Return Value

```typescript
// useAuthorizedQuery
interface UseQueryResult<T> {
    data: T | undefined;           // Fetched data
    isLoading: boolean;             // First fetch
    isFetching: boolean;            // Any fetch (including refetch)
    error: Error | null;            // Error if failed
    refetch: () => Promise<
    ...>;    // Manual refetch
}

// useAuthorizedMutation
interface UseMutationResult<TData, TVariables> {
    mutate: (variables: TVariables) => void;
    isPending: boolean;             // Mutation in progress
    error: Error | null;            // Error if failed
    data: TData | undefined;        // Last successful response
    status: 'idle' | 'pending' | 'success' | 'error';
}
```

### Query Configuration

- **Query Key**: `['authorized', url]`
- **Stale Time**: 0 (application-specific)
- **Retry**: No (application-specific)

---

## Query Key Conventions

Follow this pattern for consistency: **`['domain', ...identifiers]`**

```typescript
// ✅ GOOD - Clear domain, specific identifiers
['authorized', '/api/custom-endpoint']
    ['authorized', '/api/members']
    ['/api', 'v1']                              // Special: root navigation

// ❌ BAD - Not following convention
    ['members']                                  // No domain
    ['/api/members/123']                        // No domain prefix
    ['data', 'members', 123]                    // Unclear structure
```

### Why This Matters

1. **Debugging**: DevTools and logs show the domain/purpose
2. **Cache Isolation**: Different domains don't collide
3. **Team Consistency**: Everyone knows the pattern
4. **Maintenance**: Future developers understand intent

---

## Cache Strategy

### Stale Time (When to re-validate)

| Data Type            | Stale Time | Rationale                              |
|----------------------|------------|----------------------------------------|
| Type-safe OpenAPI    | Varies     | Configured per endpoint in spec        |
| Generic Custom Fetch | 0          | Application-specific; default to fresh |

### Cache Time (When to evict)

| Data Type            | Cache Time | Rationale                       |
|----------------------|------------|---------------------------------|
| Type-safe OpenAPI    | Varies     | Configured per endpoint in spec |
| Generic Custom Fetch | default    | Application-specific            |

### Retry Strategy

All standardized patterns use **`retry: 0`** (no automatic retry) because:

1. **Better UX**: Show errors immediately rather than delaying with retries
2. **Graceful Degradation**: Forms show empty fields instead of hanging
3. **User Control**: Let users manually retry via refresh button
4. **Network Awareness**: Respect user's network state

---

## Error Handling

### Component-Level Error Display

```typescript
function MyComponent() {
    const {data, isLoading, error} = useAuthorizedQuery('/api/items');

    if (isLoading) return <Spinner / >;
    if (error) return <Alert variant = "error"
    title = "Failed to Load" >
        {error.message}
        < button
    onClick = {()
=>
    window.location.reload()
}>
    Retry < /button>
    < /Alert>;

    return <div>{/* render content */} < /div>;
}
```

### Error Boundaries for Critical Errors

```typescript
import {ErrorBoundary} from 'react-error-boundary';

function App() {
    return (
        <ErrorBoundary FallbackComponent = {ErrorFallback} >
            <Router>
                <Routes>
                    {/* routes */}
            < /Routes>
            < /Router>
            < /ErrorBoundary>
    );
}
```

---

## Best Practices

### ✅ DO

1. **Use React Query hooks** - Always use React Query for data fetching
   ```typescript
   // ✅ GOOD
   const { data } = useAuthorizedQuery('/api/members');
   ```

2. **Return data from query function** - Never return undefined
   ```typescript
   // ✅ GOOD
   queryFn: async () => {
     const response = await fetch('/api/data');
     return response.json();  // Always return data
   }
   ```

3. **Use enabled flag for conditional queries**
   ```typescript
   // ✅ GOOD
   const { data } = useAuthorizedQuery('/api/members', {
     enabled: memberId !== undefined,
   });
   ```

4. **Transform data with select**
   ```typescript
   // ✅ GOOD
   const { data } = useAuthorizedQuery('/api/members', {
     select: (data) => data.filter(m => m.active),
   });
   ```

5. **Handle all three states** - loading, success, error
   ```typescript
   // ✅ GOOD
   if (isLoading) return <Spinner />;
   if (error) return <ErrorAlert />;
   return <Content data={data} />;
   ```

6. **Use queryKey convention** - Follow `['domain', identifier]`
   ```typescript
   // ✅ GOOD
   queryKey: ['authorized', '/api/custom']
   ```

### 3. Table Data Loading with KlabisTableWithQuery

For displaying paginated, sortable data in tables, use the **KlabisTableWithQuery** component which wraps
`useAuthorizedQuery()`.

#### Overview

`KlabisTableWithQuery` is a data-loading wrapper around the pure UI component `KlabisTable`. It handles:

- Data fetching with pagination (`page`, `size` query parameters)
- Sorting (`sort` query parameter)
- State management (page, rowsPerPage, sort)
- Error handling
- All using `useAuthorizedQuery()` internally

#### Implementation - Basic Usage

```typescript
import {KlabisTableWithQuery} from '../components/KlabisTable';
import {TableCell} from '../components/KlabisTable';

function MembersTable() {
   const {resourceData} = useHalRoute();
   const memberLink = resourceData?._links?.self;

   if (!memberLink) return null;

   return (
           <KlabisTableWithQuery
                   link = {memberLink}
   collectionName = "membersApiResponseList"
   defaultOrderBy = "lastName"
   defaultOrderDirection = "asc"
   onRowClick = {(member)
=>
   navigate(`/members/${member.id}`)
}
>
   <TableCell column = "firstName"
   sortable > First
   Name < /TableCell>
   < TableCell
   column = "lastName"
   sortable > Last
   Name < /TableCell>
   < TableCell
   column = "email" > Email < /TableCell>
           < /KlabisTableWithQuery>
)
   ;
}
```

#### Implementation - With Custom Link

```typescript
import {KlabisTableWithQuery} from '../components/KlabisTable';

function CustomDataTable() {
   const link = {href: '/api/v1/items?includeArchived=true'};

   return (
           <KlabisTableWithQuery
                   link = {link}
   defaultOrderBy = "createdDate"
   defaultOrderDirection = "desc"
   rowsPerPageOptions = {[5, 10, 20
]
}
>
   <TableCell column = "name"
   sortable > Name < /TableCell>
   < TableCell
   column = "createdDate"
   sortable > Created < /TableCell>
   < /KlabisTableWithQuery>
)
   ;
}
```

#### How It Works

The wrapper internally:

1. Creates pagination state (`page`, `rowsPerPage`, `sort`)
2. Builds query URL with parameters: `/api/items?page=0&size=10&sort=name,asc`
3. Calls `useAuthorizedQuery(url, {staleTime: 30000})`
4. Passes data and callbacks to `KlabisTable` (pure UI component)
5. `KlabisTable` renders table, handles user clicks
6. User interactions trigger state updates → new URL → new query

#### Props

```typescript
interface KlabisTableWithQueryProps<T = any> {
   // HAL Link object with href
   link: Link;

   // Optional: Extract from _embedded[collectionName]
   collectionName?: string;

   // UI customization
   onRowClick?: (item: T) => void;
   defaultOrderBy?: string;
   defaultOrderDirection?: 'asc' | 'desc';
   emptyMessage?: string;
   rowsPerPageOptions?: number[];
   defaultRowsPerPage?: number;

   // Column definitions
   children: React.ReactNode;
}
```

#### Pure UI Component - KlabisTable

For more control, use `KlabisTable` directly with manual data fetching:

```typescript
import {KlabisTable} from '../components/KlabisTable';
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';

function CustomTable() {
   const [page, setPage] = useState(0);
   const [rowsPerPage, setRowsPerPage] = useState(10);

   const {data, error} = useAuthorizedQuery(
           `/api/items?page=${page}&size=${rowsPerPage}`,
           {staleTime: 30000}
   );

   return (
           <KlabisTable
                   data = {data?.items || []
}
   page = {data?.page
}
   error = {error}
   currentPage = {page}
   rowsPerPage = {rowsPerPage}
   onPageChange = {setPage}
   onRowsPerPageChange = {setRowsPerPage}
   >
   <TableCell column = "name" > Name < /TableCell>
           < /KlabisTable>
)
   ;
}
```

#### Query Parameters

`KlabisTableWithQuery` automatically builds these query parameters:

| Parameter | Example       | Purpose                  |
|-----------|---------------|--------------------------|
| `page`    | `0`, `1`, `2` | Zero-indexed page number |
| `size`    | `10`, `25`    | Rows per page            |
| `sort`    | `name,asc`    | `column,direction`       |

The base URL from the link is preserved:

```
/api/items?existing=param&page=0&size=10&sort=name,asc
```

#### Caching Behavior

- **Stale Time**: 30 seconds (automatic revalidation after 30s)
- **Retry**: 1 attempt (per useAuthorizedQuery defaults)
- **Cache Key**: Based on full URL including page/sort/size parameters
- **Deduplication**: Simultaneous requests to same URL are deduplicated

#### Testing

```typescript
import {render, screen, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {KlabisTableWithQuery} from '../components/KlabisTable';
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';

jest.mock('../hooks/useAuthorizedFetch');

describe('KlabisTableWithQuery', () => {
   let queryClient: QueryClient;

   beforeEach(() => {
      queryClient = new QueryClient({
         defaultOptions: {queries: {retry: false, gcTime: 0}}
      });

      jest.mocked(useAuthorizedQuery).mockReturnValue({
         data: {
            items: [{id: 1, name: 'Item 1'}],
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0}
         },
         isLoading: false,
         error: null,
         refetch: jest.fn(),
      } as any);
   });

   it('renders table with fetched data', async () => {
      render(
              <QueryClientProvider client = {queryClient} >
              <KlabisTableWithQuery link = {
      {
         href: '/api/items'
      }
   }>
      <TableCell column = "name" > Name < /TableCell>
              < /KlabisTableWithQuery>
              < /QueryClientProvider>
   )
      ;

      await waitFor(() => {
         expect(screen.getByText('Item 1')).toBeInTheDocument();
      });
   });
});
```

### ❌ DON'T

1. **Use direct fetch()** - Always wrap with React Query
   ```typescript
   // ❌ BAD
   const [data, setData] = useState(null);
   useEffect(() => {
     fetch('/api/members').then(r => r.json()).then(setData);
   }, []);

   // ✅ GOOD
   const { data } = useAuthorizedQuery('/api/members');
   ```

2. **Manual loading states** - Use React Query's built-in states
   ```typescript
   // ❌ BAD
   const [isLoading, setIsLoading] = useState(false);
   useEffect(() => {
     setIsLoading(true);
     fetch(...).finally(() => setIsLoading(false));
   }, []);

   // ✅ GOOD
   const { isLoading } = useAuthorizedQuery('/api/members');
   ```

3. **Manual caching** - React Query handles it
   ```typescript
   // ❌ BAD
   const [cache, setCache] = useState({});
   if (cache[url]) return cache[url];

   // ✅ GOOD (automatic with React Query)
   const { data } = useAuthorizedQuery(url);
   ```

4. **Return undefined from queryFn** - Return null or empty value
   ```typescript
   // ❌ BAD
   queryFn: async () => {
     const response = await fetch('/api/data');
     if (!response.ok) return undefined;
     return response.json();
   }

   // ✅ GOOD
   queryFn: async () => {
     const response = await fetch('/api/data');
     if (!response.ok) return null;
     return response.json();
   }
   ```

5. **Retry without understanding cost** - Retries delay error display
   ```typescript
   // ❌ BAD
   retry: 3,  // Waits for retries before showing error

   // ✅ GOOD
   retry: 0,  // Show error immediately, let user retry manually
   ```

6. **Ignore error states** - Always display errors to users
   ```typescript
   // ❌ BAD
   const { data, error } = useAuthorizedQuery('/api/members');
   return <List items={data || []} />;  // Silently fail

   // ✅ GOOD
   const { data, error } = useAuthorizedQuery('/api/members');
   if (error) return <Alert>{error.message}</Alert>;
   return <List items={data} />;
   ```

---

## Migration Guide

### From Direct fetch() to useAuthorizedQuery

**Before:**

```typescript
function MembersList() {
    const [members, setMembers] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetch = async () => {
            setIsLoading(true);
            try {
                const res = await authorizedFetch('/api/members');
                const data = await res.json();
                setMembers(data);
            } catch (err) {
                setError(err);
            } finally {
                setIsLoading(false);
            }
        };
        fetch();
    }, []);

    if (isLoading) return <Spinner / >;
    if (error) return <Alert>{error.message} < /Alert>;
    return <table>
...
    </table>;
}
```

**After:**

```typescript
function MembersList() {
    const {data: members, isLoading, error} = useAuthorizedQuery('/api/members');

    if (isLoading) return <Spinner / >;
    if (error) return <Alert>{error.message} < /Alert>;
    return <table>
...
    </table>;
}
```

**Benefits:**

- ✅ 19 lines → 7 lines (63% less code)
- ✅ Automatic caching
- ✅ Automatic refetching on focus
- ✅ Automatic request deduplication
- ✅ Built-in loading/error states

---

## Testing

### Testing Queries

```typescript
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';
import {authorizedFetch} from '../api/authorizedFetch';

jest.mock('../api/authorizedFetch');

describe('useAuthorizedQuery', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},  // Isolate tests
            },
        });
    });

    it('should fetch data successfully', async () => {
        const mockData = {id: 1, name: 'Test'};
        jest.mocked(authorizedFetch).mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue(mockData),
        } as any);

        const {result} = renderHook(
            () => useAuthorizedQuery('/api/items/1'),
            {
                wrapper: ({children}) => (
                    <QueryClientProvider client = {queryClient} >
                        {children}
                        < /QueryClientProvider>
                ),
            }
        );

        expect(result.current.isLoading).toBe(true);

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.data).toEqual(mockData);
    });
});
```

### Testing Mutations

```typescript
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useAuthorizedMutation} from '../hooks/useAuthorizedFetch';
import {authorizedFetch} from '../api/authorizedFetch';

jest.mock('../api/authorizedFetch');

describe('useAuthorizedMutation', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                mutations: {retry: false},
            },
        });
    });

    it('should post data successfully', async () => {
        const mockResponse = {id: 1, name: 'New Item'};
        jest.mocked(authorizedFetch).mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue(mockResponse),
        } as any);

        const {result} = renderHook(
            () => useAuthorizedMutation({method: 'POST'}),
            {
                wrapper: ({children}) => (
                    <QueryClientProvider client = {queryClient} >
                        {children}
                        < /QueryClientProvider>
                ),
            }
        );

        result.current.mutate({
            url: '/api/items',
            data: {name: 'New Item'},
        });

        await waitFor(() => {
            expect(result.current.isPending).toBe(false);
        });

        expect(result.current.data).toEqual(mockResponse);
    });
});
```

---

## Troubleshooting

### Problem: Query Key Collision

**Symptom**: Multiple hooks share cached data unexpectedly

**Cause**: Using identical query keys for different data

**Solution**: Follow query key convention with domain prefix

```typescript
// ❌ BAD - Collides
queryKey: ['members']
queryKey: ['members']  // Same cache!

// ✅ GOOD - Distinct
queryKey: ['authorized', '/api/members']
queryKey: ['authorized', '/api/custom']
```

### Problem: Infinite Loading State

**Symptom**: `isLoading` stays true forever

**Cause**: Query function returns `undefined`

**Solution**: Always return a value from queryFn

```typescript
// ❌ BAD
queryFn: async () => {
    if (!shouldFetch) return undefined;
    return fetchData();
}

// ✅ GOOD
queryFn: async () => {
    if (!shouldFetch) return null;
    return fetchData();
}
```

### Problem: Stale Cache Not Updating

**Symptom**: Data is old/outdated

**Cause**: Stale time too long, or data changed on server

**Solution**: Adjust stale time or manually refetch

```typescript
// Reduce stale time
staleTime: 1 * 60 * 1000,  // 1 minute instead of 5

// Or manually refetch after mutation
const {refetch} = useAuthorizedQuery('/api/items');
const {mutate} = useAuthorizedMutation({
    method: 'POST',
    onSuccess: () => {
        refetch();
    },
});
```

### Problem: Authorization Header Not Sent

**Symptom**: API returns 401 Unauthorized

**Cause**: Not using `useAuthorizedQuery` / `useAuthorizedMutation`

**Solution**: Always use authorized hooks instead of fetch()

```typescript
// ❌ BAD - No authorization
fetch('/api/members')

// ✅ GOOD - Authorization header added automatically
useAuthorizedQuery('/api/members')
```

---

## Additional Resources

- **React Query Docs**: https://tanstack.com/query/latest
- **OpenAPI TypeScript**: https://openapi-ts.dev/

---

## Summary

The standardized API fetching approach with public APIs brings:

1. **Consistency** - All custom data fetching follows the same patterns
2. **Performance** - Automatic caching, deduplication, and refetching
3. **Developer Experience** - Less boilerplate, more features out of the box
4. **Maintainability** - Clear patterns for new team members
5. **Reliability** - Proper error handling and loading states everywhere

**Remember:** Use `useKlabisApiQuery()` for type-safe OpenAPI endpoints, and `useAuthorizedQuery()`/
`useAuthorizedMutation()` for custom endpoints. Page-level data and form handling are managed automatically by internal
components.
