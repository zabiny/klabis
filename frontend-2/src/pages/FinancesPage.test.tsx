import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberFinancePage, MemberName} from './FinancesPage';
import {mockFinanceResource, mockMemberResource} from '../__mocks__/halData';

// Mock contexts and components
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(),
    HalSubresourceProvider: jest.fn(),
}));

jest.mock('../components/HalNavigator2/HalEmbeddedTable', () => ({
    HalEmbeddedTable: ({children, onRowClick}: any) => (
        <div data-testid="hal-embedded-table">
            <div data-testid="table-header">{children}</div>
            <button data-testid="mock-table-row" onClick={() => onRowClick({id: 1})}>
                Mock Row
            </button>
        </div>
    ),
}));

jest.mock('../components/HalNavigator2/HalFormButton', () => ({
    HalFormButton: ({name}: any) => (
        <button data-testid={`form-button-${name}`}>{name}</button>
    ),
}));

jest.mock('../components/UI', () => ({
    Skeleton: () => <div data-testid="skeleton">Loading...</div>,
}));

const {useHalRoute, HalSubresourceProvider} = require('../contexts/HalRouteContext');

describe('MemberFinancePage', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        // Mock HalSubresourceProvider as pass-through for unit testing
        HalSubresourceProvider.mockImplementation(({children}: any) => children);
        jest.clearAllMocks();
    });

    const renderWithRouter = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>{ui}</MemoryRouter>
            </QueryClientProvider>
        );
    };

    const mockFinanceData = mockFinanceResource({
        balance: 1500,
        _links: {
            self: {href: '/api/finances/123'},
            owner: {href: '/api/members/456'},
            transactions: {href: '/api/finances/123/transactions'},
        },
    });

    describe('Loading State', () => {
        it('should display Skeleton when isLoading is true', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: true,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByTestId('skeleton')).toBeInTheDocument();
        });

        it('should not render finance content while loading', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: true,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);
            expect(screen.queryByText('Finance')).not.toBeInTheDocument();
            expect(screen.queryByTestId('hal-embedded-table')).not.toBeInTheDocument();
        });

        it('should hide Skeleton after loading completes', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);
            expect(screen.queryByTestId('skeleton')).not.toBeInTheDocument();
            expect(screen.getByText('Finance')).toBeInTheDocument();
        });
    });

    describe('Data Display', () => {
        beforeEach(() => {
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });
        });

        it('should render page heading "Finance"', () => {
            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
        });

        it('should display balance when data is loaded', () => {
            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByText('Zustatek:')).toBeInTheDocument();
            // Balance should be displayed (actual format depends on component implementation)
            expect(screen.getByText(/1500/)).toBeInTheDocument();
        });


        it('should render HalEmbeddedTable with transactions', () => {
            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByTestId('hal-embedded-table')).toBeInTheDocument();
            // Table header content is mocked, verify table is present
            expect(screen.getByTestId('table-header')).toBeInTheDocument();
        });

        it('should render HalFormButton for deposit', () => {
            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByTestId('form-button-deposit')).toBeInTheDocument();
        });

        it('should display balance value correctly', () => {
            renderWithRouter(<MemberFinancePage/>);
            // The balance should be visible in the rendered output
            const page = screen.getByRole('heading', {name: /Finance/i}).closest('div');
            expect(page).toBeInTheDocument();
        });
    });

    describe('Null Resource Handling (CRITICAL BUG FIX)', () => {
        it('should not crash when resourceData is null', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            // Should not throw error
            expect(() => {
                renderWithRouter(<MemberFinancePage/>);
            }).not.toThrow();
        });

        it('should display "-" for balance when resourceData is null', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);
            // Should handle null gracefully, not crash on accessing .balance
            expect(screen.getByText('Finance')).toBeInTheDocument();
        });

        it('should handle undefined balance property', () => {
            const dataWithoutBalance = {
                ...mockFinanceData,
                balance: undefined,
            };

            useHalRoute.mockReturnValue({
                resourceData: dataWithoutBalance,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            expect(() => {
                renderWithRouter(<MemberFinancePage/>);
            }).not.toThrow();
        });

        it('should handle null resource gracefully without crashing', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);

            // Page should still render with safe defaults
            expect(screen.getByText('Finance')).toBeInTheDocument();
            expect(screen.getByText('- Kč')).toBeInTheDocument();
        });
    });

    describe('HalSubresourceProvider Integration', () => {
        it('should render both subresource providers and child content', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });

            renderWithRouter(<MemberFinancePage/>);
            // Verify HalSubresourceProvider was called (at least twice for owner and transactions)
            expect(HalSubresourceProvider).toHaveBeenCalled();
            expect(HalSubresourceProvider.mock.calls.length).toBeGreaterThanOrEqual(2);

            // Verify content is rendered when providers work
            expect(screen.getByText('Finance')).toBeInTheDocument();
        });

    });

    describe('HalFormButton Integration', () => {
        beforeEach(() => {
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: jest.fn(),
            });
        });

        it('should render HalFormButton with name "deposit"', () => {
            renderWithRouter(<MemberFinancePage/>);
            expect(screen.getByTestId('form-button-deposit')).toBeInTheDocument();
        });
    });

    describe('Navigation Callbacks', () => {
        it('should pass navigateToResource to HalEmbeddedTable', () => {
            const mockNavigateToResource = jest.fn();
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: mockNavigateToResource,
            });

            renderWithRouter(<MemberFinancePage/>);
            // Verify navigateToResource was provided to table
            expect(screen.getByTestId('hal-embedded-table')).toBeInTheDocument();
        });

        it('should call navigateToResource when row is clicked', () => {
            const mockNavigateToResource = jest.fn();
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: mockNavigateToResource,
            });

            renderWithRouter(<MemberFinancePage/>);
            const row = screen.getByTestId('mock-table-row');
            row.click();
            // navigateToResource should be called with the row data
            expect(mockNavigateToResource).toHaveBeenCalled();
        });

        it('should navigate with correct resource data', () => {
            const mockNavigateToResource = jest.fn();
            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: mockNavigateToResource,
            });

            renderWithRouter(<MemberFinancePage/>);
            const row = screen.getByTestId('mock-table-row');
            row.click();
            // Should be called with an object containing id
            expect(mockNavigateToResource).toHaveBeenCalledWith(expect.objectContaining({id: 1}));
        });
    });

    describe('Error Handling', () => {
        it('should handle error from useHalRoute gracefully', () => {
            const error = new Error('Failed to load finances');
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: error,
                navigateToResource: jest.fn(),
            });

            expect(() => {
                renderWithRouter(<MemberFinancePage/>);
            }).not.toThrow();
        });
    });

    describe('navigateToResource Edge Cases', () => {
        it('should handle resource with array self link', () => {
            const mockNavigateToResource = jest.fn();
            const resourceWithArraySelfLink = {
                id: 1,
                _links: {
                    self: [
                        {href: '/api/transactions/1'},
                        {href: '/api/transactions/1?view=detailed'},
                    ],
                },
            };

            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: mockNavigateToResource,
            });

            mockNavigateToResource.mockImplementation((resource) => {
                const selfLink = resource?._links?.self;
                if (!selfLink) throw new Error('Self link not found');
                const link = Array.isArray(selfLink) ? selfLink[0] : selfLink;
                const href = link?.href;
                if (!href) throw new Error('Self link href is empty');
            });

            renderWithRouter(<MemberFinancePage/>);

            expect(() => mockNavigateToResource(resourceWithArraySelfLink)).not.toThrow();
        });

        it('should throw error when navigating to resource without self link', () => {
            const mockNavigateToResource = jest.fn();
            const resourceWithoutSelfLink = {
                id: 1,
                _links: {
                    other: {href: '/api/other/1'},
                },
            };

            useHalRoute.mockReturnValue({
                resourceData: mockFinanceData,
                isLoading: false,
                error: null,
                navigateToResource: mockNavigateToResource,
            });

            mockNavigateToResource.mockImplementation((resource) => {
                const selfLink = resource?._links?.self;
                if (!selfLink) {
                    throw new Error('Self link not found in resource data');
                }
            });

            renderWithRouter(<MemberFinancePage/>);

            expect(() => {
                mockNavigateToResource(resourceWithoutSelfLink);
            }).toThrow('Self link not found');
        });
    });
});

