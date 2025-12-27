# Frontend Testing Patterns

This document describes testing patterns and best practices discovered through test reviews and implementation in the
klabis project.

## Table of Contents

- [Component Mocking Strategies](#component-mocking-strategies)
- [Context Provider Testing](#context-provider-testing)
- [React Router Testing](#react-router-testing)
- [React Query Cache Testing](#react-query-cache-testing)
- [Page Component Testing](#page-component-testing)
- [Error State Testing](#error-state-testing)
- [Null Safety Testing](#null-safety-testing)
- [Common Anti-Patterns](#common-anti-patterns)

## Component Mocking Strategies

### Selective Component Mocking

When testing a component that depends on child components, mock only the child components while testing the parent's
behavior, not their implementation details.

**Good Example** (from GenericHalPage.test.tsx):

```typescript
jest.mock('../components/UI', () => ({
    Alert: ({severity, children}: any) => (
        <div data - testid = {`alert-${severity}`
}
role = "alert" >
    {children}
    < /div>
),
Modal: ({isOpen, onClose, title, children}: any) =>
    isOpen ? (
        <div data - testid = "modal" role = "dialog" >
        {title && <h2>{title} < /h2>}
{
    children
}
<button onClick = {onClose} > Close < /button>
    < /div>
) :
null,
}))
;
```

**Why**:

- Mocked components maintain props interface for parent testing
- Uses `data-testid` for easy element location
- Simulates realistic behavior without implementation details

### What NOT to Mock

Don't mock providers that are central to your component's functionality. Instead, set up the provider hierarchy
properly.

```typescript
// BAD: Mocking HalRouteProvider completely
jest.mock('../contexts/HalRouteContext', () => ({
    HalRouteProvider: jest.fn(({children}) => children),
}));

// GOOD: Mock useHalRoute hook and set up real provider
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(), // Only mock the hook
}));
```

## Context Provider Testing

### Hook Testing with Wrappers

When testing context hooks with `renderHook`, create a wrapper component that includes all necessary providers.

**Example** (from HalRouteContext.test.tsx):

```typescript
const createWrapper = () => {
    return ({children}: { children: React.ReactNode }) => (
        <QueryClientProvider client = {queryClient} >
            <BrowserRouter>
                <HalRouteProvider>{children} < /HalRouteProvider>
            < /BrowserRouter>
            < /QueryClientProvider>
    );
};

const {result} = renderHook(() => useHalRoute(), {
    wrapper: createWrapper(),
});
```

**Benefits**:

- Isolated test environment for hooks
- Reusable wrapper across multiple tests
- Proper context hierarchy

### Component vs Hook Testing

Test hooks independently from components that use them:

```typescript
// Hook test: verify hook behavior in isolation
test('hook should return context value', () => {
    const {result} = renderHook(() => useHalRoute(), {wrapper: createWrapper()});
    expect(result.current.resourceData).toBeDefined();
});

// Component test: mock the hook to control input
test('component should handle context value', () => {
    useHalRoute.mockReturnValue({
        resourceData: mockData,
        isLoading: false,
        error: null,
    });
    render(<Component / >);
    expect(screen.getByText('expected text')).toBeInTheDocument();
});
```

## React Router Testing

### MemoryRouter for Route Testing

Use `MemoryRouter` with `initialEntries` to test components that depend on specific routes.

**Example** (from CalendarPage.test.tsx):

```typescript
const renderWithRouter = (ui: React.ReactElement, initialRoute: string = '/calendar') => {
    return render(
        <QueryClientProvider client = {queryClient} >
        <MemoryRouter initialEntries = {[initialRoute]} >
            {ui}
            < /MemoryRouter>
            < /QueryClientProvider>
    );
};

// Usage
renderWithRouter(<CalendarPage / >, '/calendar?referenceDate=2025-06-15');
```

**Key Points**:

- `initialEntries` expects an array of route strings
- Include query parameters in the entry string
- Wrap with QueryClientProvider if needed

### Route Parameter Testing

Test how components handle different URL parameters:

```typescript
it('should parse reference date from URL', () => {
    renderWithRouter(<CalendarPage / >, '/calendar?referenceDate=2025-12-15');
    expect(screen.getByText(/December 2025/i)).toBeInTheDocument();
});
```

## React Query Cache Testing

### Cache Configuration for Tests

Disable caching in tests to ensure isolation between tests:

```typescript
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,      // Disable retries
            gcTime: 0,         // Disable cache (was cacheTime in React Query v4)
        },
    },
});
```

### Testing Actual Caching Behavior

When testing cache behavior, create a separate QueryClient with caching enabled:

```typescript
it('should cache data between hook instances', async () => {
    // Create client WITH caching
    const cachingQueryClient = new QueryClient({
        defaultOptions: {
            queries: {retry: false}, // Keep caching enabled
        },
    });

    const createCachingWrapper = () => {
        return ({children}) => (
            <QueryClientProvider client = {cachingQueryClient} >
                <BrowserRouter>
                    <HalRouteProvider>{children} < /HalRouteProvider>
                < /BrowserRouter>
                < /QueryClientProvider>
        );
    };

    // First hook instance
    const {result: result1, unmount: unmount1} = renderHook(() => useHalRoute(), {
        wrapper: createCachingWrapper(),
    });

    await waitFor(() => {
        expect(result1.current.resourceData).toBeDefined();
    });

    // Verify it was called
    expect(fetchResource).toHaveBeenCalledTimes(1);
    unmount1();

    // Second hook instance - should use cache
    const {result: result2} = renderHook(() => useHalRoute(), {
        wrapper: createCachingWrapper(),
    });

    await waitFor(() => {
        expect(result2.current.resourceData).toBeDefined();
    });

    // Still only 1 call - data came from cache
    expect(fetchResource).toHaveBeenCalledTimes(1);
});
```

## Page Component Testing

Page components (in `/pages` folder) are top-level route components that fetch and display data. They differ from
regular components in that they:

- Fetch data via HAL API calls
- Manage loading and error states
- Orchestrate multiple child components
- Handle navigation

### Testing Strategy: API Calls as Mocked Boundary

Page components should be tested as **integration components** with the **API call layer as the mocked boundary**. This
approach:

1. **Mocks API calls** (`fetchResource`) - the external boundary
2. **Tests with real child components** - no mocking of UI components
3. **Tests the real context hooks** that manage data flow
4. **Verifies data flow** from API → context → UI

**Why this approach**:

- Tests the component in realistic conditions (with real child components and context)
- Verifies correct API calls are made
- Catches bugs in data transformation and component integration logic
- Remains fast and isolated (only external API is mocked)
- Provides confidence that pages work end-to-end with their real child components

### Page Component Test Structure

```typescript
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemberFinancePage} from './FinancesPage';

// 1. Mock ONLY the API boundary
jest.mock('../components/HalNavigator/hooks', () => ({
    fetchResource: jest.fn(),
}));

// 2. Mock context hook for controlling data flow
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(),
}));

const {fetchResource} = require('../components/HalNavigator/hooks');
const {useHalRoute} = require('../contexts/HalRouteContext');

describe('MemberFinancePage', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0}, // Isolate tests
            },
        });
        jest.clearAllMocks();
    });

    const renderPage = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client = {queryClient} >
                <MemoryRouter>{ui} < /MemoryRouter>
                < /QueryClientProvider>
        );
    };
});
```

**Key difference from unit tests**:

- Do NOT mock `HalEmbeddedTable`, `HalFormButton`, or other child components
- Let real child components render so you test integration with them
- Mocking only the API boundary (`fetchResource`) keeps tests fast and isolated

### Testing Data Loading Flow

Test that the page correctly fetches and displays data using real child components:

```typescript
describe('Data Loading', () => {
    it('should display loading skeleton while fetching data', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: true,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderPage(<MemberFinancePage / >);
        // Skeleton comes from real UI component behavior
        expect(screen.getByTestId('skeleton')).toBeInTheDocument();
    });

    it('should fetch finance data and display it with real child components', async () => {
        const mockFinanceData = {
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                owner: {href: '/api/members/456'},
                transactions: {href: '/api/finances/123/transactions'},
            },
        };

        // Mock API call that HalRouteProvider would make
        const {fetchResource} = require('../components/HalNavigator/hooks');
        fetchResource.mockResolvedValueOnce(mockFinanceData);

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderPage(<MemberFinancePage / >);

        // Verify page content is rendered (tests integration with real children)
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText(/1500/)).toBeInTheDocument();
        // Real child components are rendered and tested
        expect(screen.getByRole('table')).toBeInTheDocument(); // From real HalEmbeddedTable
    });
});
```

### Testing Subresource Loading

When pages use `HalSubresourceProvider` for nested data, test with real subresource components:

```typescript
describe('Subresource Loading', () => {
    it('should load related member data alongside finance data', async () => {
        const mockFinanceData = {
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                owner: {href: '/api/members/456'},
                transactions: {href: '/api/finances/123/transactions'},
            },
        };

        // Mock fetchResource for parent and subresources
        const {fetchResource} = require('../components/HalNavigator/hooks');
        fetchResource
            .mockResolvedValueOnce(mockFinanceData)     // Parent finance resource
            .mockResolvedValueOnce({                     // Owner subresource
                firstName: 'Jan',
                lastName: 'Novák',
            })
            .mockResolvedValueOnce({                     // Transactions subresource
                _embedded: {
                    transactionItemResponseList: [
                        {id: 1, date: '2025-12-01', amount: 100},
                    ],
                },
            });

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderPage(<MemberFinancePage / >);

        // Verify parent data rendered
        expect(screen.getByText('Finance')).toBeInTheDocument();

        // Verify subresource data is displayed through real child components
        await waitFor(() => {
            expect(screen.getByText('Jan Novák')).toBeInTheDocument(); // From real MemberName component
            expect(screen.getByRole('table')).toBeInTheDocument();      // From real HalEmbeddedTable
        });
    });
});
```

### Testing Error States

Test how pages handle API failures:

```typescript
describe('Error Handling', () => {
    it('should show error message when API call fails', () => {
        const error = new Error('HTTP 500: Internal Server Error');
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: error,
            navigateToResource: jest.fn(),
        });

        renderPage(<MemberFinancePage / >);
        expect(screen.getByTestId('alert-error')).toBeInTheDocument();
        expect(screen.getByText(/Internal Server Error/)).toBeInTheDocument();
    });

    it('should show NotFound when resource does not exist', () => {
        const error = new Error('HTTP 404: Not Found');
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: error,
            pathname: '/api/finances/999',
        });

        renderPage(<MemberFinancePage / >);
        expect(screen.getByTestId('not-found-page')).toBeInTheDocument();
    });
});
```

### Testing Navigation and User Interactions

Test that page components correctly handle user actions through real child components:

```typescript
describe('Navigation', () => {
    it('should navigate when table row is clicked on real table', async () => {
        const mockNavigateToResource = jest.fn();
        const mockFinanceData = {
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                transactions: {href: '/api/finances/123/transactions'},
            },
        };

        const mockTransactionData = {
            _embedded: {
                transactionItemResponseList: [
                    {
                        id: 1,
                        date: '2025-12-01',
                        amount: 100,
                        _links: {
                            self: {href: '/api/transactions/1'},
                        },
                    },
                ],
            },
        };

        const {fetchResource} = require('../components/HalNavigator/hooks');
        fetchResource.mockResolvedValueOnce(mockTransactionData);

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: mockNavigateToResource,
        });

        const {user} = renderPage(<MemberFinancePage / >);

        // Real table renders transaction row
        await waitFor(() => {
            expect(screen.getByRole('table')).toBeInTheDocument();
        });

        // User clicks real table row
        const row = screen.getByRole('row', {name: /2025-12-01/});
        await user.click(row);

        // Verify navigation was called with real transaction data
        expect(mockNavigateToResource).toHaveBeenCalledWith(
            expect.objectContaining({id: 1})
        );
    });

    it('should pass context correctly to real child components', async () => {
        const mockFinanceData = {
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                transactions: {href: '/api/finances/123/transactions'},
            },
        };

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderPage(<MemberFinancePage / >);

        // Verify real child component is rendered
        expect(screen.getByRole('table')).toBeInTheDocument();
    });
});
```

### Testing Null Safety

Pages must handle null data gracefully:

```typescript
describe('Null Safety', () => {
    it('should not crash when parent resource is null', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        expect(() => {
            renderPage(<MemberFinancePage / >);
        }).not.toThrow();

        // Page should still render with safe defaults
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText('- Kč')).toBeInTheDocument(); // Default for missing balance
    });

    it('should handle missing optional properties', () => {
        useHalRoute.mockReturnValue({
            resourceData: {}, // Empty object
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        expect(() => {
            renderPage(<MemberFinancePage / >);
        }).not.toThrow();
    });
});
```

### Real-World Example: MemberFinancePage

Full test example from `FinancesPage.test.tsx` using real child components:

```typescript
const mockFinanceData = mockFinanceResource({
    balance: 1500,
    _links: {
        self: {href: '/api/finances/123'},
        owner: {href: '/api/members/456'},
        transactions: {href: '/api/finances/123/transactions'},
    },
});

describe('MemberFinancePage', () => {
    // No mocking of child components - test with real HalSubresourceProvider and child components

    it('should load and display owner data through real subresource provider', async () => {
        const mockOwnerData = mockMemberResource({
            firstName: 'Jan',
            lastName: 'Novák',
        });

        // Mock fetchResource for both parent and subresource
        const {fetchResource} = require('../components/HalNavigator/hooks');
        fetchResource
            .mockResolvedValueOnce(mockFinanceData)
            .mockResolvedValueOnce(mockOwnerData);

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderWithRouter(<MemberFinancePage / >);

        // Verify data is displayed through real MemberName child component
        await waitFor(() => {
            expect(screen.getByText('Jan Novák')).toBeInTheDocument();
        });

        // Verify correct API call was made for subresource
        expect(fetchResource).toHaveBeenCalledWith('/api/members/456');
    });

    it('should handle null parent resource gracefully with real children', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderWithRouter(<MemberFinancePage / >);

        // Page renders with safe defaults (real child components handle null gracefully)
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText('- Kč')).toBeInTheDocument();
    });

    it('should render real transaction table when data loads', async () => {
        const mockTransactionsData = {
            _embedded: {
                transactionItemResponseList: [
                    {
                        id: 1,
                        date: '2025-12-01',
                        amount: 500,
                        _links: {self: {href: '/api/transactions/1'}},
                    },
                ],
            },
        };

        const {fetchResource} = require('../components/HalNavigator/hooks');
        fetchResource.mockResolvedValueOnce(mockTransactionsData);

        useHalRoute.mockReturnValue({
            resourceData: mockFinanceData,
            isLoading: false,
            error: null,
            navigateToResource: jest.fn(),
        });

        renderWithRouter(<MemberFinancePage / >);

        // Real table component renders with actual data
        await waitFor(() => {
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.getByText(/2025-12-01/)).toBeInTheDocument();
            expect(screen.getByText(/500/)).toBeInTheDocument();
        });
    });
});
```

**Key differences from unit test approach:**

- No mocking of HalSubresourceProvider - uses real provider
- No mocking of MemberName, HalEmbeddedTable - uses real components
- Tests real component integration and data flow
- Uses semantic queries (`getByRole('table')`) instead of `data-testid`
- Verifies actual rendered UI with real child components

### Key Principles for Page Component Testing

1. **Mock ONLY the API boundary** - Mock `fetchResource`, not context providers or child components
2. **Test with real child components** - Tables, modals, forms should be real for integration testing
3. **Use real context hooks** - Test with actual `useHalRoute` behavior
4. **Test data flow** - Verify data moves from API → context → UI → child components correctly
5. **Test error states** - Loading, errors, null data, and success states
6. **Test navigation** - User interactions with real child components that trigger navigation
7. **Keep tests isolated** - Use `gcTime: 0` in QueryClient, clear mocks in beforeEach
8. **Use semantic queries** - Use `getByRole()`, `getByText()` instead of `data-testid` where possible
9. **Mock factory functions** - Use `mockFinanceResource()`, `mockMemberResource()` for consistent test data

### Anti-Patterns for Page Component Testing

```typescript
// BAD: Testing implementation details
expect(component.state.isLoading).toBe(false);

// GOOD: Testing observable behavior
expect(screen.getByTestId('skeleton')).not.toBeInTheDocument();

// BAD: Mocking providers or child components
jest.mock('../contexts/HalRouteContext', () => ({
    HalRouteProvider: jest.fn(),
}));
jest.mock('../components/HalNavigator2/HalEmbeddedTable', () => ({
    HalEmbeddedTable: jest.fn(),
}));

// GOOD: Mock ONLY the API boundary
jest.mock('../components/HalNavigator/hooks', () => ({
    fetchResource: jest.fn(),
}));

// BAD: Testing without realistic data
useHalRoute.mockReturnValue({resourceData: {}});

// GOOD: Use complete mock data
useHalRoute.mockReturnValue({
    resourceData: mockFinanceData,
    isLoading: false,
    error: null,
    navigateToResource: jest.fn(),
});

// BAD: Using data-testid from mocked components
const row = screen.getByTestId('mock-table-row'); // Doesn't exist with real table

// GOOD: Using semantic queries with real components
const row = screen.getByRole('row', {name: /transaction-date/});
const table = screen.getByRole('table');
```

## Error State Testing

### Testing Different Error Scenarios

Always test how components handle different error types:

```typescript
describe('Error Handling', () => {
    it('should display NotFoundPage when 404 error occurs', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: new Error('HTTP 404 Not Found'),
            pathname: '/api/items/1',
        });

        render(<GenericHalPage / >);
        expect(screen.getByTestId('not-found-page')).toBeInTheDocument();
    });

    it('should display error alert for non-404 errors', () => {
        const error = new Error('Failed to fetch data');
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error,
            pathname: '/api/items',
        });

        render(<GenericHalPage / >);
        expect(screen.getByTestId('alert-error')).toBeInTheDocument();
        expect(screen.getByText(error.message)).toBeInTheDocument();
    });
});
```

### Suppressing Console Errors

When testing expected errors, suppress console output:

```typescript
it('should throw error when used outside provider', () => {
    // Suppress console.error for this test
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

    expect(() => {
        renderHook(() => useHalRoute());
    }).toThrow('useHalRoute must be used within HalRouteProvider');

    consoleSpy.mockRestore();
});
```

## Null Safety Testing

### Testing Null/Undefined Handling

Always test how components handle null and undefined values:

```typescript
describe('Null Handling', () => {
    it('should not crash when resourceData is null', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: null,
        });

        // Should not throw
        expect(() => {
            render(<Component / >);
        }).not.toThrow();
    });

    it('should display safe defaults when data is missing', () => {
        useHalRoute.mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: null,
        });

        render(<Component / >);
        expect(screen.getByText('- Kč')).toBeInTheDocument();
    });

    it('should handle undefined property gracefully', () => {
        useHalRoute.mockReturnValue({
            resourceData: {balance: undefined},
            isLoading: false,
            error: null,
        });

        expect(() => {
            render(<Component / >);
        }).not.toThrow();
    });
});
```

### Pattern: Optional Chaining + Nullish Coalescing

```typescript
// In component
function formatCurrency(amount?: number): string {
    return `${amount ?? '-'} Kč`;
}

// In test
expect(screen.getByText('- Kč')).toBeInTheDocument();
```

## Common Anti-Patterns

### ❌ Testing Implementation Details

```typescript
// BAD: Tests DOM structure instead of behavior
const spanElements = document.querySelectorAll('span');
expect(spanElements.length).toBeGreaterThanOrEqual(3);

// GOOD: Tests actual content/behavior
expect(screen.getByText('Expected Content')).toBeInTheDocument();
```

### ❌ Fragile Tests

```typescript
// BAD: Depends on exact button position
const button = screen.getByTestId('button').parentElement?.parentElement;
expect(button?.parentElement?.textContent).toContain('Container');

// GOOD: Tests logical relationship
const button = screen.getByTestId('button');
const table = screen.getByTestId('table');
expect(table.contains(button)).toBe(false); // Button is outside table
```

### ❌ Incomplete Tests

```typescript
// BAD: Doesn't verify the feature actually works
it('should skip fetching for login route', async () => {
    // This test doesn't actually verify the skip happened
    const {result} = renderHook(() => useHalRoute(), {wrapper: createWrapper()});
    expect(result.current).toBeDefined();
});

// GOOD: Explicitly verifies the behavior
it('should have idle state when fetch is disabled', () => {
    useHalRoute.mockReturnValue({
        queryState: 'idle', // Verify idle, not pending
        resourceData: null,
    });
    expect(result.current.queryState).toBe('idle');
});
```

### ❌ Over-mocking

```typescript
// BAD: Mocking too much makes tests unrealistic
jest.mock('../contexts/HalRouteContext', () => ({
    HalRouteProvider: jest.fn(),
    useHalRoute: jest.fn(),
    HalSubresourceProvider: jest.fn(),
}));

// GOOD: Mock only what's necessary
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(), // Only mock the hook
}));
```

## Mock Data Patterns

### Mock Factory Functions

Use factory functions in `__mocks__` for consistent test data:

```typescript
// __mocks__/halData.ts
export function mockHalResponse(overrides?: Partial<HalResponse>): HalResponse {
    return {
        id: '1',
        _links: {
            self: {href: '/api/items/1'},
        },
        ...overrides,
    };
}

// In tests
const mockData = mockHalResponse({name: 'Custom Name'});
```

## Best Practices Summary

1. **Test behavior, not implementation** - Use screen.getByText, screen.getByTestId
2. **Keep tests isolated** - Disable caching, clear mocks in beforeEach
3. **Mock strategically** - Only mock child components, not providers
4. **Test error states** - Always test null, undefined, and error cases
5. **Use data-testid carefully** - Only for elements without semantic role
6. **Document complex setups** - Explain why specific mocks are needed
7. **Avoid time-dependent tests** - Use waitFor instead of setTimeout
8. **Test from user perspective** - What does the user see/interact with?

## Related Files

- `/home/davca/Documents/Devel/klabis/frontend-2/src/contexts/HalRouteContext.test.tsx` - Hook testing examples
- `/home/davca/Documents/Devel/klabis/frontend-2/src/pages/FinancesPage.test.tsx` - Component integration testing
- `/home/davca/Documents/Devel/klabis/frontend-2/src/pages/GenericHalPage.test.tsx` - Error handling patterns
- `/home/davca/Documents/Devel/klabis/frontend-2/src/__mocks__/halData.ts` - Mock factory functions
