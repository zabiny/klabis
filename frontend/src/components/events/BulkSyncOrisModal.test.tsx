import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {BulkSyncOrisModal} from './BulkSyncOrisModal';
import {labels} from '../../localization';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(),
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
}));

const renderModal = (props: Partial<React.ComponentProps<typeof BulkSyncOrisModal>> = {}) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    const defaults = {
        isOpen: true,
        onClose: vi.fn(),
        syncUrl: '/api/events/sync-from-oris/all-upcoming',
        onSyncComplete: vi.fn(),
    };
    return render(
        <QueryClientProvider client={queryClient}>
            <BulkSyncOrisModal {...defaults} {...props} />
        </QueryClientProvider>,
    );
};

describe('BulkSyncOrisModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('progress state', () => {
        it('shows spinner and progress message while sync is pending', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn(),
                reset: vi.fn(),
                isPending: true,
                data: undefined,
                error: null,
                isSuccess: false,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal();

            expect(screen.getByText(labels.bulkSync.progress)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: labels.buttons.close})).not.toBeInTheDocument();
        });
    });

    describe('summary state', () => {
        it('shows success and failure counts when sync completes', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn(),
                reset: vi.fn(),
                isPending: false,
                data: {
                    data: {
                        totalProcessed: 3,
                        successCount: 2,
                        failureCount: 1,
                        results: [
                            {eventId: '1', name: 'Jarní závod', status: 'synced'},
                            {eventId: '2', name: 'Letní pohár', status: 'failed', error: 'ORIS returned 404'},
                            {eventId: '3', name: 'Podzimní tour', status: 'synced'},
                        ],
                    },
                    location: null,
                },
                error: null,
                isSuccess: true,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal();

            expect(screen.getByText(labels.bulkSync.successCount(2), {exact: false})).toBeInTheDocument();
            expect(screen.getByText(labels.bulkSync.failureCount(1), {exact: false})).toBeInTheDocument();
        });

        it('shows failure list with event name and error message', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn(),
                reset: vi.fn(),
                isPending: false,
                data: {
                    data: {
                        totalProcessed: 2,
                        successCount: 1,
                        failureCount: 1,
                        results: [
                            {eventId: '2', name: 'Letní pohár', status: 'failed', error: 'ORIS returned 404'},
                        ],
                    },
                    location: null,
                },
                error: null,
                isSuccess: true,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal();

            expect(screen.getByText('Letní pohár')).toBeInTheDocument();
            expect(screen.getByText('ORIS returned 404')).toBeInTheDocument();
        });

        it('shows close button after sync completes', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn(),
                reset: vi.fn(),
                isPending: false,
                data: {
                    data: {totalProcessed: 0, successCount: 0, failureCount: 0, results: []},
                    location: null,
                },
                error: null,
                isSuccess: true,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal();

            expect(screen.getByRole('button', {name: labels.buttons.close})).toBeInTheDocument();
        });

        it('calls onClose when close button is clicked', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn(),
                reset: vi.fn(),
                isPending: false,
                data: {
                    data: {totalProcessed: 0, successCount: 0, failureCount: 0, results: []},
                    location: null,
                },
                error: null,
                isSuccess: true,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal({onClose});

            await user.click(screen.getByRole('button', {name: labels.buttons.close}));

            expect(onClose).toHaveBeenCalled();
        });
    });

    describe('initial state', () => {
        it('triggers mutation on mount when modal opens', () => {
            const mutate = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate,
                reset: vi.fn(),
                isPending: false,
                data: undefined,
                error: null,
                isSuccess: false,
                isError: false,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderModal({syncUrl: '/api/events/sync-from-oris/all-upcoming'});

            expect(mutate).toHaveBeenCalledWith(
                {url: '/api/events/sync-from-oris/all-upcoming'},
                expect.anything(),
            );
        });
    });
});