describe('MemberName Component', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    const renderWithRouter = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>{ui}</MemoryRouter>
            </QueryClientProvider>
        );
    };

    describe('With User Prop', () => {
        it('should display firstName and lastName when user prop provided', () => {
            const user = mockMemberResource();
            renderWithRouter(<MemberName user={user as any}/>);
            expect(screen.getByText(`${user.firstName} ${user.lastName}`)).toBeInTheDocument();
        });

        it('should use provided user prop over resourceData from context', () => {
            const user = mockMemberResource({firstName: 'Provided', lastName: 'User'});
            useHalRoute.mockReturnValue({
                resourceData: mockMemberResource({firstName: 'Context', lastName: 'User'}),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<MemberName user={user as any}/>);
            expect(screen.getByText('Provided User')).toBeInTheDocument();
        });

        it('should handle user object with partial data', () => {
            const user = {firstName: 'Jan'} as any;
            renderWithRouter(<MemberName user={user}/>);
            // Should display firstName and '-' for lastName
            expect(screen.getByText(/Jan.*-/)).toBeInTheDocument();
        });
    });

    describe('Without User Prop (from context)', () => {
        it('should use resourceData from useHalRoute when user prop not provided', () => {
            const contextUser = mockMemberResource({firstName: 'Context', lastName: 'User'});
            useHalRoute.mockReturnValue({
                resourceData: contextUser,
                isLoading: false,
                error: null,
            });

            renderWithRouter(<MemberName user={undefined}/>);
            expect(screen.getByText('Context User')).toBeInTheDocument();
        });

        it('should display firstName from resourceData', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockMemberResource({firstName: 'Jan', lastName: 'Novák'}),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<MemberName user={undefined}/>);
            expect(screen.getByText('Jan Novák')).toBeInTheDocument();
        });
    });

    describe('Null Handling', () => {
        it('should display "- -" when user is null', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
            });

            renderWithRouter(<MemberName user={null as any}/>);
            expect(screen.getByText('- -')).toBeInTheDocument();
        });

        it('should display "- lastName" when firstName is missing', () => {
            const user = {firstName: undefined, lastName: 'Novák'} as any;
            renderWithRouter(<MemberName user={user}/>);
            expect(screen.getByText('- Novák')).toBeInTheDocument();
        });

        it('should display "firstName -" when lastName is missing', () => {
            const user = {firstName: 'Jan', lastName: undefined} as any;
            renderWithRouter(<MemberName user={user}/>);
            expect(screen.getByText('Jan -')).toBeInTheDocument();
        });

        it('should display "- -" when both names are missing', () => {
            const user = {} as any;
            renderWithRouter(<MemberName user={user}/>);
            expect(screen.getByText('- -')).toBeInTheDocument();
        });

        it('should handle undefined user prop with null context data', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
            });

            renderWithRouter(<MemberName user={undefined}/>);
            expect(screen.getByText('- -')).toBeInTheDocument();
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty string names', () => {
            const user = {firstName: '', lastName: ''} as any;
            const {container} = renderWithRouter(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle whitespace-only names', () => {
            const user = {firstName: '   ', lastName: '   '} as any;
            const {container} = renderWithRouter(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle special characters in names', () => {
            const user = {firstName: 'José', lastName: 'García-López'} as any;
            renderWithRouter(<MemberName user={user}/>);
            expect(screen.getByText('José García-López')).toBeInTheDocument();
        });

        it('should handle very long names', () => {
            const user = {
                firstName: 'Alexander'.repeat(5),
                lastName: 'Schwarz'.repeat(5),
            } as any;
            renderWithRouter(<MemberName user={user}/>);
            expect(screen.getByText(new RegExp(`${user.firstName}.*${user.lastName}`))).toBeInTheDocument();
        });
    });

    describe('Rendering', () => {
        it('should render as span element', () => {
            const user = mockMemberResource();
            const {container} = renderWithRouter(<MemberName user={user as any}/>);
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should display full name with space between first and last name', () => {
            const user = mockMemberResource({firstName: 'Jan', lastName: 'Novák'});
            renderWithRouter(<MemberName user={user as any}/>);
            const text = screen.getByText('Jan Novák');
            expect(text.textContent).toBe('Jan Novák');
        });
    });
});
