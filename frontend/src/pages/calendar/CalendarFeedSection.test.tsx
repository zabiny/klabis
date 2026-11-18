import '@testing-library/jest-dom';
import {act, render, screen, waitFor} from '@testing-library/react';
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

            expect(screen.getByText(/Načítání/i)).toBeInTheDocument();
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

        it('shows inline confirmation instead of a nested modal when "Vygenerovat nový token" is clicked', async () => {
            const user = userEvent.setup();
            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
            expect(screen.getByText(/předchozí URL přestane fungovat/i)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /Zrušit/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /^Vygenerovat$/i})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /Vygenerovat nový token/i})).not.toBeInTheDocument();
        });

        it('cancels the inline confirmation without calling mutation', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));
            await user.click(screen.getByRole('button', {name: /Zrušit/i}));

            expect(mutateMock).not.toHaveBeenCalled();
            expect(screen.queryByText(/předchozí URL přestane fungovat/i)).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: /Vygenerovat nový token/i})).toBeInTheDocument();
        });

        it('calls POST when the inline confirm button is clicked', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});

            renderSection();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));
            await user.click(screen.getByRole('button', {name: /^Vygenerovat$/i}));

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

    describe('revealed full URL', () => {
        const fullUrl = 'https://api.klabis.otakar.io/ical/my-schedule.ics?token=abc123def456ghi789jkl012mno345pqr678stu';

        const renderAndReveal = async () => {
            const user = userEvent.setup();
            let onSuccessCallback: ((result: unknown) => void) | undefined;
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {url: null, lastSetAt: null},
                isLoading: false,
                error: null,
                status: 'success',
            } as ReturnType<typeof useAuthorizedQuery>);
            mockMutation({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onSuccessCallback = opts?.onSuccess;
                }),
            });
            const view = renderSection();
            await user.click(screen.getByRole('button', {name: /Vytvořit kalendářový feed/i}));
            act(() => onSuccessCallback?.({data: {url: fullUrl, lastSetAt: '2026-05-21T10:00:00Z'}, location: null}));
            return {user, view};
        };

        it('copies the full URL to the clipboard', async () => {
            const {user} = await renderAndReveal();
            const writeText = vi.fn().mockResolvedValue(undefined);
            Object.defineProperty(navigator, 'clipboard', {value: {writeText}, configurable: true});

            await user.click(screen.getByRole('button', {name: /Kopírovat/i}));

            expect(writeText).toHaveBeenCalledWith(fullUrl);
            expect(await screen.findByRole('button', {name: /Zkopírováno/i})).toBeInTheDocument();
        });

        it('offers inline regeneration confirmation while the full URL is shown', async () => {
            const {user} = await renderAndReveal();

            await user.click(screen.getByRole('button', {name: /Vygenerovat nový token/i}));

            expect(screen.getByText(/předchozí URL přestane fungovat/i)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /Zrušit/i})).toBeInTheDocument();
        });

        it('does not show the full URL again after the section is unmounted and mounted again', async () => {
            const {view} = await renderAndReveal();
            expect(screen.getByText(fullUrl)).toBeInTheDocument();

            view.unmount();
            renderSection();

            expect(screen.queryByText(fullUrl)).not.toBeInTheDocument();
        });
    });
});
