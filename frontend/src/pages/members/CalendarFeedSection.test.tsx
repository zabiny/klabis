import '@testing-library/jest-dom';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {CalendarFeedSection} from './CalendarFeedSection';
import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

const renderSection = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <CalendarFeedSection icalTokenHref="/api/me/ical-token"/>
        </QueryClientProvider>
    );
};

const mockMutation = (overrides?: Partial<ReturnType<typeof useAuthorizedMutation>>) => {
    vi.mocked(useAuthorizedMutation).mockReturnValue({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        isSuccess: false,
        isIdle: true,
        isError: false,
        data: undefined,
        error: null,
        reset: vi.fn(),
        status: 'idle',
        variables: undefined,
        context: undefined,
        failureCount: 0,
        failureReason: null,
        submittedAt: 0,
        ...overrides,
    } as unknown as ReturnType<typeof useAuthorizedMutation>);
};

describe('CalendarFeedSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('loading state', () => {
        it('shows loading state while fetching token status', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: true,
                error: null,
                status: 'pending',
            } as ReturnType<typeof useAuthorizedQuery>);
            mockMutation();

            renderSection();

            expect(screen.getByText(/Kalendářový feed/i)).toBeInTheDocument();
        });
    });

    describe('no token exists (url is null)', () => {
        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {url: null, lastSetAt: null},
                isLoading: false,
                error: null,
                status: 'success',
            } as ReturnType<typeof useAuthorizedQuery>);
            mockMutation();
        });

        it('renders the section heading', () => {
            renderSection();
            expect(screen.getByRole('heading', {name: /Kalendářový feed/i})).toBeInTheDocument();
        });

        it('shows "Vytvořit kalendářový feed" button when no token exists', () => {
            renderSection();
            expect(screen.getByRole('button', {name: /Vytvořit kalendářový feed/i})).toBeInTheDocument();
        });

        it('does not show masked URL when no token exists', () => {
            renderSection();
            expect(screen.queryByText(/my-schedule\.ics/i)).not.toBeInTheDocument();
        });

        it('shows help text with calendar client instructions', () => {
            renderSection();
            expect(screen.getByText(/Google Calendar: Jiné kalendáře/i)).toBeInTheDocument();
            expect(screen.getByText(/Apple Calendar: Soubor/i)).toBeInTheDocument();
            expect(screen.getByText(/Outlook: Přidat kalendář/i)).toBeInTheDocument();
        });

        it('shows info about Můj rozvrh filter connection', () => {
            renderSection();
            expect(screen.getByText(/Můj rozvrh/i)).toBeInTheDocument();
        });

        it('calls POST when "Vytvořit" button is clicked', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vytvořit kalendářový feed/i}));

            expect(mutateMock).toHaveBeenCalledWith(
                {url: '/api/me/ical-token'},
                expect.any(Object),
            );
        });

        it('shows loading state on button while POST is pending', () => {
            mockMutation({isPending: true});

            renderSection();

            const button = screen.getByRole('button', {name: /Vytvořit kalendářový feed/i});
            expect(button).toBeDisabled();
        });
    });

    describe('token exists (masked URL)', () => {
        const maskedUrl = 'https://api.klabis.otakar.io/ical/my-schedule.ics?token=••••••••••••••••••••••••••••••••••••••••';
        const lastSetAt = '2026-05-10T10:00:00Z';

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {url: maskedUrl, lastSetAt},
                isLoading: false,
                error: null,
                status: 'success',
            } as ReturnType<typeof useAuthorizedQuery>);
            mockMutation();
        });

        it('shows the masked URL', () => {
            renderSection();
            expect(screen.getByText(/my-schedule\.ics/i)).toBeInTheDocument();
        });

        it('shows "Vygenerovat nový token" button', () => {
            renderSection();
            expect(screen.getByRole('button', {name: /Vygenerovat nový token/i})).toBeInTheDocument();
        });

        it('does not show "Vytvořit kalendářový feed" button when token exists', () => {
            renderSection();
            expect(screen.queryByRole('button', {name: /Vytvořit kalendářový feed/i})).not.toBeInTheDocument();
        });

        it('shows lastSetAt date', () => {
            renderSection();
            expect(screen.getByText(/Vygenerováno/i)).toBeInTheDocument();
        });

        it('opens confirm dialog when "Vygenerovat nový token" is clicked', async () => {
            const user = userEvent.setup();
            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText(/předchozí URL přestane fungovat/i)).toBeInTheDocument();
        });

        it('cancels confirm dialog without calling mutation', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));
            await user.click(screen.getByRole('button', {name: /Zrušit/i}));

            expect(mutateMock).not.toHaveBeenCalled();
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('calls POST when confirm button is clicked in dialog', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));
            const dialog = screen.getByRole('dialog');
            await user.click(within(dialog).getByRole('button', {name: /Vygenerovat/i}));

            expect(mutateMock).toHaveBeenCalledWith(
                {url: '/api/me/ical-token'},
                expect.any(Object),
            );
        });
    });

    describe('after successful token generation (full URL revealed)', () => {
        const fullUrl = 'https://api.klabis.otakar.io/ical/my-schedule.ics?token=abc123def456ghi789jkl012mno345pqr678stu';

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {url: null, lastSetAt: null},
                isLoading: false,
                error: null,
                status: 'success',
            } as ReturnType<typeof useAuthorizedQuery>);
        });

        it('shows full URL after successful POST', async () => {
            const user = userEvent.setup();
            let onSuccessCallback: ((result: unknown) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onSuccessCallback = opts?.onSuccess;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vytvořit kalendářový feed/i}));

            onSuccessCallback?.({data: {url: fullUrl, lastSetAt: '2026-05-21T10:00:00Z'}, location: null});

            await waitFor(() => {
                expect(screen.getByText(fullUrl)).toBeInTheDocument();
            });
        });

        it('shows "Kopírovat" button after POST success', async () => {
            const user = userEvent.setup();
            let onSuccessCallback: ((result: unknown) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onSuccessCallback = opts?.onSuccess;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vytvořit kalendářový feed/i}));
            onSuccessCallback?.({data: {url: fullUrl, lastSetAt: '2026-05-21T10:00:00Z'}, location: null});

            await waitFor(() => {
                expect(screen.getByRole('button', {name: /Kopírovat/i})).toBeInTheDocument();
            });
        });
    });
});
