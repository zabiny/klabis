import '@testing-library/jest-dom';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {SyncStatusIndicator} from './SyncStatusIndicator';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';
import {HalFormsPageLayout} from '../HalNavigator2/HalFormsPageLayout.tsx';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import type {GetSyncStateResource, HalFormsTemplate, Link} from '../../api';
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

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

const buildSyncLink = (overrides: Partial<Link> = {}): Link => ({
    href: '/api/events/1/sync',
    ...overrides,
});

const buildTemplate = (title: string): HalFormsTemplate => ({
    method: 'POST',
    target: '/api/events/1/sync',
    title,
    properties: [],
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
                <HalRouteProvider>
                    <HalFormProvider>
                        <HalFormsPageLayout>
                            <SyncStatusIndicator syncLink={syncLink ?? undefined} mode={mode}/>
                        </HalFormsPageLayout>
                    </HalFormProvider>
                </HalRouteProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const openOverlay = async (user: ReturnType<typeof userEvent.setup>, status: GetSyncStateResource['status'] = 'IN_SYNC') => {
    const trigger = await screen.findByTestId(`sync-status-${status}`);
    await user.click(trigger);
    expect(await screen.findByTestId('sync-overlay-modal')).toBeInTheDocument();
    return screen.getByTestId('sync-overlay-modal');
};

describe('SyncStatusOverlay', () => {
    describe('click affordance based on _templates presence', () => {
        it('does not expose a click affordance when _templates is absent', async () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({status: 'IN_SYNC', _templates: undefined}),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            expect(badge).not.toHaveAttribute('role', 'button');
            expect(badge.tagName.toLowerCase()).toBe('span');
            expect(screen.queryByTestId('sync-overlay-modal')).not.toBeInTheDocument();
        });

        it('does not expose a click affordance when _templates is an empty object', async () => {
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({status: 'IN_SYNC', _templates: {}}),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            expect(badge).not.toHaveAttribute('role', 'button');
        });

        it('exposes a click affordance when at least one template is present', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        _templates: {synchronizeNow: buildTemplate('Synchronizovat nyní')},
                    }),
                },
            });

            const badge = await screen.findByTestId('sync-status-IN_SYNC');
            expect(badge).toHaveAttribute('role', 'button');
            expect(badge).toHaveAttribute('tabindex', '0');

            await user.click(badge);
            expect(await screen.findByTestId('sync-overlay-modal')).toBeInTheDocument();
        });
    });

    describe('overlay content', () => {
        it('renders status header with localized status label', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'CONFLICT',
                        _templates: {acknowledgeSyncConflict: buildTemplate('Acknowledge')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'CONFLICT');
            expect(within(overlay).getByText('Konflikt')).toBeInTheDocument();
        });

        it('renders formatted lastSuccessfulSyncAt', async () => {
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
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            const overlay = await openOverlay(user);
            const lastSyncSection = within(overlay).getByTestId('sync-overlay-last-sync');
            expect(lastSyncSection.textContent).not.toBe('');
            expect(lastSyncSection.textContent).not.toBe('-');
        });

        it('renders fallback "Nikdy synchronizováno" when lastSuccessfulSyncAt is null', async () => {
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
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'NEW');
            expect(within(overlay).getByTestId('sync-overlay-last-sync')).toHaveTextContent('Nikdy synchronizováno');
        });
    });

    describe('manager-only details', () => {
        it('renders direction/externalId/nextAttemptDueAt/failedAttemptsSinceLastSuccess for SYNC:MANAGE callers', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'RETRYING',
                        externalId: 'EXT-42',
                        lastDirection: 'INWARD',
                        nextAttemptDueAt: '2026-05-01T10:00:00Z',
                        failedAttemptsSinceLastSuccess: 3,
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'RETRYING');
            expect(within(overlay).getByTestId('sync-overlay-external-id')).toHaveTextContent('EXT-42');
            expect(within(overlay).getByTestId('sync-overlay-direction')).toHaveTextContent('Do Klabisu');
            expect(within(overlay).getByTestId('sync-overlay-failed-attempts')).toHaveTextContent('3');
        });

        it('omits manager-only details when those fields are absent (non-manage caller)', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        _templates: undefined,
                    }),
                },
            });

            await user.click(await screen.findByTestId('sync-status-IN_SYNC'));
            // click affordance absent ⇒ overlay never opens; sanity-assert no overlay rendered
            expect(screen.queryByTestId('sync-overlay-modal')).not.toBeInTheDocument();
        });

        it('falls back to raw enum value for unknown lastDirection', async () => {
            const user = userEvent.setup();
            const unknownDirection = 'NEW_UNKNOWN_DIRECTION' as unknown as 'INWARD';
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'RETRYING',
                        lastDirection: unknownDirection,
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'RETRYING');
            expect(within(overlay).getByTestId('sync-overlay-direction')).toHaveTextContent('NEW_UNKNOWN_DIRECTION');
        });
    });

    describe('CONFLICT diverged-fields table', () => {
        it('renders diverged fields with local/external/baseline values when CONFLICT + manage', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'CONFLICT',
                        divergedFields: ['name', 'eventDate'],
                        changedSides: {name: 'LOCAL', eventDate: 'BOTH'},
                        local: {name: 'Local name', eventDate: '2026-06-01'},
                        external: {name: 'Ext name', eventDate: '2026-06-02'},
                        baseline: {name: 'Base name', eventDate: '2026-06-03'},
                        _templates: {acknowledgeSyncConflict: buildTemplate('Ack')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'CONFLICT');
            const divergence = within(overlay).getByTestId('sync-overlay-divergence');
            expect(divergence).toBeInTheDocument();
            expect(within(divergence).getByText('name')).toBeInTheDocument();
            expect(within(divergence).getByText('eventDate')).toBeInTheDocument();
            expect(within(divergence).getByText('Local name')).toBeInTheDocument();
            expect(within(divergence).getByText('Ext name')).toBeInTheDocument();
            expect(within(divergence).getByText('Base name')).toBeInTheDocument();
        });

        it('does not render divergence table when not in CONFLICT', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        divergedFields: ['name'],
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            const overlay = await openOverlay(user);
            expect(within(overlay).queryByTestId('sync-overlay-divergence')).not.toBeInTheDocument();
        });

        it('falls back to raw enum value for unknown changedSides', async () => {
            const user = userEvent.setup();
            const unknownSides = {name: 'NEW_UNKNOWN_SIDE'} as unknown as {name: 'LOCAL'};
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'CONFLICT',
                        divergedFields: ['name'],
                        changedSides: unknownSides,
                        local: {name: 'Local name'},
                        external: {name: 'Ext name'},
                        baseline: {name: 'Base name'},
                        _templates: {acknowledgeSyncConflict: buildTemplate('Ack')},
                    }),
                },
            });

            const overlay = await openOverlay(user, 'CONFLICT');
            const divergence = within(overlay).getByTestId('sync-overlay-divergence');
            expect(within(divergence).getByText('NEW_UNKNOWN_SIDE')).toBeInTheDocument();
        });
    });

    describe('action templates', () => {
        it('renders only the actions whose template exists (synchronizeNow only)', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        _templates: {synchronizeNow: buildTemplate('Synchronize now')},
                    }),
                },
            });

            const overlay = await openOverlay(user);
            expect(within(overlay).getByTestId('form-template-button-synchronizeNow')).toBeInTheDocument();
            expect(within(overlay).queryByTestId('form-template-button-acknowledgeSyncConflict')).not.toBeInTheDocument();
            expect(within(overlay).queryByTestId('form-template-button-resolveSyncConflict')).not.toBeInTheDocument();
            expect(within(overlay).queryByTestId('form-template-button-resetSyncRecord')).not.toBeInTheDocument();
        });

        it('renders all four action buttons when all templates are present', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'CONFLICT',
                        _templates: {
                            synchronizeNow: buildTemplate('Synchronize now'),
                            acknowledgeSyncConflict: buildTemplate('Acknowledge'),
                            resolveSyncConflict: buildTemplate('Resolve'),
                            resetSyncRecord: buildTemplate('Reset'),
                        },
                    }),
                },
            });

            const overlay = await openOverlay(user, 'CONFLICT');
            expect(within(overlay).getByTestId('form-template-button-synchronizeNow')).toBeInTheDocument();
            expect(within(overlay).getByTestId('form-template-button-acknowledgeSyncConflict')).toBeInTheDocument();
            expect(within(overlay).getByTestId('form-template-button-resolveSyncConflict')).toBeInTheDocument();
            expect(within(overlay).getByTestId('form-template-button-resetSyncRecord')).toBeInTheDocument();
        });
    });

    describe('overlay close', () => {
        it('closes the overlay when close button is clicked', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            await openOverlay(user);
            const closeButton = screen.getByTestId('modal-close-button');
            await user.click(closeButton);
            expect(screen.queryByTestId('sync-overlay-modal')).not.toBeInTheDocument();
        });

        it('closes the overlay when backdrop is clicked', async () => {
            const user = userEvent.setup();
            renderIndicator({
                syncLink: buildSyncLink(),
                mode: 'icon',
                queryState: {
                    isLoading: false,
                    error: null,
                    data: buildSyncState({
                        status: 'IN_SYNC',
                        _templates: {synchronizeNow: buildTemplate('Synchronize')},
                    }),
                },
            });

            await openOverlay(user);
            const backdrop = screen.getByTestId('modal-backdrop');
            await user.click(backdrop);
            expect(screen.queryByTestId('sync-overlay-modal')).not.toBeInTheDocument();
        });
    });
});
