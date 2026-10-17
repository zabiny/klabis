import '@testing-library/jest-dom';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {HalResponse} from '../../api';
import {SyncOverviewPage} from './SyncOverviewPage';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({mutate: vi.fn(), isPending: false, error: null}),
}));

vi.mock('../../components/sync/SyncStatusIndicator.tsx', () => ({
    SyncStatusIndicator: ({syncLink}: {syncLink?: {href: string}}) =>
        syncLink ? <span data-testid="sync-indicator" data-href={syncLink.href}/> : null,
}));

const attempt = (overrides?: Record<string, unknown>) => ({
    startedAt: '2026-10-01T10:15:30Z',
    trigger: 'SCHEDULED',
    outcome: 'SUCCESS',
    entityType: 'members',
    entityLabel: 'Jan Novák (ZBM9500)',
    externalId: '4711',
    direction: 'INWARD',
    _links: {syncState: {href: 'http://localhost/api/members/1/sync'}},
    ...overrides,
});

const pageData = (resourceData: HalResponse | null) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/sync/attempts',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/sync/attempts'}),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => false),
    getEmbeddedItems: vi.fn(() => []),
    isCollection: vi.fn(() => false),
    hasLink: vi.fn(() => false),
    hasTemplate: vi.fn(() => false),
    hasForms: vi.fn(() => false),
    getPageMetadata: vi.fn(() => undefined),
});

