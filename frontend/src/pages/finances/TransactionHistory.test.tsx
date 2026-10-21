import '@testing-library/jest-dom';
import {act, fireEvent, render, renderHook, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {type Mock, vi} from 'vitest';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';
import {TransactionsTable} from './FinancesPage.tsx';
import {TransactionFilterBar} from './TransactionFilterBar.tsx';
import {useTransactionFilters} from './useTransactionFilters.ts';
import {labels} from '../../localization';

const TX_URL = 'https://test.com/api/members/456/account/transactions';

const tx = (id: string, extra: Record<string, unknown> = {}) => ({
    id,
    occurredAt: '2025-03-10',
    amount: 500,
    currency: 'CZK',
    note: `note-${id}`,
    type: 'DEPOSIT',
    reversesTransactionId: null,
    _links: {self: {href: `${TX_URL}/${id}`}},
    ...extra,
});

const txPage = (items: unknown[], page: Record<string, number> = {}) => ({
    _embedded: {transactions: items},
    page: {totalElements: items.length, totalPages: 1, size: 10, number: 0, ...page},
    _links: {self: {href: TX_URL}},
});

describe('transaction history table', () => {
    let fetchSpy: Mock;

    beforeEach(() => {
        localStorage.clear();
        fetchSpy = vi.fn() as Mock;
        (globalThis as typeof globalThis & Record<string, unknown>).fetch = fetchSpy;
    });

    afterEach(() => {
        Reflect.deleteProperty(globalThis, 'fetch');
    });

    const respondWith = (body: unknown) => {
        fetchSpy.mockImplementation(() => Promise.resolve(createMockResponse(body)));
    };

    const requested = () => fetchSpy.mock.calls.map(([url]) => new URL(String(url), 'https://test.com'));
    const lastRequest = () => requested().filter(u => u.pathname.endsWith('/transactions')).at(-1)!;

    const renderTable = (props: React.ComponentProps<typeof TransactionsTable> = {}) => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/members/456/account/transactions']}>
                    <HalFormProvider>
                        <HalRouteProvider>
                            <TransactionsTable {...props}/>
                        </HalRouteProvider>
                    </HalFormProvider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    it('requests the newest transactions first by default', async () => {
        respondWith(txPage([tx('t1')]));
        renderTable();

        await screen.findByText('note-t1');

        const url = lastRequest();
        expect(url.searchParams.get('sort')).toBe('occurredAt,desc');
        expect(url.searchParams.get('page')).toBe('0');
    });

    it('sends the active filters as query parameters', async () => {
        respondWith(txPage([tx('t1')]));
        renderTable({extraParams: {type: 'DEPOSIT', occurredAtFrom: '2025-01-01', occurredAtTo: '2025-03-31'}});

        await screen.findByText('note-t1');

        const url = lastRequest();
        expect(url.searchParams.get('type')).toBe('DEPOSIT');
        expect(url.searchParams.get('occurredAtFrom')).toBe('2025-01-01');
        expect(url.searchParams.get('occurredAtTo')).toBe('2025-03-31');
    });

    it('re-sorts by amount when the amount header is clicked', async () => {
        respondWith(txPage([tx('t1')]));
        renderTable();
        await screen.findByText('note-t1');

        await userEvent.click(screen.getByRole('button', {name: new RegExp(`Sort by ${labels.finance.amount}`)}));

        await waitFor(() => expect(lastRequest().searchParams.get('sort')).toMatch(/^amount,(asc|desc)$/));
        expect(lastRequest().searchParams.get('page')).toBe('0');
    });

    it('fetches the next page and goes back to the first page on re-sort', async () => {
        respondWith(txPage([tx('t1')], {totalElements: 25, totalPages: 3}));
        renderTable();
        await screen.findByText('note-t1');

        await userEvent.click(screen.getByRole('button', {name: /Další/}));
        await waitFor(() => expect(lastRequest().searchParams.get('page')).toBe('1'));

        await userEvent.click(screen.getByRole('button', {name: new RegExp(`Sort by ${labels.finance.amount}`)}));
        await waitFor(() => expect(lastRequest().searchParams.get('page')).toBe('0'));
    });

    it('requests the chosen page size and returns to the first page', async () => {
        respondWith(txPage([tx('t1')], {totalElements: 60, totalPages: 6}));
        renderTable();
        await screen.findByText('note-t1');

        await userEvent.click(screen.getByRole('button', {name: /Další/}));
        await waitFor(() => expect(lastRequest().searchParams.get('page')).toBe('1'));

        fireEvent.change(screen.getByRole('combobox'), {target: {value: '25'}});

        await waitFor(() => expect(lastRequest().searchParams.get('size')).toBe('25'));
        expect(lastRequest().searchParams.get('page')).toBe('0');
    });

    it('shows the empty message when there are no transactions', async () => {
        respondWith(txPage([]));
        renderTable();

        expect(await screen.findByText(labels.finance.noTransactions)).toBeInTheDocument();
    });

    it('marks a reversed transaction and its reversal', async () => {
        respondWith(txPage([
            tx('t1', {note: 'original'}),
            tx('t2', {note: 'storno', reversesTransactionId: 't1', amount: -500}),
        ]));
        renderTable();

        expect(await screen.findByText(labels.finance.reversal)).toBeInTheDocument();
        expect(screen.getByText(`[${labels.finance.reversed}]`)).toBeInTheDocument();
    });
});

describe('TransactionFilterBar', () => {
    const value = {type: '', dateFrom: '', dateTo: ''};

    it('emits the chosen type and keeps other filters', async () => {
        const onChange = vi.fn();
        render(<TransactionFilterBar value={{...value, dateFrom: '2025-01-01'}} onChange={onChange}/>);

        await userEvent.click(screen.getByRole('button', {name: labels.finance.typeDeposit}));

        expect(onChange).toHaveBeenCalledWith({type: 'DEPOSIT', dateFrom: '2025-01-01', dateTo: ''});
    });

    it('emits date range changes', () => {
        const onChange = vi.fn();
        const {container} = render(<TransactionFilterBar value={value} onChange={onChange}/>);
        const [from, to] = Array.from(container.querySelectorAll('input[type="date"]'));

        fireEvent.change(from, {target: {value: '2025-02-01'}});
        fireEvent.change(to, {target: {value: '2025-02-28'}});

        expect(onChange).toHaveBeenNthCalledWith(1, {...value, dateFrom: '2025-02-01'});
        expect(onChange).toHaveBeenNthCalledWith(2, {...value, dateTo: '2025-02-28'});
    });

    it('marks the selected type', () => {
        render(<TransactionFilterBar value={{...value, type: 'OTHER'}} onChange={vi.fn()}/>);

        expect(screen.getByRole('button', {name: labels.finance.typeOther})).toHaveAttribute('aria-pressed', 'true');
        expect(screen.getByRole('button', {name: labels.finance.filterTypeAll})).toHaveAttribute('aria-pressed', 'false');
    });
});

describe('useTransactionFilters', () => {
    const wrapperAt = (url: string) => ({children}: {children: React.ReactNode}) => (
        <MemoryRouter initialEntries={[url]}>{children}</MemoryRouter>
    );

    it('reads filters from the URL and exposes only non-empty ones as request params', () => {
        const {result} = renderHook(() => useTransactionFilters(), {wrapper: wrapperAt('/a?type=OTHER&occurredAtFrom=2025-01-01')});

        expect(result.current.filters).toEqual({type: 'OTHER', dateFrom: '2025-01-01', dateTo: ''});
        expect(result.current.extraParams).toEqual({type: 'OTHER', occurredAtFrom: '2025-01-01'});
    });

    it('is empty without URL params', () => {
        const {result} = renderHook(() => useTransactionFilters(), {wrapper: wrapperAt('/a')});

        expect(result.current.extraParams).toEqual({});
    });

    it('writes changes to the URL and removes cleared filters while keeping unrelated params', () => {
        const Wrapper = ({children}: {children: React.ReactNode}) => (
            <MemoryRouter initialEntries={['/a?type=OTHER&foo=bar']}>{children}</MemoryRouter>
        );
        const {result} = renderHook(() => ({filters: useTransactionFilters(), location: useLocation()}), {wrapper: Wrapper});

        act(() => result.current.filters.handleFilterChange({type: '', dateFrom: '2025-02-01', dateTo: ''}));

        const params = new URLSearchParams(result.current.location.search);
        expect(params.get('type')).toBeNull();
        expect(params.get('occurredAtFrom')).toBe('2025-02-01');
        expect(params.get('foo')).toBe('bar');
    });
});
