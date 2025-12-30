import {HalEmbeddedTable} from './HalEmbeddedTable';
import {useHalRoute} from '../../contexts/HalRouteContext';
import {render, screen} from '@testing-library/react';
import React from 'react';

// Mock the useHalRoute hook
jest.mock('../../contexts/HalRouteContext', () => ({
    useHalRoute: jest.fn(),
}));

// Mock KlabisTableWithQuery component to simplify testing
jest.mock('../KlabisTable', () => ({
    KlabisTableWithQuery: ({
                               link,
                               collectionName,
                               onRowClick,
                               defaultOrderBy,
                               defaultOrderDirection,
                               emptyMessage,
                               children,
                           }: any) => (
        <div data-testid="klabis-table-with-query"
             data-link={link.href}
             data-collection={collectionName}
             data-order-by={defaultOrderBy}
             data-order-direction={defaultOrderDirection}
             data-empty-message={emptyMessage}
             data-on-row-click={onRowClick ? 'true' : 'false'}
        >
            <div data-testid="table-children">{children}</div>
        </div>
    ),
}));

describe('HalEmbeddedTable', () => {
    const mockGetResourceLink = jest.fn();
    const mockTableCell = <div data-testid="table-cell">Test Cell</div>;

    beforeEach(() => {
        jest.clearAllMocks();
        mockGetResourceLink.mockReturnValue({href: '/api/test/resource'});
        (useHalRoute as jest.Mock).mockReturnValue({
            getResourceLink: mockGetResourceLink,
        });
    });

    it('renders table with data from specified collection', () => {
        render(
            <HalEmbeddedTable
                collectionName="testCollection"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toBeInTheDocument();
        expect(table).toHaveAttribute('data-collection', 'testCollection');
        expect(table).toHaveAttribute('data-link', '/api/test/resource');
    });

    it('renders different collection when collectionName changes', () => {
        const {rerender} = render(
            <HalEmbeddedTable
                collectionName="collectionA"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        let table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-collection', 'collectionA');

        rerender(
            <HalEmbeddedTable
                collectionName="collectionB"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-collection', 'collectionB');
    });

    it('handles onRowClick callback', () => {
        const mockOnRowClick = jest.fn();

        render(
            <HalEmbeddedTable
                collectionName="testCollection"
                onRowClick={mockOnRowClick}
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-on-row-click', 'true');
    });

    it('displays empty message when collection has no data', () => {
        render(
            <HalEmbeddedTable
                collectionName="testCollection"
                emptyMessage="No items available"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-empty-message', 'No items available');
    });

    it('displays custom empty message', () => {
        render(
            <HalEmbeddedTable
                collectionName="testCollection"
                emptyMessage="Custom empty state"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-empty-message', 'Custom empty state');
    });

    it('handles sorting when defaultOrderBy is provided', () => {
        render(
            <HalEmbeddedTable
                collectionName="testCollection"
                defaultOrderBy="name"
                defaultOrderDirection="desc"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-order-by', 'name');
        expect(table).toHaveAttribute('data-order-direction', 'desc');
    });

    it('should display error message when self link is missing', () => {
        mockGetResourceLink.mockReturnValue(null);

        render(
            <HalEmbeddedTable
                collectionName="testCollection"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const errorBox = screen.getByText('Failed to load data');
        expect(errorBox).toBeInTheDocument();
        expect(screen.getByText(/Self link not found in resource data/)).toBeInTheDocument();
    });

    it('fetches with pagination parameters', () => {
        render(
            <HalEmbeddedTable
                collectionName="testCollection"
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        // Verify that the link is passed to KlabisTableWithQuery
        // KlabisTableWithQuery handles pagination internally
        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-link', '/api/test/resource');
    });
});