const renderPage = (items: unknown[], page = {size: 10, totalElements: items.length, totalPages: 1, number: 0}, initialEntry = '/sync/attempts') => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/sync/attempts'}},
        _embedded: {syncAttempts: items},
        page,
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
    vi.mocked(useHalPageData).mockReturnValue(pageData(resourceData) as unknown as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <SyncOverviewPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const lastUrl = () => new URL(vi.mocked(useAuthorizedQuery).mock.calls.at(-1)![0] as string, 'http://localhost');

describe('SyncOverviewPage filters', () => {
    beforeEach(() => vi.clearAllMocks());

    const pagedPage = {size: 10, totalElements: 30, totalPages: 3, number: 0};

    it('sends no filter parameters by default and hides unchanged attempts', () => {
        renderPage([attempt()], pagedPage);

        const params = lastUrl().searchParams;
        ['entityType', 'outcome', 'from', 'to', 'includeUnchanged'].forEach(p => expect(params.has(p)).toBe(false));
    });

    it('sends entityType and resets to the first page', () => {
        renderPage([attempt()], pagedPage);
        fireEvent.click(screen.getByText('Další →'));
        expect(lastUrl().searchParams.get('page')).toBe('1');

        fireEvent.change(screen.getByLabelText('Druh entity'), {target: {value: 'members'}});

        expect(lastUrl().searchParams.get('entityType')).toBe('members');
        expect(lastUrl().searchParams.get('page')).toBe('0');
    });

    it('sends outcome and resets to the first page', () => {
        renderPage([attempt()], pagedPage);
        fireEvent.click(screen.getByText('Další →'));

        fireEvent.change(screen.getByLabelText('Výsledek'), {target: {value: 'CONFLICT'}});

        expect(lastUrl().searchParams.get('outcome')).toBe('CONFLICT');
        expect(lastUrl().searchParams.get('page')).toBe('0');
    });

    it('sends the local date range as instants with an inclusive end date', () => {
        renderPage([attempt()], pagedPage);
        fireEvent.click(screen.getByText('Další →'));

        fireEvent.change(screen.getByLabelText('Datum od'), {target: {value: '2026-01-10'}});
        fireEvent.change(screen.getByLabelText('Datum do'), {target: {value: '2026-01-31'}});

        const params = lastUrl().searchParams;
        expect(params.get('from')).toBe(new Date(2026, 0, 10).toISOString());
        expect(params.get('to')).toBe(new Date(2026, 1, 1).toISOString());
        expect(params.get('page')).toBe('0');
    });

    it('sends includeUnchanged when the toggle is checked and resets to the first page', () => {
        renderPage([attempt()], pagedPage);
        fireEvent.click(screen.getByText('Další →'));

        fireEvent.click(screen.getByLabelText('Zobrazit i pokusy bez změny'));

        expect(lastUrl().searchParams.get('includeUnchanged')).toBe('true');
        expect(lastUrl().searchParams.get('page')).toBe('0');
    });

    it('ignores invalid dates from a hand-edited URL', () => {
        renderPage([attempt()], pagedPage, '/sync/attempts?dateFrom=abc&dateTo=2026-13-45x');

        const params = lastUrl().searchParams;
        expect(params.has('from')).toBe(false);
        expect(params.has('to')).toBe(false);
    });

    it('clears a filter parameter when the selection is reset', () => {
        renderPage([attempt()], pagedPage);
        fireEvent.change(screen.getByLabelText('Výsledek'), {target: {value: 'FAILED'}});

        fireEvent.change(screen.getByLabelText('Výsledek'), {target: {value: ''}});

        expect(lastUrl().searchParams.has('outcome')).toBe(false);
    });
});

describe('SyncOverviewPage', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders page title and column headers', () => {
        renderPage([attempt()]);

        expect(screen.getByRole('heading', {level: 1, name: 'Synchronizace'})).toBeInTheDocument();
        ['Kdy', 'Entita', 'Druh entity', 'Spouštěč', 'Směr', 'Výsledek', 'Důvod selhání', 'Provedl']
            .forEach(header => expect(screen.getByRole('columnheader', {name: header})).toBeInTheDocument());
    });

    it('renders attempt values with translated enums', () => {
        renderPage([attempt({outcome: 'CONFLICT', trigger: 'MANUAL', direction: 'OUTWARD', failureReason: 'Boom', performedBy: 'ZBM9000', entityType: 'event-registrations'})]);

        const row = screen.getByText('Jan Novák (ZBM9500)').closest('tr')!;
        expect(within(row).getByText('Konflikt')).toBeInTheDocument();
        expect(within(row).getByText('Ruční')).toBeInTheDocument();
        expect(within(row).getByText('Ven z Klabisu')).toBeInTheDocument();
        expect(within(row).getByText('Přihláška na akci')).toBeInTheDocument();
        expect(within(row).getByText('Boom')).toBeInTheDocument();
        expect(within(row).getByText('ZBM9000')).toBeInTheDocument();
    });

    it('passes the row syncState link to the sync status indicator', () => {
        renderPage([attempt(), attempt({entityLabel: 'Bez odkazu', _links: {}})]);

        const indicators = screen.getAllByTestId('sync-indicator');
        expect(indicators).toHaveLength(1);
        expect(indicators[0]).toHaveAttribute('data-href', 'http://localhost/api/members/1/sync');
    });

    it('renders entity label as a link to the frontend route when entity link is present', () => {
        renderPage([attempt({_links: {entity: {href: 'http://localhost/api/members/abc'}}})]);

        const link = screen.getByRole('link', {name: 'Jan Novák (ZBM9500)'});
        expect(link).toHaveAttribute('href', '/members/abc');
    });

    it('renders entity label as plain text when entity link is absent', () => {
        renderPage([attempt({entityLabel: '4711', _links: {}})]);

        expect(screen.getByText('4711')).toBeInTheDocument();
        expect(screen.queryByRole('link', {name: '4711'})).not.toBeInTheDocument();
    });

    it('requests the next page when paging forward', () => {
        renderPage([attempt()], {size: 10, totalElements: 30, totalPages: 3, number: 0});

        fireEvent.click(screen.getByText('Další →'));

        const lastUrl = vi.mocked(useAuthorizedQuery).mock.calls.at(-1)![0] as string;
        expect(lastUrl).toContain('page=1');
    });

    it('shows empty message when there are no attempts', () => {
        renderPage([]);

        expect(screen.getByText('Zatím nebyl zaznamenán žádný pokus o synchronizaci.')).toBeInTheDocument();
    });
});

