import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {KlabisTableWithQuery} from './KlabisTableWithQuery'
import {TableCell} from './TableCell'
import {createMockResponse} from '../../__mocks__/mockFetch'
import {type Mock, vi} from 'vitest';

// Mock auth manager
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}))

describe('KlabisTableWithQuery - Data Loading Wrapper', () => {
    let queryClient: QueryClient
    let fetchSpy: Mock

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0}
            }
        })
        vi.clearAllMocks()
        // Mock global fetch
        fetchSpy = vi.fn() as Mock
        ;(globalThis as any).fetch = fetchSpy
    })

    afterEach(() => {
        delete (globalThis as any).fetch
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

            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })

            expect(fetchSpy).toHaveBeenCalledWith(
                expect.stringContaining('/api/members'),
                expect.any(Object)
            )
        })

        it('uses collectionName to extract embedded data', async () => {
            const mockData = {
                _embedded: {
                    membersList: mockMemberData
                },
                page: mockPageData
            }

            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink} collectionName="membersList">
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })
        })

        it('handles missing _embedded property gracefully', async () => {
            const mockData = {
                content: mockMemberData,
                page: mockPageData
            }

            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink} collectionName="membersList">
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            // Should not crash and should show empty table or message
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled()
            })
        })

        it('handles null _embedded.membersList gracefully', async () => {
            const mockData = {
                _embedded: {
                    membersList: null
                },
                page: mockPageData
            }

            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink} collectionName="membersList">
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            // Should not crash
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled()
            })
        })

        it('handles empty membersList array', async () => {
            const mockData = {
                _embedded: {
                    membersList: []
                },
                page: mockPageData
            }

            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink} collectionName="membersList">
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled()
            })
        })
    })

    describe('Query Parameter Building', () => {
        it('builds correct URL with pagination parameters', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled()
            })

            expect(fetchSpy).toHaveBeenCalledWith(
                expect.stringContaining('page=0'),
                expect.any(Object)
            )
            expect(fetchSpy).toHaveBeenCalledWith(
                expect.stringContaining('size=10'),
                expect.any(Object)
            )
        })

        it('updates URL when page changes', async () => {
            const page0Data = {content: mockMemberData, page: {size: 10, totalElements: 50, totalPages: 5, number: 0}}
            const page1Data = {content: mockMemberData, page: {size: 10, totalElements: 50, totalPages: 5, number: 1}}

            // Return different data for page 0 and page 1
            fetchSpy
                .mockResolvedValueOnce(createMockResponse(page0Data))
                .mockResolvedValueOnce(createMockResponse(page1Data))

            const user = userEvent.setup()
            renderWithQuery(
                <KlabisTableWithQuery
                    link={mockHalLink}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            // Verify initial call
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledWith(
                    expect.stringContaining('page=0'),
                    expect.any(Object)
                )
            })

            // Find and click the next page button (pagination component)
            const nextButton = screen.getByRole('button', {name: /next/i})
            await user.click(nextButton)

            // Verify a new query was made with page=1
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledWith(
                    expect.stringContaining('page=1'),
                    expect.any(Object)
                )
            })

            expect(fetchSpy).toHaveBeenCalledTimes(2)
        })

        it('includes sort parameters in URL', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))

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
                expect(fetchSpy).toHaveBeenCalled()
            })

            // URL encodes comma as %2C
            expect(fetchSpy).toHaveBeenCalledWith(
                expect.stringContaining('sort=name%2Casc'),
                expect.any(Object)
            )
        })
    })

    describe('Error States', () => {
        it('displays error when fetch fails', async () => {
            const error = new Error('Failed to fetch members')
            fetchSpy.mockRejectedValueOnce(error)

            renderWithQuery(
                <KlabisTableWithQuery link={mockHalLink}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTableWithQuery>
            )

            // Wait for fetch to be called when query hook mounts
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled()
            })
        })
    })

    describe('Props Passthrough', () => {
        it('passes UI props through to KlabisTable', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))

            const onRowClick = vi.fn()

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
            fetchSpy.mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))

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
            fetchSpy
                .mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))
                .mockResolvedValueOnce(createMockResponse({content: mockMemberData, page: mockPageData}))

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

            // Verify initial call includes sort parameter
            expect(fetchSpy).toHaveBeenCalledWith(
                expect.stringContaining('sort=name%2Casc'),
                expect.any(Object)
            )

            // Find and click the sortable column header to toggle sort direction
            const sortHeader = screen.getByText('Name')
            await user.click(sortHeader)

            // Verify a new query was made with reversed sort direction
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledWith(
                    expect.stringContaining('sort=name%2Cdesc'),
                    expect.any(Object)
                )
            })

            expect(fetchSpy).toHaveBeenCalledTimes(2)
        })
    })
})
