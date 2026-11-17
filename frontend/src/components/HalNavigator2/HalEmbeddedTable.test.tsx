import {HalEmbeddedTable} from './HalEmbeddedTable';
import {useHalPageData} from '../../hooks/useHalPageData';
import {render, screen} from '@testing-library/react';
import type {KlabisTableWithQueryProps} from "../KlabisTable/KlabisTableWithQuery.tsx";
import {type Mock, vi} from 'vitest';

// Mock the useHalPageData hook
vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

// Mock KlabisTableWithQuery component to simplify testing
vi.mock('../KlabisTable', () => ({
    KlabisTableWithQuery: ({
                               link,
                               onRowClick,
                               defaultOrderBy,
                               defaultOrderDirection,
                               emptyMessage,
                               children,
                           }: KlabisTableWithQueryProps) => (
        <div data-testid="klabis-table-with-query"
             data-link={link.href}
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
    const mockGetResourceLink = vi.fn();
    const mockTableCell = <div data-testid="table-cell">Test Cell</div>;

    beforeEach(() => {
        vi.clearAllMocks();
        mockGetResourceLink.mockReturnValue({href: '/api/test/resource'});
        (useHalPageData as Mock).mockReturnValue({
            isLoading: false,
            getResourceLink: mockGetResourceLink,
            navigateToResource: vi.fn(),
            refetch: vi.fn(),
        });
    });

    it('renders the table for the resource self link', () => {
        render(
            <HalEmbeddedTable>
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toBeInTheDocument();
        expect(table).toHaveAttribute('data-link', '/api/test/resource');
    });

    it('handles onRowClick callback', () => {
        const mockOnRowClick = vi.fn();

        render(
            <HalEmbeddedTable
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
            <HalEmbeddedTable>
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const errorBox = screen.getByText('Data se nepodařilo načíst.');
        expect(errorBox).toBeInTheDocument();
        expect(screen.queryByText(/Self link not found/)).not.toBeInTheDocument();
    });

    it('fetches with pagination parameters', () => {
        render(
            <HalEmbeddedTable>
                {mockTableCell}
            </HalEmbeddedTable>
        );

        // Verify that the link is passed to KlabisTableWithQuery
        // KlabisTableWithQuery handles pagination internally
        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-link', '/api/test/resource');
    });

    it('appends extraParams to a relative self link without throwing (regression: Invalid URL)', () => {
        mockGetResourceLink.mockReturnValue({href: '/api/events'});

        render(
            <HalEmbeddedTable
                extraParams={{year: '2026', when: 'budouci'}}
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-link', '/api/events?year=2026&when=budouci');
    });

    it('supports multi-value extraParams on a relative self link', () => {
        mockGetResourceLink.mockReturnValue({href: '/api/events'});

        render(
            <HalEmbeddedTable
                extraParams={{status: ['OPEN', 'CLOSED']}}
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-link', '/api/events?status=OPEN&status=CLOSED');
    });

    it('normalises an absolute self link to origin-relative when appending extraParams', () => {
        mockGetResourceLink.mockReturnValue({href: 'https://api.example.com/api/events'});

        render(
            <HalEmbeddedTable
                extraParams={{year: '2026'}}
            >
                {mockTableCell}
            </HalEmbeddedTable>
        );

        const table = screen.getByTestId('klabis-table-with-query');
        expect(table).toHaveAttribute('data-link', '/api/events?year=2026');
    });
});