describe('SyncOverviewPage attention tab', () => {
    beforeEach(() => vi.clearAllMocks());

    const record = (overrides?: Record<string, unknown>) => ({
        entityType: 'members',
        status: 'CONFLICT',
        entityLabel: 'Jan Novák (ZBM9500)',
        externalId: '4711',
        lastSuccessfulSyncAt: '2026-10-01T10:15:30Z',
        failedAttemptsSinceLastSuccess: 3,
        _links: {syncState: {href: 'http://localhost/api/members/1/sync'}},
        ...overrides,
    });

    const renderWithAttention = (records: unknown[], initialEntry = '/sync/attempts') => {
        const attempts: HalResponse = {
            _links: {
                self: {href: 'http://localhost/api/sync/attempts'},
                recordsNeedingAttention: {href: 'http://localhost/api/sync/records?status=CONFLICT&status=FAILED'},
            },
            _embedded: {syncAttempts: [attempt()]},
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        const recordsData: HalResponse = {
            _links: {self: {href: 'http://localhost/api/sync/records?status=CONFLICT&status=FAILED'}},
            _embedded: {syncRecords: records},
        };
        vi.mocked(useAuthorizedQuery).mockImplementation(((url: string) => ({
            data: url.includes('/sync/records') ? recordsData : attempts,
            error: null,
        })) as unknown as typeof useAuthorizedQuery);
        vi.mocked(useHalPageData).mockReturnValue(pageData(attempts) as unknown as ReturnType<typeof useHalPageData>);
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialEntry]}>
                    <SyncOverviewPage/>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    const openAttentionTab = () => fireEvent.click(screen.getByRole('tab', {name: 'Vyžaduje pozornost'}));

    it('shows attempts tab first and switches to attention tab', () => {
        renderWithAttention([record()]);

        expect(screen.getByRole('tab', {name: 'Všechny pokusy'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.queryByText('Selhaných pokusů')).not.toBeInTheDocument();

        openAttentionTab();

        expect(screen.getByRole('tab', {name: 'Vyžaduje pozornost'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('columnheader', {name: 'Selhaných pokusů'})).toBeInTheDocument();
    });

    it('follows the recordsNeedingAttention link', () => {
        renderWithAttention([record()]);
        openAttentionTab();

        const params = lastUrl().searchParams;
        expect(lastUrl().pathname).toBe('/api/sync/records');
        expect(params.getAll('status')).toEqual(['CONFLICT', 'FAILED']);
    });

    it('renders record rows with status, kind, last sync, failed count, indicator and entity link', () => {
        renderWithAttention([record({status: 'FAILED', _links: {
            syncState: {href: 'http://localhost/api/members/1/sync'},
            entity: {href: 'http://localhost/api/members/abc'},
        }})]);
        openAttentionTab();

        const row = screen.getByRole('link', {name: 'Jan Novák (ZBM9500)'}).closest('tr')!;
        expect(screen.getByRole('link', {name: 'Jan Novák (ZBM9500)'})).toHaveAttribute('href', '/members/abc');
        expect(within(row).getByText('Selhalo')).toBeInTheDocument();
        expect(within(row).getByText('Člen')).toBeInTheDocument();
        expect(within(row).getByText('3')).toBeInTheDocument();
        expect(within(row).getByTestId('sync-indicator')).toHaveAttribute('data-href', 'http://localhost/api/members/1/sync');
    });

    it('shows never-synchronised text when there was no successful sync', () => {
        renderWithAttention([record({lastSuccessfulSyncAt: undefined})]);
        openAttentionTab();

        expect(screen.getByText('Nikdy synchronizováno')).toBeInTheDocument();
    });

    it('shows empty message when nothing needs attention', () => {
        renderWithAttention([]);
        openAttentionTab();

        expect(screen.getByText('Nic nevyžaduje pozornost.')).toBeInTheDocument();
    });

    it('does not offer the attention tab when the link is missing', () => {
        renderPage([attempt()]);

        expect(screen.queryByRole('tab', {name: 'Vyžaduje pozornost'})).not.toBeInTheDocument();
    });

    it('links tab and panel for accessibility', () => {
        renderWithAttention([record()]);
        openAttentionTab();

        const tab = screen.getByRole('tab', {name: 'Vyžaduje pozornost'});
        const panel = screen.getByRole('tabpanel');
        expect(tab).toHaveAttribute('aria-controls', panel.id);
        expect(panel).toHaveAttribute('aria-labelledby', tab.id);
    });

    it('restores the attention tab from the URL and keeps attempt filters', () => {
        renderWithAttention([record()], '/sync/attempts?tab=attention&outcome=FAILED');

        expect(screen.getByRole('tab', {name: 'Vyžaduje pozornost'})).toHaveAttribute('aria-selected', 'true');
        fireEvent.click(screen.getByRole('tab', {name: 'Všechny pokusy'}));
        expect(screen.getByLabelText('Výsledek')).toHaveValue('FAILED');
    });
});
