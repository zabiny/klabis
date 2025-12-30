import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalEmbeddedTable} from './HalEmbeddedTable';
import {TableCell} from '../KlabisTable';
import * as HalRouteContext from '../../contexts/HalRouteContext';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

jest.mock('../../hooks/useAuthorizedFetch');

const mockUseAuthorizedQuery = useAuthorizedQuery as jest.MockedFunction<typeof useAuthorizedQuery>;

describe('HalEmbeddedTable', () => {
    let queryClient: QueryClient;

    const mockResourceData = {
        _links: {
            self: {
                href: 'http://localhost:8080/api/resources',
            },
        },
        _embedded: {
            items: [
                {id: 1, name: 'Item 1', description: 'Description 1'},
                {id: 2, name: 'Item 2', description: 'Description 2'},
            ],
            otherCollection: [
                {id: 10, title: 'Other 1'},
            ],
        },
        page: {
            size: 10,
            totalElements: 2,
            totalPages: 1,
            number: 0,
        },
    };

    const mockFetchResponse = {
        _embedded: {
            items: [
                {id: 1, name: 'Item 1', description: 'Description 1'},
                {id: 2, name: 'Item 2', description: 'Description 2'},
            ],
            otherCollection: [
                {id: 10, title: 'Other 1'},
            ],
        },
        page: {
            size: 10,
            totalElements: 2,
            totalPages: 1,
            number: 0,
        },
    };

    const mockHalRouteValue: HalRouteContext.HalRouteContextValue = {
        resourceData: mockResourceData as any,
        isLoading: false,
        error: null,
        refetch: async () => {
        },
        pathname: '/test',
        queryState: 'success',
        navigateToResource: jest.fn(),
        getResourceLink: jest.fn()
    };

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0}
            }
        });
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue(mockHalRouteValue);

        mockUseAuthorizedQuery.mockReturnValue({
            data: mockFetchResponse,
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
        } as any);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    const renderWithQuery = (component: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                {component}
            </QueryClientProvider>
        );
    };

    it('renders table with data from specified collection', async () => {
        renderWithQuery(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="id">ID</TableCell>
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        // Should render table headers
        expect(screen.getByText('ID')).toBeInTheDocument();
        expect(screen.getByText('Name')).toBeInTheDocument();

        // Should render data from 'items' collection
        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
            expect(screen.getByText('Item 2')).toBeInTheDocument();
        });
    });

    it('renders different collection when collectionName changes', async () => {
        const {rerender} = renderWithQuery(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
        });

        // Update mock to return different collection data
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    otherCollection: [
                        {id: 10, title: 'Other 1'},
                    ],
                },
                page: {
                    size: 10,
                    totalElements: 1,
                    totalPages: 1,
                    number: 0,
                },
            } as any,
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
        } as any);

        // Re-render with different collection
        rerender(
            <HalEmbeddedTable collectionName="otherCollection">
                <TableCell column="title">Title</TableCell>
            </HalEmbeddedTable>
        );

        await waitFor(() => {
            expect(screen.getByText('Other 1')).toBeInTheDocument();
            expect(screen.queryByText('Item 1')).not.toBeInTheDocument();
        });
    });

    it('handles onRowClick callback', async () => {
        const onRowClick = jest.fn();
        const user = userEvent.setup();

        renderWithQuery(
            <HalEmbeddedTable collectionName="items" onRowClick={onRowClick}>
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        // Wait for data to load
        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
        });

        // Click on first row
        const firstRow = screen.getByText('Item 1').closest('tr');
        if (firstRow) {
            await user.click(firstRow);
            expect(onRowClick).toHaveBeenCalledWith(expect.objectContaining({id: 1}));
        }
    });

    it('displays empty message when collection has no data', async () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    items: [],
                },
                page: {
                    size: 10,
                    totalElements: 0,
                    totalPages: 1,
                    number: 0,
                },
            } as any,
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
        } as any);

        renderWithQuery(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        await waitFor(() => {
            expect(screen.getByText('Žádná data')).toBeInTheDocument();
        });
    });

    it('displays custom empty message', async () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    items: [],
                },
                page: {
                    size: 10,
                    totalElements: 0,
                    totalPages: 1,
                    number: 0,
                },
            } as any,
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
        } as any);

        renderWithQuery(
            <HalEmbeddedTable collectionName="items" emptyMessage="Žádné položky k dispozici">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        await waitFor(() => {
            expect(screen.getByText('Žádné položky k dispozici')).toBeInTheDocument();
        });
    });

    it('handles sorting when defaultOrderBy is provided', async () => {
        const fetchSpy = mockUseAuthorizedQuery;

        renderWithQuery(
            <HalEmbeddedTable
                collectionName="items"
                defaultOrderBy="name"
                defaultOrderDirection="asc"
            >
                <TableCell column="name" sortable>
                    Name
                </TableCell>
            </HalEmbeddedTable>
        );

        // Wait for data to load
        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
        });

        // Verify that useAuthorizedQuery was called with sort parameter
        expect(fetchSpy).toHaveBeenCalled();
        const callUrl = (fetchSpy.mock.calls[0] as any)[0] as string;
        expect(callUrl).toContain('sort=name%2Casc');
    });

    it('throws error when self link is missing', async () => {
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
            ...mockHalRouteValue,
            resourceData: {
                _embedded: {items: []},
                page: {size: 10, totalElements: 0, totalPages: 1, number: 0},
            } as any, // Missing _links
        });

        renderWithQuery(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        // Should display error message
        await waitFor(() => {
            expect(screen.getByText(/Failed to load data/i)).toBeInTheDocument();
            expect(screen.getByText(/Self link not found/i)).toBeInTheDocument();
        });
    });

    it('fetches with pagination parameters', async () => {
        const multiPageMock = {
            _embedded: {
                items: Array.from({length: 25}, (_, i) => ({
                    id: i + 1,
                    name: `Item ${i + 1}`,
                })),
            },
            page: {
                size: 25,
                totalElements: 50,
                totalPages: 2,
                number: 0,
            },
        };

        mockUseAuthorizedQuery.mockReturnValue({
            data: multiPageMock as any,
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
        } as any);

        const user = userEvent.setup();

        renderWithQuery(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        // Wait for data to load
        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
        });

        // Verify initial call has page=0, size=10
        let callUrl = (mockUseAuthorizedQuery.mock.calls[0] as any)[0] as string;
        expect(callUrl).toContain('page=0');
        expect(callUrl).toContain('size=10');

        // Change page size (mock new response with new size)
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    items: Array.from({length: 25}, (_, i) => ({
                        id: i + 1,
                        name: `Item ${i + 1}`,
                    })),
                },
                page: {
                    size: 25,
                    totalElements: 50,
                    totalPages: 2,
                    number: 0,
                },
            } as any,
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
        } as any);

        // Click on page size dropdown and select 25
        const select = screen.getByDisplayValue('10');
        await user.selectOptions(select, '25');

        // Verify the fetch was called with new size
        await waitFor(() => {
            const lastCall = (mockUseAuthorizedQuery.mock.calls[mockUseAuthorizedQuery.mock.calls.length - 1] as any)[0] as string;
            expect(lastCall).toContain('size=25');
        });
    });
});
