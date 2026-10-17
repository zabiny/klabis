import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {SyncAttemptHistory} from './SyncAttemptHistory';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

const attempt = (overrides: Record<string, unknown> = {}) => ({
    startedAt: '2026-10-01T10:00:00Z',
    trigger: 'MANUAL',
    direction: 'INWARD',
    outcome: 'FAILED',
    failureReason: 'ORIS nedostupný',
    performedBy: 'admin',
    ...overrides,
});

const mockResponse = (items: unknown[]) => {
    vi.mocked(useAuthorizedQuery).mockReturnValue({
        data: {_embedded: {syncAttempts: items}},
        isLoading: false,
        error: null,
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

const renderHistory = (href = '/api/events/1/sync/attempts') =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <SyncAttemptHistory href={href}/>
        </QueryClientProvider>
    );

describe('SyncAttemptHistory', () => {
    beforeEach(() => vi.mocked(useAuthorizedQuery).mockReset());

    it('requests the 10 most recent attempts without unchanged ones', () => {
        mockResponse([attempt()]);
        renderHistory();
        expect(useAuthorizedQuery).toHaveBeenCalledWith('/api/events/1/sync/attempts?size=10&page=0');
    });

    it('keeps existing query parameters and drops a URI template suffix', () => {
        mockResponse([attempt()]);
        renderHistory('/api/events/1/sync/attempts?foo=bar{&page,size}');
        expect(useAuthorizedQuery).toHaveBeenCalledWith('/api/events/1/sync/attempts?foo=bar&size=10&page=0');
    });

    it('renders attempt details with localized enums', () => {
        mockResponse([attempt()]);
        renderHistory();
        const row = screen.getByTestId('sync-history-row');
        expect(row).toHaveTextContent('Ruční');
        expect(row).toHaveTextContent('Do Klabisu');
        expect(row).toHaveTextContent('Selhalo');
        expect(row).toHaveTextContent('ORIS nedostupný');
        expect(row).toHaveTextContent('admin');
    });

    it('requests unchanged attempts after toggling the checkbox', async () => {
        mockResponse([attempt()]);
        renderHistory();
        await userEvent.click(screen.getByLabelText('Zobrazit i pokusy bez změny'));
        expect(useAuthorizedQuery).toHaveBeenLastCalledWith(
            '/api/events/1/sync/attempts?size=10&page=0&includeUnchanged=true');
    });

    it('shows retention message when there are no attempts', () => {
        mockResponse([]);
        renderHistory();
        expect(screen.getByTestId('sync-history-empty')).toBeInTheDocument();
        expect(screen.queryByTestId('sync-history-row')).not.toBeInTheDocument();
    });
});
