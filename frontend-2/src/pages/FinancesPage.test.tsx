import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberFinancePage, MemberName} from './FinancesPage';
import {mockFinanceResource, mockMemberResource} from '../__mocks__/halData';
import {HalRouteProvider} from '../contexts/HalRouteContext';

// Mock only the API boundary
jest.mock('../components/HalNavigator/hooks', () => {
    const actualModule = jest.requireActual('../components/HalNavigator/hooks');
    return {
        ...actualModule,
        fetchResource: jest.fn(),
    };
});

const {fetchResource} = require('../components/HalNavigator/hooks');

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

    // For integration tests with real HalRouteProvider and mocked API
    const renderPage = (ui: React.ReactElement, initialRoute = '/finances/123') => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialRoute]}>
                    <HalRouteProvider>
                        {ui}
                    </HalRouteProvider>
                </MemoryRouter>
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
        _templates: {
            deposit: {
                title: 'Deposit',
                method: 'POST',
                contentType: 'application/x-www-form-urlencoded',
                properties: [
                    {
                        name: 'amount',
                        prompt: 'Amount',
                        required: true,
                        type: 'number',
                    },
                ],
                target: '/api/finances/123/deposit',
            },
        },
    });

    describe('Data Loading and Display', () => {
        beforeEach(() => {
            fetchResource.mockResolvedValue(mockFinanceData);
        });

        it('should render page heading "Finance"', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should display balance when data is loaded', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
                expect(screen.getByText(/1500/)).toBeInTheDocument();
            });
        });

        it('should render transaction table with real HalEmbeddedTable', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('table')).toBeInTheDocument();
            });
        });

        it('should render deposit button through real HalFormButton', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
            });
        });
    });

    describe('Subresource Integration', () => {
        beforeEach(() => {
            fetchResource.mockResolvedValue(mockFinanceData);
        });

        it('should load and display finance data through real child components', async () => {
            renderPage(<MemberFinancePage/>);

            // Verify all real child components render with actual data
            await waitFor(() => {
                expect(screen.getByText('Finance')).toBeInTheDocument();
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
                expect(screen.getByRole('table')).toBeInTheDocument();
                expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
            });
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

    // MemberName uses useHalRoute internally, so it needs HalRouteProvider
    const renderComponent = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/members/123']}>
                    <HalRouteProvider>
                        {ui}
                    </HalRouteProvider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    describe('Name Display', () => {
        it('should display firstName and lastName when provided', () => {
            const user = mockMemberResource();
            renderComponent(<MemberName user={user as any}/>);
            expect(screen.getByText(`${user.firstName} ${user.lastName}`)).toBeInTheDocument();
        });

        it('should handle user object with partial data', () => {
            const user = {firstName: 'Jan'} as any;
            renderComponent(<MemberName user={user}/>);
            // Should display firstName and '-' for lastName
            expect(screen.getByText(/Jan.*-/)).toBeInTheDocument();
        });

        it('should display "- lastName" when firstName is missing', () => {
            const user = {firstName: undefined, lastName: 'Novák'} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('- Novák')).toBeInTheDocument();
        });

        it('should display "firstName -" when lastName is missing', () => {
            const user = {firstName: 'Jan', lastName: undefined} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('Jan -')).toBeInTheDocument();
        });

        it('should display "- -" when both names are missing', () => {
            const user = {} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('- -')).toBeInTheDocument();
        });

        it('should handle empty string names', () => {
            const user = {firstName: '', lastName: ''} as any;
            const {container} = renderComponent(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle whitespace-only names', () => {
            const user = {firstName: '   ', lastName: '   '} as any;
            const {container} = renderComponent(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle special characters in names', () => {
            const user = {firstName: 'José', lastName: 'García-López'} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('José García-López')).toBeInTheDocument();
        });

        it('should handle very long names', () => {
            const user = {
                firstName: 'Alexander'.repeat(5),
                lastName: 'Schwarz'.repeat(5),
            } as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText(new RegExp(`${user.firstName}.*${user.lastName}`))).toBeInTheDocument();
        });
    });

    describe('Rendering', () => {
        it('should render as span element', () => {
            const user = mockMemberResource();
            const {container} = renderComponent(<MemberName user={user as any}/>);
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should display full name with space between first and last name', () => {
            const user = mockMemberResource({firstName: 'Jan', lastName: 'Novák'});
            renderComponent(<MemberName user={user as any}/>);
            const text = screen.getByText('Jan Novák');
            expect(text.textContent).toBe('Jan Novák');
        });
    });
});
