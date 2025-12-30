# Frontend Testing Patterns

This document describes testing patterns and best practices discovered through test reviews and implementation in the
klabis project. Tests use **Vitest** as the test runner with React Testing Library for component testing.

## Running Tests

- `npm test` - Run all tests
- `npm run test:watch` - Run tests in watch mode
- `npm run test:coverage` - Run tests with coverage report
- `npm run test:ui` - Run tests with interactive UI

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
import {vi} from 'vitest';

vi.mock('../components/UI', () => ({
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
import {vi} from 'vitest';

// BAD: Mocking HalRouteProvider completely
vi.mock('../contexts/HalRouteContext', () => ({
  HalRouteProvider: vi.fn(({children}) => children),
}));

// GOOD: Mock useHalRoute hook and set up real provider
vi.mock('../contexts/HalRouteContext', async () => ({
  ...(await vi.importActual('../contexts/HalRouteContext')),
  useHalRoute: vi.fn(), // Only mock the hook
}));
```

## Context Provider Testing

### Hook Testing with Wrappers

When testing context hooks with `renderHook`, create a wrapper component that includes all necessary providers.

**Example** (from HalRouteContext.test.tsx):

```typescript
import {renderHook} from '@testing-library/react';

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
import React from 'react';
import {MemberFinancePage} from './FinancesPage';
import {HalRouteProvider} from '../contexts/HalRouteContext';

// 1. Mock ONLY the API boundary - preserve other hook exports
jest.mock('../components/HalNavigator/hooks', () => {
    const actualModule = jest.requireActual('../components/HalNavigator/hooks');
    return {
        ...actualModule,  // Preserve toHref, toURLPath, etc.
        fetchResource: jest.fn(),  // Only mock the API call
    };
});

const {fetchResource} = require('../components/HalNavigator/hooks');

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

    // For integration tests with real HalRouteProvider (primary pattern)
    const renderPage = (ui: React.ReactElement, initialRoute = '/finances/123') => {
        return render(
            <QueryClientProvider client = {queryClient} >
            <MemoryRouter initialEntries = {[initialRoute]} >
                <HalRouteProvider>
                    {ui}
                < /HalRouteProvider>
                < /MemoryRouter>
                < /QueryClientProvider>
        );
    };
});
```

**Key differences from unit tests**:

- Do NOT mock `HalEmbeddedTable`, `HalFormButton`, or other child components
- Let real child components render so you test integration with them
- Use real `HalRouteProvider` wrapped in `MemoryRouter` for integration tests
- Mocking only the API boundary (`fetchResource`) keeps tests fast and isolated
- **Important**: When mocking modules with multiple exports, preserve other exports using `...actualModule` to avoid
  breaking dependencies

### Testing Data Loading Flow

Test that the page correctly fetches and displays data using real child components:

```typescript
describe('Data Loading and Display', () => {
    beforeEach(() => {
        // Mock API to return finance data when HalRouteProvider fetches
        fetchResource.mockResolvedValue({
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                owner: {href: '/api/members/456'},
                transactions: {href: '/api/finances/123/transactions'},
            },
            _templates: {
                // Include HAL Forms templates for form buttons
                deposit: {
                    title: 'Deposit',
                    method: 'POST',
                    contentType: 'application/x-www-form-urlencoded',
                    properties: [{name: 'amount', prompt: 'Amount', required: true}],
                    target: '/api/finances/123/deposit',
                },
            },
        });
    });

    it('should render page heading and content when data loads', async () => {
        renderPage(<MemberFinancePage / >);

        // Use semantic queries instead of data-testid - tests real component behavior
        await waitFor(() => {
            expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            expect(screen.getByText(/1500/)).toBeInTheDocument();
        });
    });

    it('should render real child components with fetched data', async () => {
        renderPage(<MemberFinancePage / >);

        // Wait for async data loading from real HalRouteProvider
        await waitFor(() => {
            // Real HalEmbeddedTable renders with semantic role
            expect(screen.getByRole('table')).toBeInTheDocument();
            // Real HalFormButton renders with semantic role
            expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
        });
    });
});
```

**Important practical details**:

- **Mock data must be complete**: Include `_templates` for HAL Forms if the page renders form buttons
- **Use async/await**: Real `HalRouteProvider` fetches asynchronously, so use `await waitFor()`
- **Use semantic queries**: Queries like `getByRole()` and `getByText()` work with real components
- **Set up mocks in beforeEach**: Simpler than mocking useHalRoute for each test
- **Don't mock useHalRoute for integration tests**: Let the real provider manage data flow

### Testing Subresource Loading

When pages use `HalSubresourceProvider` for nested data, test with real subresource providers and components:

```typescript
describe('Subresource Integration', () => {
    it('should load and display finance data through real child components', async () => {
        // Mock fetchResource for all data loads (parent + subresources)
        fetchResource
            .mockResolvedValueOnce({
                // Parent finance resource
                balance: 1500,
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                    transactions: {href: '/api/finances/123/transactions'},
                },
                _templates: {
                    deposit: {
                        title: 'Deposit',
                        method: 'POST',
                        contentType: 'application/x-www-form-urlencoded',
                        properties: [{name: 'amount', prompt: 'Amount', required: true}],
                        target: '/api/finances/123/deposit',
                    },
                },
            })
            .mockResolvedValueOnce({
                // Owner subresource - fetched by HalSubresourceProvider
                firstName: 'Jan',
                lastName: 'Novák',
            })
            .mockResolvedValueOnce({
                // Transactions subresource - fetched by HalSubresourceProvider
                _embedded: {
                    transactionItemResponseList: [
                        {id: 1, date: '2025-12-01', amount: 100, _links: {self: {href: '/api/transactions/1'}}},
                    ],
                },
            });

        renderPage(<MemberFinancePage / >);

        // Wait for all async subresource loads via real HalSubresourceProvider
        await waitFor(() => {
            // Parent data displays
            expect(screen.getByText('Finance')).toBeInTheDocument();
            expect(screen.getByText(/1500/)).toBeInTheDocument();

            // Subresource data displays through real child components
            expect(screen.getByText('Jan Novák')).toBeInTheDocument(); // Real MemberName component
            expect(screen.getByRole('table')).toBeInTheDocument();      // Real HalEmbeddedTable component
            expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
        });
    });
});
```

**Key points for subresource testing**:

- Use real `HalSubresourceProvider` (it's imported in your component)
- Mock `fetchResource` to return data in the order subresources are fetched
- Include necessary data structures (`_embedded` for lists, `_links` for navigation)
- Include `_templates` for HAL Forms rendered in child components
- All data loads happen asynchronously, so use `await waitFor()`

### Testing Error States

Test how pages handle API failures by mocking fetchResource to reject:

```typescript
describe('Error Handling', () => {
    it('should handle API fetch errors gracefully', async () => {
        // Mock fetchResource to reject (simulates API failure)
        fetchResource.mockRejectedValueOnce(new Error('HTTP 500: Internal Server Error'));

        renderPage(<MemberFinancePage / >);

        // Page should render without crashing even if API fails
        await waitFor(() => {
            expect(screen.getByText('Finance')).toBeInTheDocument();
            // Default/safe content renders when API fails
        });
    });

    it('should display null safety defaults when resource is null', () => {
        // Mock fetchResource to return null (resource not found)
        fetchResource.mockResolvedValueOnce(null);

        renderPage(<MemberFinancePage / >);

        // Page displays with safe defaults
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText('- Kč')).toBeInTheDocument(); // Default balance display
    });
});
```

**Error handling notes**:

- Real `HalRouteProvider` catches fetch errors automatically
- No need to mock useHalRoute for error testing with real providers
- Test that the page gracefully displays default content on errors
- Verify the component doesn't crash when API fails

### Testing Navigation and User Interactions

Test that page components correctly handle user actions through real child components:

```typescript
describe('Navigation Callbacks', () => {
    beforeEach(() => {
        // Mock finance data with transactions
        fetchResource.mockResolvedValue({
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                transactions: {href: '/api/finances/123/transactions'},
            },
            _templates: {
                deposit: {
                    title: 'Deposit',
                    method: 'POST',
                    contentType: 'application/x-www-form-urlencoded',
                    properties: [{name: 'amount', prompt: 'Amount', required: true}],
                    target: '/api/finances/123/deposit',
                },
            },
        });
    });

    it('should render real table with navigation support', async () => {
        renderPage(<MemberFinancePage / >);

        // Real HalEmbeddedTable renders with semantic role
        await waitFor(() => {
            expect(screen.getByRole('table')).toBeInTheDocument();
        });
    });

    it('should render real form buttons for HAL Forms actions', async () => {
        renderPage(<MemberFinancePage / >);

        // HAL Forms buttons render through real HalFormButton component
        await waitFor(() => {
            expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
        });
    });
});
```

**Navigation testing notes**:

- Real `HalEmbeddedTable` provides navigation through row click handlers
- Don't mock navigation function for integration tests—test that UI renders correctly
- HAL Forms buttons (like "deposit") render only when `_templates` are present in mock data
- User interactions happen through real child components with semantic roles

### Testing Null Safety

Pages must handle null data gracefully with real providers:

```typescript
describe('Null Safety', () => {
    it('should render with safe defaults when resource is null', () => {
        // Mock fetchResource to return null (resource not found)
        fetchResource.mockResolvedValueOnce(null);

        renderPage(<MemberFinancePage / >);

        // Page should still render with safe defaults, not crash
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText('- Kč')).toBeInTheDocument(); // Default for missing balance
    });

    it('should handle missing optional properties gracefully', () => {
        // Mock with minimal data
        fetchResource.mockResolvedValueOnce({
            _links: {
                self: {href: '/api/finances/123'},
            },
        });

        expect(() => {
            renderPage(<MemberFinancePage / >);
        }).not.toThrow();

        // Page renders with safe defaults
        expect(screen.getByText('Finance')).toBeInTheDocument();
    });

    it('should not crash when subresources fail to load', () => {
        // Parent loads successfully
        fetchResource.mockResolvedValueOnce({
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                owner: {href: '/api/members/456'},
                transactions: {href: '/api/finances/123/transactions'},
            },
        });

        // Subresource fetch fails
        fetchResource.mockRejectedValueOnce(new Error('Subresource not found'));

        expect(() => {
            renderPage(<MemberFinancePage / >);
        }).not.toThrow();

        // Parent data still displays
        expect(screen.getByText('Finance')).toBeInTheDocument();
    });
});
```

**Null safety testing notes**:

- Use real `HalRouteProvider`—it handles errors and null data automatically
- No need to mock useHalRoute for null/error scenarios
- Test that pages display gracefully with safe defaults
- Verify components don't crash when data is missing

### Real-World Example: MemberFinancePage

Full test example from `FinancesPage.test.tsx` using real child components and `HalRouteProvider`:

```typescript
describe('MemberFinancePage', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    // Use real HalRouteProvider for integration testing
    const renderPage = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client = {queryClient} >
            <MemoryRouter initialEntries = {['/finances/123']} >
                <HalRouteProvider>
                    {ui}
                < /HalRouteProvider>
                < /MemoryRouter>
                < /QueryClientProvider>
        );
    };

    it('should load and display finance data through real child components', async () => {
        fetchResource.mockResolvedValue({
            balance: 1500,
            _links: {
                self: {href: '/api/finances/123'},
                owner: {href: '/api/members/456'},
                transactions: {href: '/api/finances/123/transactions'},
            },
            _templates: {
                deposit: {
                    title: 'Deposit',
                    method: 'POST',
                    contentType: 'application/x-www-form-urlencoded',
                    properties: [{name: 'amount', prompt: 'Amount', required: true}],
                    target: '/api/finances/123/deposit',
                },
            },
        });

        renderPage(<MemberFinancePage / >);

        // Real child components render with fetched data
        await waitFor(() => {
            expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            expect(screen.getByText(/1500/)).toBeInTheDocument();
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
        });
    });

    it('should handle null resource gracefully with real children', () => {
        fetchResource.mockResolvedValueOnce(null);

        renderPage(<MemberFinancePage / >);

        // Page renders with safe defaults
        expect(screen.getByText('Finance')).toBeInTheDocument();
        expect(screen.getByText('- Kč')).toBeInTheDocument();
    });

    it('should handle API errors gracefully', () => {
        fetchResource.mockRejectedValueOnce(new Error('API Error'));

        expect(() => {
            renderPage(<MemberFinancePage / >);
        }).not.toThrow();

        // Page still displays with safe defaults
        expect(screen.getByText('Finance')).toBeInTheDocument();
    });
});
```

**Key differences from unit test approach:**

- No mocking of HalEmbeddedTable, HalFormButton, or other child components
- Uses real `HalRouteProvider` with real providers (not mocking useHalRoute)
- Tests real component integration and data flow end-to-end
- Uses semantic queries (`getByRole()`, `getByText()`) instead of `data-testid`
- Mock data includes complete structures: `_links`, `_templates`, `_embedded`
- All data loads are async—uses `await waitFor()`

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
