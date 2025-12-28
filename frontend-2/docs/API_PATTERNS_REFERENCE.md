# API Fetching Patterns - Quick Reference Card

## When to Use Which Pattern

```
┌─ Is this endpoint in OpenAPI spec?
│  └─ YES → Use useKlabisApiQuery (type-safe)
│  └─ NO → Use useAuthorizedQuery / useAuthorizedMutation
│
└─ For page-level data and form handling:
   → Handled automatically by internal components
```

## One-Line Examples

| Pattern                    | Example                                                              |
|----------------------------|----------------------------------------------------------------------|
| **Type-Safe OpenAPI GET**  | `const { data } = useKlabisApiQuery('get', '/members/{id}', {...});` |
| **Type-Safe OpenAPI POST** | `const { mutate } = useKlabisApiMutation('post', '/members');`       |
| **Custom GET**             | `const { data } = useAuthorizedQuery('/api/custom');`                |
| **Custom POST**            | `const { mutate } = useAuthorizedMutation({ method: 'POST' });`      |

## Cache Configuration at a Glance

```typescript
// Type-Safe OpenAPI (configured per endpoint)
// Check your *.d.ts spec file

// Custom Authorized Requests
staleTime: 0(or
app - specific
)     // Application-specific
gcTime: default                     // Application-specific
retry: 0                            // No retry
```

## Query Key Patterns

```typescript
// ✅ CORRECT - ['domain', ...identifiers]
['authorized', '/api/custom-endpoint']
    ['authorized', '/api/members']
    ['/api', 'v1']

// ❌ WRONG - Missing domain or unclear
    ['members']
    ['/api/members']
    ['data', id]
```

## The 5-Second Decision Tree

1. **OpenAPI spec available?** → `useKlabisApiQuery()` / `useKlabisApiMutation()`
2. **Custom GET request?** → `useAuthorizedQuery()`
3. **Custom mutation?** → `useAuthorizedMutation()`
4. **Page-level data?** → Handled by HalRouteProvider (internal)
5. **Form data/options?** → Handled by form components (internal)

## Error Handling Pattern

```typescript
// Every hook follows this pattern:
const {data, isLoading, error} = useYourHook(...);

if (isLoading) return <Spinner / >;
if (error) return <Alert>{error.message} < /Alert>;
return <YourComponent data = {data}
/>;
```

## Testing Pattern

```typescript
// Always use QueryClientProvider wrapper
const {result} = renderHook(() => useAuthorizedQuery('/api/items'), {
    wrapper: ({children}) => (
        <QueryClientProvider client = {queryClient} >
            {children}
            < /QueryClientProvider>
    ),
});

// Only mock the API boundary
jest.mock('../api/authorizedFetch');
```

## Common Mistakes & Fixes

| Mistake                              | Fix                                        |
|--------------------------------------|--------------------------------------------|
| Using `fetch()` directly             | Use `useAuthorizedQuery()` instead         |
| Manual loading state with `useState` | Use hook's built-in `isLoading`            |
| Manual caching logic                 | Let React Query handle it automatically    |
| Returning `undefined` from queryFn   | Return `null` or empty value instead       |
| Not using authorized hooks           | Always use `useAuthorizedQuery/Mutation()` |
| Ignoring error states                | Always check and display errors            |
| Retrying too many times              | Use `retry: 0` for immediate errors        |

## Performance Tips

1. **Use `enabled` flag** - Prevent unnecessary queries
   ```typescript
   const { data } = useAuthorizedQuery('/api/items', {
     enabled: itemId !== undefined,
   });
   ```

2. **Use `select` for transformation** - Reduce re-renders
   ```typescript
   const { data } = useAuthorizedQuery('/api/items', {
     select: (items) => items.filter(i => i.active),
   });
   ```

3. **Set appropriate staleTimes** - Balance freshness and caching
   ```typescript
   staleTime: 5 * 60 * 1000  // Don't over-cache
   ```

4. **Don't set `gcTime: 0`** - Unless you need fresh cache per mount
   ```typescript
   gcTime: 5 * 60 * 1000  // Keep cache for reasonable time
   ```

## By File Type

### Custom Data Component

```typescript
function CustomData() {
    const {data} = useAuthorizedQuery('/api/custom');
    // Or for type-safe:
    const {data} = useKlabisApiQuery('get', '/members/{id}', {...});
}
```

### Custom Mutation

```typescript
function UpdateForm() {
    const {mutate} = useAuthorizedMutation({
        method: 'POST',
        onSuccess: () => { /* refresh data */
        },
    });

    const handleSubmit = (data) => {
        mutate({url: '/api/items', data});
    };
}
```

## Debugging

### Enable React Query DevTools

```bash
npm install @tanstack/react-query-devtools
```

```typescript
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';

function App() {
    return (
        <>
            {/* Your app */}
        < ReactQueryDevtools
    initialIsOpen = {false}
    />
    < />
)
    ;
}
```

### Check Cache State

```typescript
const queryClient = useQueryClient();
console.log(queryClient.getQueryData(['authorized', '/api/items']));
```

### Log Query State

```typescript
const {data, status, fetchStatus} = useAuthorizedQuery('/api/items');
console.log(`Query Status: ${status}, Fetch Status: ${fetchStatus}`);
// Helps distinguish between idle/pending/success/error states
```

## Related Documentation

- **Full Guide**: See `API_FETCHING_GUIDE.md`
- **Testing Patterns**: See `docs/frontend/testing_patterns.md`
- **Project Structure**: See `developers.md`

## Last Updated

Phase 5 - Public API focus: useKlabisApiQuery, useAuthorizedQuery, useAuthorizedMutation
