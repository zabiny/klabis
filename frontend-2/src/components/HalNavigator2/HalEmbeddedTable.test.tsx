import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalEmbeddedTable} from './HalEmbeddedTable';
import {TableCell} from '../KlabisTable';
import * as HalRouteContext from '../../contexts/HalRouteContext';

describe('HalEmbeddedTable', () => {
    const mockResourceData = {
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
    };

    beforeEach(() => {
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue(mockHalRouteValue);
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

    it('displays empty message when collection has no data', () => {
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
            ...mockHalRouteValue,
            resourceData: {
                ...mockResourceData,
                _embedded: {items: []},
            } as any,
        });

        render(
            <HalEmbeddedTable collectionName="items">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        expect(screen.getByText('Žádná data')).toBeInTheDocument();
    });

    it('displays custom empty message', () => {
        jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
            ...mockHalRouteValue,
            resourceData: {
                ...mockResourceData,
                _embedded: {items: []},
            } as any,
        });

        render(
            <HalEmbeddedTable collectionName="items" emptyMessage="Žádné položky k dispozici">
                <TableCell column="name">Name</TableCell>
            </HalEmbeddedTable>
        );

        expect(screen.getByText('Žádné položky k dispozici')).toBeInTheDocument();
    });

    it('handles sorting when defaultOrderBy is provided', () => {
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

        expect(screen.getByText('Name')).toBeInTheDocument();
    });
});
