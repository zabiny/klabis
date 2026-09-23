import '@testing-library/jest-dom';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {SyncStatusIndicator} from './SyncStatusIndicator';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {GetSyncStateResource, Link} from '../../api';
import {vi} from 'vitest';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

const buildSyncLink = (overrides: Partial<Link> = {}): Link => ({
    href: '/api/events/1/sync',
    ...overrides,
});

const buildSyncState = (overrides: Partial<GetSyncStateResource> = {}): GetSyncStateResource => ({
    entityType: 'events',
    status: 'IN_SYNC',
    externalSystem: 'ORIS',
    _links: {self: {href: '/api/events/1/sync'}},
    ...overrides,
});

type QueryState = {
    isLoading: boolean;
    error: Error | null;
    data: GetSyncStateResource | undefined;
};

const createQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {retry: false, gcTime: 0, staleTime: Infinity},
        },
    });

const renderIndicator = ({
                             syncLink,
                             mode = 'icon',
                             queryState,
                         }: {
    syncLink: Link | null | undefined;
    mode?: 'icon' | 'icon+date';
    queryState: QueryState;
}) => {
    vi.mocked(useAuthorizedQuery).mockReturnValue({
        data: queryState.data,
        isLoading: queryState.isLoading,
        error: queryState.error,
        refetch: vi.fn().mockResolvedValue(undefined),
        status: queryState.error ? 'error' : queryState.data ? 'success' : 'pending',
        fetchStatus: queryState.error ? 'idle' : queryState.data ? 'idle' : 'fetching',
        dataUpdatedAt: 0,
        errorUpdatedAt: 0,
        failureCount: 0,
        failureReason: null,
        isError: !!queryState.error,
        isFetched: !!queryState.data,
        isFetchedAfterMount: !!queryState.data,
        isFetching: queryState.isLoading,
        isLoadingError: false,
        isPaused: false,
        isPending: queryState.isLoading,
        isPlaceholderData: false,
        isRefetchError: false,
        isRefetching: false,
        isStale: false,
        isSuccess: !!queryState.data,
        promise: Promise.resolve(queryState.data),
        remove: vi.fn(),
        trpc: undefined as never,
    } as unknown as ReturnType<typeof useAuthorizedQuery>);

    const queryClient = createQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/events/1']}>
                <SyncStatusIndicator syncLink={syncLink ?? undefined} mode={mode}/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const STATUS_TABLE: Array<{
    status: GetSyncStateResource['status'];
    variant: 'info' | 'success' | 'warning' | 'error' | 'default';
    label: string;
}> = [
    {status: 'NEW', variant: 'info', label: 'CircleDot'},
    {status: 'IN_SYNC', variant: 'success', label: 'Check'},
    {status: 'RETRYING', variant: 'warning', label: 'RotateCw'},
    {status: 'CONFLICT', variant: 'error', label: 'AlertTriangle'},
    {status: 'FAILED', variant: 'error', label: 'XCircle'},
    {status: 'RETIRED', variant: 'default', label: 'Archive'},
];

const variantClass: Record<'info' | 'success' | 'warning' | 'error' | 'default', string> = {
    info: 'bg-info/20',
    success: 'bg-success/20',
    warning: 'bg-warning/20',
    error: 'bg-error/20',
    default: 'bg-surface-raised',
};

describe('SyncStatusIndicator', () => {
    describe('syncLink absence', () => {
        it('renders nothing when syncLink is null', () => {
            const {container} = renderIndicator({
                syncLink: null,
                queryState: {isLoading: false, error: null, data: undefined},
            });
            expect(container.firstChild).toBeNull();
        });

        it('renders nothing when syncLink is undefined', () => {
            const {container} = renderIndicator({
                syncLink: undefined,
                queryState: {isLoading: false, error: null, data: undefined},
            });
            expect(container.firstChild).toBeNull();
        });
    });

    describe('status → Badge variant and icon mapping (design.md D4)', () => {
        for (const {status, variant, label} of STATUS_TABLE) {
            it(`renders ${variant} Badge with ${label} icon for status ${status}`, async () => {
                renderIndicator({
                    syncLink: buildSyncLink(),
                    mode: 'icon',
                    queryState: {
                        isLoading: false,
                        error: null,
                        data: buildSyncState({status}),
                    },
                });

                const badge = await screen.findByTestId(`sync-status-${status}`);
                expect(badge).toHaveClass(variantClass[variant]);
                expect(badge).toHaveAttribute('aria-label', expect.stringMatching(new RegExp(`^${label}`)));
            });
        }

        it('CONFLICT and FAILED share the error variant but render distinct icons', async () => {
            const conflictRender = renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({status: 'CONFLICT'}),
                },
            });
            const conflictBadge = await conflictRender.findByTestId('sync-status-CONFLICT');
            expect(conflictBadge).toHaveClass(variantClass.error);

            const conflictIcon = conflictBadge.querySelector('svg')?.getAttribute('class') ?? '';

            conflictRender.unmount();

            const failedRender = renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({status: 'FAILED'}),
                },
            });
            const failedBadge = await failedRender.findByTestId('sync-status-FAILED');
            expect(failedBadge).toHaveClass(variantClass.error);

            const failedIcon = failedBadge.querySelector('svg')?.getAttribute('class') ?? '';
            expect(conflictIcon).not.toBe(failedIcon);
        });
    });

    describe('loading state', () => {
        it('renders a loading placeholder while the sync sub-resource is pending', () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {isLoading: true, error: null, data: undefined},
            });

            expect(screen.getByRole('status')).toBeInTheDocument();
            expect(screen.queryByTestId(/^sync-status-/)).not.toBeInTheDocument();
        });
    });

    describe('error state', () => {
        it('renders an error indicator when the sync sub-resource fetch fails', () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: new Error('Sync unavailable'),
                    data: undefined,
                },
            });

            expect(screen.getByRole('alert')).toBeInTheDocument();
            expect(screen.queryByTestId(/^sync-status-/)).not.toBeInTheDocument();
        });
    });

    describe('mode="icon"', () => {
        it('renders only the icon — no date inline', async () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        lastSuccessfulSyncAt: '2026-04-15T10:30:00Z',
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            expect(badge.querySelector('svg')).toBeInTheDocument();
            expect(within(badge).queryByTestId('sync-last-date')).toBeNull();
        });

        it('shows formatted lastSuccessfulSyncAt as tooltip on hover', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        lastSuccessfulSyncAt: '2026-04-15T10:30:00Z',
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            await user.hover(badge);

            const tooltip = await screen.findByRole('tooltip');
            expect(tooltip.textContent).not.toBe('');
            expect(tooltip.textContent).not.toBe('Nikdy synchronizováno');
        });

        it('shows "Nikdy synchronizováno" fallback in tooltip when lastSuccessfulSyncAt is null', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'NEW',
                        lastSuccessfulSyncAt: null,
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-NEW');
            await user.hover(badge);

            const tooltip = await screen.findByRole('tooltip');
            expect(tooltip).toHaveTextContent('Nikdy synchronizováno');
        });
    });

    describe('mode="icon+date"', () => {
        it('renders icon and formatted date inline, always visible', async () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon+date',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        lastSuccessfulSyncAt: '2026-04-15T10:30:00Z',
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            expect(badge.querySelector('svg')).toBeInTheDocument();

            const date = within(badge).getByTestId('sync-last-date');
            expect(date).toBeInTheDocument();
            expect(date.textContent).not.toBe('');
        });

        it('shows the fallback "Nikdy synchronizováno" inline when lastSuccessfulSyncAt is null', async () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon+date',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'NEW',
                        lastSuccessfulSyncAt: null,
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-NEW');
            expect(within(badge).getByTestId('sync-last-date')).toHaveTextContent('Nikdy synchronizováno');
        });
    });
});
