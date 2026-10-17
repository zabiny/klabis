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

const renderPage = (items: unknown[], page = {size: 10, totalElements: items.length, totalPages: 1, number: 0}) => {
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
            <MemoryRouter initialEntries={['/sync/attempts']}>
                <SyncOverviewPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

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
