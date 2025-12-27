import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalEmbeddedTable} from './HalEmbeddedTable';
import {TableCell} from '../KlabisTable';
import * as HalRouteContext from '../../contexts/HalRouteContext';
import * as HalNavigatorHooks from '../HalNavigator/hooks';

describe('HalEmbeddedTable', () => {
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
    };

    beforeEach(() => {
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue(mockHalRouteValue);
      jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue(mockFetchResponse as any);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('renders table with data from specified collection', async () => {
        render(
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
        const {rerender} = render(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        await waitFor(() => {
            expect(screen.getByText('Item 1')).toBeInTheDocument();
        });

      // Update mock to return different collection data
      jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue({
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

        render(
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
            expect(onRowClick).toHaveBeenCalledWith(mockResourceData._embedded.items[0]);
        }
    });

  it('displays empty message when collection has no data', async () => {
    jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue({
      _embedded: {
        items: [],
      },
      page: {
        size: 10,
        totalElements: 0,
        totalPages: 1,
        number: 0,
      },
    } as any);

        render(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

    await waitFor(() => {
      expect(screen.getByText('Žádná data')).toBeInTheDocument();
    });
  });

  it('displays custom empty message', async () => {
    jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue({
      _embedded: {
        items: [],
      },
      page: {
        size: 10,
        totalElements: 0,
        totalPages: 1,
        number: 0,
      },
    } as any);

        render(
            <HalEmbeddedTable collectionName="items" emptyMessage="Žádné položky k dispozici">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

    await waitFor(() => {
      expect(screen.getByText('Žádné položky k dispozici')).toBeInTheDocument();
    });
    });

  it('handles sorting when defaultOrderBy is provided', async () => {
    const fetchSpy = jest.spyOn(HalNavigatorHooks, 'fetchResource');

        render(
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

    // Verify that fetchResource was called with sort parameter
    const callUrl = fetchSpy.mock.calls[0][0] as URL;
    expect(callUrl.searchParams.get('sort')).toBe('name,asc');
  });

  it('throws error when self link is missing', async () => {
    jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
      ...mockHalRouteValue,
      resourceData: {
        _embedded: {items: []},
        page: {size: 10, totalElements: 0, totalPages: 1, number: 0},
      } as any, // Missing _links
    });

    const consoleError = jest.spyOn(console, 'error').mockImplementation();

    render(
        <HalEmbeddedTable collectionName="items">
          <TableCell column="name">Name</TableCell>
        </HalEmbeddedTable>
    );

    // Wait for error to be caught and logged
    await waitFor(() => {
      expect(consoleError).toHaveBeenCalledWith(
          'Failed to fetch table data:',
          expect.objectContaining({
            message: expect.stringContaining('Self link not found'),
          })
      );
    });

    // Verify empty message is shown
    expect(screen.getByText('Žádná data')).toBeInTheDocument();

    consoleError.mockRestore();
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

    jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue(multiPageMock as any);
    const fetchSpy = jest.spyOn(HalNavigatorHooks, 'fetchResource');
    const user = userEvent.setup();

    render(
        <HalEmbeddedTable collectionName="items">
          <TableCell column="name">Name</TableCell>
        </HalEmbeddedTable>
    );

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Item 1')).toBeInTheDocument();
    });

    // Verify initial call has page=0, size=10
    let callUrl = fetchSpy.mock.calls[0][0] as URL;
    expect(callUrl.searchParams.get('page')).toBe('0');
    expect(callUrl.searchParams.get('size')).toBe('10');

    // Change page size (mock the response with new size)
    jest.spyOn(HalNavigatorHooks, 'fetchResource').mockResolvedValue({
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
    } as any);

    // Click on page size dropdown and select 25
    const select = screen.getByDisplayValue('10');
    await user.selectOptions(select, '25');

    // Verify the fetch was called with new size
    await waitFor(() => {
      const lastCall = fetchSpy.mock.calls[fetchSpy.mock.calls.length - 1][0] as URL;
      expect(lastCall.searchParams.get('size')).toBe('25');
    });
    });
});
