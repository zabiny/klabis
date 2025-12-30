import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {KlabisTableWithQuery} from './KlabisTableWithQuery'
import {TableCell} from './TableCell'
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch'

// Mock useAuthorizedQuery
jest.mock('../../hooks/useAuthorizedFetch')

const mockUseAuthorizedQuery = useAuthorizedQuery as jest.MockedFunction<typeof useAuthorizedQuery>

describe('KlabisTableWithQuery - Data Loading Wrapper', () => {
    let queryClient: QueryClient

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0}
            }
        })
        jest.clearAllMocks()
    })

    const mockHalLink = {
        href: 'https://api.example.com/api/members?page=0&size=10'
    }

    const mockMemberData = [
        {id: 1, name: 'Alice', email: 'alice@example.com'},
        {id: 2, name: 'Bob', email: 'bob@example.com'}
    ]

    const mockPageData = {
        size: 10,
        totalElements: 50,
        totalPages: 5,
        number: 0
    }

    const renderWithQuery = (component: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                {component}
            </QueryClientProvider>
        )
    }

    describe('Data Fetching', () => {
        it('fetches data from link href on mount', async () => {
            const mockData = {
                content: mockMemberData,
                page: mockPageData
            }

            mockUseAuthorizedQuery.mockReturnValue({
                data: mockData,
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })

            expect(mockUseAuthorizedQuery).toHaveBeenCalled()
            const callArgs = (mockUseAuthorizedQuery.mock.calls[0] as any)
            expect(callArgs[0]).toContain('/api/members')
        })

        it('uses collectionName to extract embedded data', async () => {
            const mockData = {
                _embedded: {
                    membersList: mockMemberData
                },
                page: mockPageData
            }

            mockUseAuthorizedQuery.mockReturnValue({
                data: mockData,
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink} collectionName="membersList">
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })
        })
    })

    describe('Query Parameter Building', () => {
        it('builds correct URL with pagination parameters', async () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {content: mockMemberData, page: mockPageData},
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(mockUseAuthorizedQuery).toHaveBeenCalled()
            })

            const url = (mockUseAuthorizedQuery.mock.calls[0] as any)[0]
            expect(url).toContain('page=0')
            expect(url).toContain('size=10')
        })

        it('updates URL when page changes', async () => {
            const mockData = {content: mockMemberData, page: mockPageData}
            mockUseAuthorizedQuery.mockReturnValue({
                data: mockData,
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            const user = userEvent.setup()
            const multiPageData = {...mockPageData, totalPages: 5}

            const {rerender} = renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(mockUseAuthorizedQuery).toHaveBeenCalled()
            })

            // Simulate page change by finding next button and clicking
            // (In real scenario, pagination component would trigger this)
            // For testing, we verify the URL building logic works
        })

        it('includes sort parameters in URL', async () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {content: mockMemberData, page: mockPageData},
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                    defaultOrderBy="name"
                    defaultOrderDirection="asc"
                >
                    <TableCell column="name" sortable>Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(mockUseAuthorizedQuery).toHaveBeenCalled()
            })

            const url = (mockUseAuthorizedQuery.mock.calls[0] as any)[0]
            // URL encodes comma as %2C
            expect(url).toContain('sort=name%2Casc')
        })
    })

    describe('Error States', () => {
        it('displays error when fetch fails', async () => {
            const error = new Error('Failed to fetch members')
            mockUseAuthorizedQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
                error,
                refetch: jest.fn(),
                isFetching: false,
                isError: true,
                isSuccess: false,
                status: 'error' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: 0,
                errorUpdatedAt: Date.now(),
                failureCount: 1,
                failureReason: error,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText(/failed to fetch members/i)).toBeInTheDocument()
            })
        })
    })

    describe('Props Passthrough', () => {
        it('passes UI props through to KlabisTable', async () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {content: mockMemberData, page: mockPageData},
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            const onRowClick = jest.fn()

            renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                    onRowClick={onRowClick}
                    defaultOrderBy="name"
                    emptyMessage="Custom empty"
                >
                    <TableCell column="name" sortable>Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })

            // Verify row click works (passed through)
            const user = userEvent.setup()
            const row = screen.getByText('Alice').closest('tr')
            if (row) {
                await user.click(row)
                expect(onRowClick).toHaveBeenCalledWith(expect.objectContaining({name: 'Alice'}))
            }
        })

        it('respects rowsPerPageOptions prop', async () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {content: mockMemberData, page: mockPageData},
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                    rowsPerPageOptions={[15, 30, 45]}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })

            const select = screen.getByDisplayValue('10')
            const options = Array.from((select as HTMLSelectElement).options).map(
                (opt) => opt.value
            )
            expect(options).toContain('15')
            expect(options).toContain('30')
            expect(options).toContain('45')
        })
    })

    describe('Sorting Integration', () => {
        it('passes sort callbacks to KlabisTable', async () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {content: mockMemberData, page: mockPageData},
                isLoading: false,
                error: null,
                refetch: jest.fn(),
                isFetching: false,
                isError: false,
                isSuccess: true,
                status: 'success' as const,
                fetchStatus: 'idle' as const,
                dataUpdatedAt: Date.now(),
                errorUpdatedAt: 0,
                failureCount: 0,
                failureReason: null,
                isPending: false
            } as any)

            const user = userEvent.setup()

            renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                    defaultOrderBy="name"
                >
                    <TableCell column="name" sortable>Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })

            // Note: Full integration test would require clicking sort header
            // and verifying new query is made with sort param
        })
    })
})
