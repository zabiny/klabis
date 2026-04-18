import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {ImportOrisEventModal} from './ImportOrisEventModal';
import {authorizedFetch} from '../../api/authorizedFetch';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../../utils/navigationPath', () => ({
    extractNavigationPath: (url: string) => url.startsWith('/api') ? url.substring(4) : url,
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
}));

const mockAuthorizedFetch = vi.mocked(authorizedFetch);

const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    importHref: '/api/events/import',
};

const orisEvents = [
    {id: 101, name: 'Jarní sprint', date: '2025-04-10', location: 'Brno', organizer: 'ZBM'},
    {id: 202, name: 'Letní závod', date: '2025-07-20', location: null, organizer: 'HBT'},
];

const renderModal = (props = defaultProps) =>
    render(
        <MemoryRouter>
            <ImportOrisEventModal {...props} />
        </MemoryRouter>,
    );

describe('ImportOrisEventModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('visibility', () => {
        it('does not render when isOpen is false', () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            const {container} = render(
                <MemoryRouter>
                    <ImportOrisEventModal {...defaultProps} isOpen={false} />
                </MemoryRouter>,
            );
            expect(container.firstChild).toBeNull();
        });

        it('renders modal title when open', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            expect(await screen.findByText('Import akce z ORIS')).toBeInTheDocument();
        });
    });

    describe('loading state', () => {
        it('shows loading indicator while fetching ORIS events', () => {
            mockAuthorizedFetch.mockReturnValue(new Promise(() => {}));

            renderModal();

            expect(screen.getByText(/načítání/i)).toBeInTheDocument();
        });
    });

    describe('event list', () => {
        it('renders select with ORIS events formatted as "date — name"', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            expect(await screen.findByRole('combobox')).toBeInTheDocument();
            expect(screen.getByText('2025-04-10 ZBM Jarní sprint — Brno')).toBeInTheDocument();
            expect(screen.getByText('2025-07-20 HBT Letní závod')).toBeInTheDocument();
        });

        it('has no option pre-selected (placeholder shown)', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            await screen.findByRole('combobox');
            const select = screen.getByRole('combobox') as HTMLSelectElement;
            expect(select.value).toBe('');
        });

        it('submit button is disabled when no option selected', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            await screen.findByRole('combobox');
            const submitButton = screen.getByRole('button', {name: /importovat/i});
            expect(submitButton).toBeDisabled();
        });

        it('submit button becomes enabled after selecting an option', async () => {
            const user = userEvent.setup();
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');

            const submitButton = screen.getByRole('button', {name: /importovat/i});
            expect(submitButton).toBeEnabled();
        });
    });

    describe('empty list handling', () => {
        it('shows empty message when no ORIS events returned', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => [],
            } as Response);

            renderModal();

            expect(await screen.findByText('Žádné akce k importu.')).toBeInTheDocument();
        });

        it('submit button disabled when event list is empty', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => [],
            } as Response);

            renderModal();

            await screen.findByText('Žádné akce k importu.');
            const submitButton = screen.getByRole('button', {name: /importovat/i});
            expect(submitButton).toBeDisabled();
        });
    });

    describe('fetch error handling', () => {
        it('shows error message when ORIS events fetch fails', async () => {
            mockAuthorizedFetch.mockRejectedValue(new Error('Network error'));

            renderModal();

            expect(
                await screen.findByText('Nepodařilo se načíst akce z ORIS.'),
            ).toBeInTheDocument();
        });

        it('submit button disabled when fetch fails', async () => {
            mockAuthorizedFetch.mockRejectedValue(new Error('Network error'));

            renderModal();

            await screen.findByText('Nepodařilo se načíst akce z ORIS.');
            const submitButton = screen.getByRole('button', {name: /importovat/i});
            expect(submitButton).toBeDisabled();
        });
    });

    describe('submit', () => {
        it('calls POST to importHref with selected orisId on submit', async () => {
            const user = userEvent.setup();
            mockAuthorizedFetch
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => orisEvents,
                } as Response)
                .mockResolvedValueOnce({
                    ok: true,
                    status: 201,
                    headers: new Headers({location: '/api/events/new-event-id'}),
                } as Response);

            renderModal();

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            await waitFor(() => {
                expect(mockAuthorizedFetch).toHaveBeenCalledWith(
                    '/api/events/import',
                    expect.objectContaining({
                        method: 'POST',
                        body: JSON.stringify({orisId: 101}),
                    }),
                    false,
                );
            });
        });

        it('shows loading state on submit button during submission', async () => {
            const user = userEvent.setup();
            let resolveImport: (value: Response) => void;
            const importPromise = new Promise<Response>((resolve) => {
                resolveImport = resolve;
            });

            mockAuthorizedFetch
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => orisEvents,
                } as Response)
                .mockReturnValueOnce(importPromise);

            renderModal();

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(screen.getByRole('button', {name: /importuji/i})).toBeDisabled();

            resolveImport!({
                ok: true,
                status: 201,
                headers: new Headers({location: '/api/events/new-event-id'}),
            } as Response);
        });
    });

    describe('success handling', () => {
        it('closes modal and navigates to event detail from Location header on 201', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();

            mockAuthorizedFetch
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => orisEvents,
                } as Response)
                .mockResolvedValueOnce({
                    ok: true,
                    status: 201,
                    headers: new Headers({location: '/api/events/abc-123'}),
                } as Response);

            render(
                <MemoryRouter>
                    <ImportOrisEventModal {...defaultProps} onClose={onClose} />
                </MemoryRouter>,
            );

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            await waitFor(() => {
                expect(onClose).toHaveBeenCalled();
                expect(mockNavigate).toHaveBeenCalledWith('/events/abc-123');
            });
        });
    });

    describe('409 conflict handling', () => {
        it('keeps modal open and shows conflict error on 409', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();

            mockAuthorizedFetch
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => orisEvents,
                } as Response)
                .mockResolvedValueOnce({
                    ok: false,
                    status: 409,
                    headers: new Headers(),
                } as Response);

            render(
                <MemoryRouter>
                    <ImportOrisEventModal {...defaultProps} onClose={onClose} />
                </MemoryRouter>,
            );

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(await screen.findByText('Tato akce již byla importována.')).toBeInTheDocument();
            expect(onClose).not.toHaveBeenCalled();
        });
    });

    describe('generic error handling on submit', () => {
        it('keeps modal open and shows generic error on non-409 failure', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();

            mockAuthorizedFetch
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => orisEvents,
                } as Response)
                .mockResolvedValueOnce({
                    ok: false,
                    status: 500,
                    headers: new Headers(),
                } as Response);

            render(
                <MemoryRouter>
                    <ImportOrisEventModal {...defaultProps} onClose={onClose} />
                </MemoryRouter>,
            );

            await screen.findByRole('combobox');
            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(
                await screen.findByText('Import akce se nezdařil. Zkuste to prosím znovu.'),
            ).toBeInTheDocument();
            expect(onClose).not.toHaveBeenCalled();
        });
    });

    describe('region picker', () => {
        it('renders three radio buttons for JIHOMORAVSKA, MORAVA, CR regions', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'Žebříček Morava'})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'ČR'})).toBeInTheDocument();
        });

        it('JIHOMORAVSKA radio is selected by default', async () => {
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).toBeChecked();
        });

        it('picking ČR clears the prior Jihomoravská selection', async () => {
            const user = userEvent.setup();
            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            renderModal();

            await user.click(screen.getByRole('radio', {name: 'ČR'}));

            expect(screen.getByRole('radio', {name: 'ČR'})).toBeChecked();
            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).not.toBeChecked();
        });
    });

    describe('cancel/close handling', () => {
        it('calls onClose when Cancel button is clicked without making a request', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();

            mockAuthorizedFetch.mockResolvedValue({
                ok: true,
                json: async () => orisEvents,
            } as Response);

            render(
                <MemoryRouter>
                    <ImportOrisEventModal {...defaultProps} onClose={onClose} />
                </MemoryRouter>,
            );

            await screen.findByRole('combobox');
            await user.click(screen.getByRole('button', {name: /zrušit/i}));

            expect(onClose).toHaveBeenCalled();
            expect(mockAuthorizedFetch).toHaveBeenCalledTimes(1);
        });
    });
});
