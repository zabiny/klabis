import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {HalRouteProvider} from '../../contexts/HalRouteContext';
import {EventRegistrationDialog, type EventRegistrationDialogProps} from './EventRegistrationDialog.tsx';
import {createDelayedMockResponse, createMockResponse} from '../../__mocks__/mockFetch.ts';
import {authorizedFetch, FetchError} from '../../api/authorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {type Mock, vi} from 'vitest';
import type {HalFormsTemplate} from '../../api/types';
import {labels} from '../../localization';

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
    FetchError: class FetchError extends Error {
        public responseStatus: number;
        public responseBody?: string;
        public responseHeaders: Headers;
        constructor(message: string, responseStatus: number, _statusText?: string, headers?: Headers, responseBody?: string) {
            super(message);
            this.responseStatus = responseStatus;
            this.responseHeaders = headers ?? new Headers();
            this.responseBody = responseBody;
        }
    },
}));

const PREFILL_URL = '/api/events/evt-1/registrations/M001?newRegistration=true';
const EVENT_URL = '/api/events/evt-1';
const EDIT_URL = '/api/events/evt-1/registrations/member-1';

const CATEGORY_OPTIONS = {inline: [{value: 'cat-1', prompt: 'H21'}, {value: 'cat-2', prompt: 'D21'}]};

const eventData = (overrides: Record<string, unknown> = {}) => ({
    name: 'Jarní závod',
    eventDate: '2026-04-15',
    location: 'Brno - Bystrc',
    deadlines: ['2026-01-15', '2026-03-15'],
    sharedTransportEnabled: true,
    sharedAccommodationEnabled: true,
    siUsage: 'REQUIRED',
    siRentalAllowed: false,
    _links: {self: {href: EVENT_URL}},
    ...overrides,
});

const newRegistrationTemplate = (): HalFormsTemplate => mockHalFormsTemplate({
    method: 'POST',
    target: '/api/events/evt-1/registrations',
    title: 'Přihlásit se',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', regex: '\\d{6,7}'},
        {name: 'wantsToRentSi', prompt: 'Chci si půjčit čip', type: 'Boolean'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', required: true, options: CATEGORY_OPTIONS},
        {name: 'wantsSharedTransport', prompt: 'Společná doprava', type: 'Boolean'},
        {name: 'wantsSharedAccommodation', prompt: 'Společné ubytování', type: 'Boolean'},
    ],
});

const editRegistrationTemplate = (): HalFormsTemplate => mockHalFormsTemplate({
    method: 'PUT',
    target: EDIT_URL,
    title: 'Upravit přihlášku',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', regex: '\\d{6,7}'},
        {name: 'wantsToRentSi', prompt: 'Chci si půjčit čip', type: 'Boolean'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', options: CATEGORY_OPTIONS},
        {name: 'wantsSharedTransport', prompt: 'Společná doprava', type: 'Boolean'},
        {name: 'wantsSharedAccommodation', prompt: 'Společné ubytování', type: 'Boolean'},
    ],
});

const prefillData = (overrides: Record<string, unknown> = {}) => ({
    siCardNumber: '1234567',
    firstName: 'Jana',
    lastName: 'Nováková',
    wantsSharedTransport: false,
    wantsSharedAccommodation: false,
    _links: {self: {href: PREFILL_URL}, event: {href: EVENT_URL}},
    _templates: {registerForEvent: newRegistrationTemplate()},
    ...overrides,
});

const editInitialData = (overrides: Record<string, unknown> = {}) => ({
    siCardNumber: '7654321',
    firstName: 'Jana',
    lastName: 'Nováková',
    category: {id: 'cat-2', name: 'D21'},
    wantsSharedTransport: true,
    wantsSharedAccommodation: false,
    _links: {self: {href: EDIT_URL}, event: {href: EVENT_URL}},
    _templates: {editRegistration: editRegistrationTemplate()},
    ...overrides,
});

const jsonOk = (data: unknown) => Promise.resolve(createMockResponse(data));
const writeOk = () => Promise.resolve(createMockResponse({}, 200));

type RouteHandler = (url: string, options?: RequestInit) => Promise<Response> | Response;

const mockRoutes = (handler: RouteHandler) => {
    vi.mocked(authorizedFetch as Mock).mockImplementation(handler as typeof authorizedFetch);
};

describe('EventRegistrationDialog', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {queries: {retry: false, gcTime: 0}},
        });
        vi.mocked(authorizedFetch as Mock).mockReset();
    });

    const renderDialog = (props: Partial<EventRegistrationDialogProps> = {}) => {
        const defaultProps: EventRegistrationDialogProps = {
            registration: {href: PREFILL_URL},
            onClose: vi.fn(),
        };
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    <HalRouteProvider>
                        <EventRegistrationDialog {...defaultProps} {...props}/>
                    </HalRouteProvider>
                </MemoryRouter>
            </QueryClientProvider>,
        );
    };

    it('shows skeleton while the registration data is loading', () => {
        mockRoutes(() => createDelayedMockResponse(prefillData(), 200));
        renderDialog();

        expect(screen.getByTestId('event-registration-dialog-skeleton')).toBeInTheDocument();
    });

    it('renders new-registration form prefilled with SI chip from member profile', async () => {
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData()));
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        expect(siInput).toHaveValue('1234567');
        expect(screen.getByText(labels.events.registrationModal.siChipHelperPrefilled)).toBeInTheDocument();
    });

    it('renders context strip with event name, date and location fetched from the event link', async () => {
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData()));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByTestId('modal-context')).toHaveTextContent('Jarní závod');
        expect(screen.getByTestId('modal-context')).toHaveTextContent('15. 4. 2026');
        expect(screen.getByTestId('modal-context')).toHaveTextContent('Brno - Bystrc');
    });

    it('puts the member name into the header title in new mode', async () => {
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData()));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByTestId('modal-title')).toHaveTextContent('Jana Nováková');
        expect(screen.queryByTestId('registration-member-chip')).not.toBeInTheDocument();
    });

    it('shows deadline chip with the currently relevant deadline', async () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-02-01'));
        try {
            mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData()));
            renderDialog();

            await vi.waitFor(() => {
                expect(screen.getByTestId('registration-deadline-chip')).toHaveTextContent('Přihlášky do 15. 3. 2026');
            });
        } finally {
            vi.useRealTimers();
        }
    });

    it('edit mode shows member name in the header, prefilled SI chip and category', async () => {
        mockRoutes((url) => url === EDIT_URL ? jsonOk(editInitialData()) : jsonOk(eventData()));
        renderDialog({registration: {href: EDIT_URL}});

        const siInput = await screen.findByLabelText(/SI čip/);
        expect(siInput).toHaveValue('7654321');
        expect(screen.getByTestId('modal-title')).toHaveTextContent('Jana Nováková');
        expect(screen.getByLabelText(/Kategorie/)).toHaveValue('cat-2');
        expect(screen.queryByTestId('modal-footer-note')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit})).toBeInTheDocument();
    });

    it('resets the form when the registration link switches to another registration', async () => {
        const EDIT_URL_2 = '/api/events/evt-1/registrations/member-2';
        mockRoutes((url) => {
            if (url === EDIT_URL) return jsonOk(editInitialData());
            if (url === EDIT_URL_2) {
                return jsonOk(editInitialData({
                    siCardNumber: '1112223',
                    category: {id: 'cat-1', name: 'H21'},
                    lastName: 'Skládaná',
                    _links: {self: {href: EDIT_URL_2}, event: {href: EVENT_URL}},
                }));
            }
            return jsonOk(eventData());
        });
        const view = renderDialog({registration: {href: EDIT_URL}});

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByLabelText(/SI čip/)).toHaveValue('7654321');

        view.rerender(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    <HalRouteProvider>
                        <EventRegistrationDialog registration={{href: EDIT_URL_2}} onClose={vi.fn()}/>
                    </HalRouteProvider>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByLabelText(/SI čip/)).toHaveValue('1112223');
        expect(screen.getByLabelText(/Kategorie/)).toHaveValue('cat-1');
        expect(screen.getByTestId('modal-title')).toHaveTextContent('Jana Skládaná');
    });

    it('renders category select and shared-service checkboxes only when template has the properties', async () => {
        const siOnlyTemplate = mockHalFormsTemplate({
            method: 'POST',
            target: '/api/events/evt-1/registrations',
            title: 'Přihlásit se',
            properties: [{name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true}],
        });
        mockRoutes((url) => url === PREFILL_URL
            ? jsonOk(prefillData({_templates: {registerForEvent: siOnlyTemplate}}))
            : jsonOk(eventData()));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.queryByLabelText(/Kategorie/)).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedTransport})).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('hides shared-service checkboxes the event does not offer even when the template has the properties', async () => {
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData({
            sharedTransportEnabled: false,
            sharedAccommodationEnabled: false,
        })));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedTransport})).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('hides only the shared-service checkbox whose offer is disabled', async () => {
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData({
            sharedAccommodationEnabled: false,
        })));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByRole('checkbox', {name: labels.fields.wantsSharedTransport})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('omits shared-service fields from the POST body when the event offers neither service', async () => {
        const user = userEvent.setup();
        mockRoutes((url, options) => {
            if (options?.method) return writeOk();
            if (url === EVENT_URL) return jsonOk(eventData({sharedTransportEnabled: false, sharedAccommodationEnabled: false}));
            if (url === PREFILL_URL) return jsonOk(prefillData());
            return jsonOk({});
        });
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        await waitFor(() => {
            const postCall = vi.mocked(authorizedFetch).mock.calls.find(([, options]) =>
                (options as RequestInit | undefined)?.method === 'POST');
            expect(postCall).toBeDefined();
            expect(JSON.parse((postCall?.[1] as RequestInit).body as string)).toEqual({
                siCardNumber: '1234567',
                categoryId: 'cat-1',
            });
        });
    });

    it('edit mode omits hidden shared-service fields from the PUT body', async () => {
        const user = userEvent.setup();
        mockRoutes((url, options) => {
            if (options?.method) return writeOk();
            if (url === EDIT_URL) return jsonOk(editInitialData());
            if (url === EVENT_URL) return jsonOk(eventData({sharedTransportEnabled: false, sharedAccommodationEnabled: false}));
            return jsonOk({});
        });
        renderDialog({registration: {href: EDIT_URL}});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

        await waitFor(() => {
            const putCall = vi.mocked(authorizedFetch).mock.calls.find(([, options]) =>
                (options as RequestInit | undefined)?.method === 'PUT');
            expect(putCall).toBeDefined();
            expect(JSON.parse((putCall?.[1] as RequestInit).body as string)).toEqual({
                siCardNumber: '7654321',
                categoryId: 'cat-2',
            });
        });
    });

    it('shows validation error and skips submit when SI chip is empty', async () => {
        const user = userEvent.setup();
        const onRegistered = vi.fn();
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData({siCardNumber: ''})) : jsonOk(eventData()));
        renderDialog({onRegistered});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        expect(await screen.findByText(labels.events.registrationModal.siChipRequired)).toBeInTheDocument();
        expect(authorizedFetch).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({method: 'POST'}),
            expect.anything(),
        );
        expect(onRegistered).not.toHaveBeenCalled();
    });

    it('shows validation error when SI chip does not match the template regex', async () => {
        const user = userEvent.setup();
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData({siCardNumber: ''})) : jsonOk(eventData()));
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        await user.type(siInput, 'abc');
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        expect(await screen.findByText(labels.events.registrationModal.siChipInvalidFormat)).toBeInTheDocument();
    });

    it('submits POST with only template-present fields to the normalized registerForEvent target', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        const onRegistered = vi.fn();
        mockRoutes((url, options) => {
            if (options?.method === 'POST') return writeOk();
            if (options?.method) return writeOk();
            if (url === EVENT_URL) return jsonOk(eventData());
            if (url === PREFILL_URL) return jsonOk(prefillData());
            return jsonOk({});
        });
        renderDialog({onClose, onRegistered});

        const siInput = await screen.findByLabelText(/SI čip/);
        await user.clear(siInput);
        await user.type(siInput, '1234567');
        await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
        await user.click(screen.getByRole('checkbox', {name: labels.fields.wantsSharedTransport}));
        await user.click(screen.getByRole('checkbox', {name: labels.fields.wantsSharedAccommodation}));
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        await waitFor(() => {
            expect(onClose).toHaveBeenCalled();
        });
        expect(onRegistered).toHaveBeenCalled();

        const postCall = vi.mocked(authorizedFetch).mock.calls.find(([, options]) =>
            (options as RequestInit | undefined)?.method === 'POST');
        expect(postCall?.[0]).toBe('/events/evt-1/registrations');
        expect(JSON.parse((postCall?.[1] as RequestInit).body as string)).toEqual({
            siCardNumber: '1234567',
            categoryId: 'cat-1',
            wantsSharedTransport: true,
            wantsSharedAccommodation: true,
        });
    });

    describe('notes', () => {
        const noteProps = [
            {name: 'internalNote', prompt: 'Interní poznámka', type: 'textarea', maxLength: 255},
            {name: 'orisNote', prompt: 'Poznámka do ORIS', type: 'textarea', maxLength: 255},
        ];
        const withNotes = () => ({
            ...newRegistrationTemplate(),
            properties: [...newRegistrationTemplate().properties, ...noteProps],
        });

        it('shows both note fields, caps length at the template maxLength and submits their values', async () => {
            const user = userEvent.setup();
            mockRoutes((url, options) => {
                if (options?.method) return writeOk();
                if (url === EVENT_URL) return jsonOk(eventData());
                if (url === PREFILL_URL) return jsonOk(prefillData({_templates: {registerForEvent: withNotes()}}));
                return jsonOk({});
            });
            renderDialog();

            const internal = await screen.findByLabelText(labels.fields.internalNote);
            const oris = screen.getByLabelText(labels.fields.orisNote);
            expect(internal).toHaveAttribute('maxlength', '255');
            await user.type(internal, 'moje poznámka');
            await user.type(oris, 'pro ORIS');
            expect(screen.getByText('13 / 255')).toBeInTheDocument();
            expect(screen.getByText('8 / 255')).toBeInTheDocument();
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

            await waitFor(() => {
                expect(vi.mocked(authorizedFetch).mock.calls.some(([, o]) => (o as RequestInit | undefined)?.method === 'POST')).toBe(true);
            });
            const postCall = vi.mocked(authorizedFetch).mock.calls.find(([, o]) => (o as RequestInit | undefined)?.method === 'POST');
            expect(JSON.parse((postCall?.[1] as RequestInit).body as string)).toMatchObject({
                internalNote: 'moje poznámka',
                orisNote: 'pro ORIS',
            });
        });

        it('does not render note fields when the template does not offer them', async () => {
            mockRoutes((url) => url === EVENT_URL ? jsonOk(eventData()) : jsonOk(prefillData()));
            renderDialog();

            await screen.findByLabelText(/SI čip/);
            expect(screen.queryByLabelText(labels.fields.internalNote)).not.toBeInTheDocument();
            expect(screen.queryByLabelText(labels.fields.orisNote)).not.toBeInTheDocument();
        });

        describe('edit mode', () => {
            const editWithNotes = () => ({
                ...editRegistrationTemplate(),
                properties: [...editRegistrationTemplate().properties, ...noteProps],
            });

            const mockEdit = () => mockRoutes((url, options) => {
                if (options?.method) return writeOk();
                if (url === EVENT_URL) return jsonOk(eventData());
                if (url === EDIT_URL) {
                    return jsonOk(editInitialData({
                        internalNote: 'stará interní',
                        orisNote: 'stará oris',
                        _templates: {editRegistration: editWithNotes()},
                    }));
                }
                return jsonOk({});
            });

            const putBody = () => {
                const putCall = vi.mocked(authorizedFetch).mock.calls.find(([, o]) =>
                    (o as RequestInit | undefined)?.method === 'PUT');
                return JSON.parse((putCall?.[1] as RequestInit).body as string);
            };

            it('prefills current notes and saves a changed note', async () => {
                const user = userEvent.setup();
                const onClose = vi.fn();
                mockEdit();
                renderDialog({registration: {href: EDIT_URL}, onClose});

                const internal = await screen.findByLabelText(labels.fields.internalNote);
                expect(internal).toHaveValue('stará interní');
                expect(screen.getByLabelText(labels.fields.orisNote)).toHaveValue('stará oris');

                await user.clear(internal);
                await user.type(internal, 'nová interní');
                await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

                await waitFor(() => expect(onClose).toHaveBeenCalled());
                expect(putBody()).toMatchObject({internalNote: 'nová interní', orisNote: 'stará oris'});
            });

            it('clearing a note does not resend the old value', async () => {
                const user = userEvent.setup();
                const onClose = vi.fn();
                mockEdit();
                renderDialog({registration: {href: EDIT_URL}, onClose});

                await user.clear(await screen.findByLabelText(labels.fields.orisNote));
                await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

                await waitFor(() => expect(onClose).toHaveBeenCalled());
                const body = putBody();
                expect(body.orisNote).not.toBe('stará oris');
                expect(body.orisNote ?? '').toBe('');
                expect(body.internalNote).toBe('stará interní');
            });
        });
    });

    it('edit mode submits PUT with values from fetched registration', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        mockRoutes((url, options) => {
            if (options?.method === 'PUT') return writeOk();
            if (options?.method) return writeOk();
            if (url === EVENT_URL) return jsonOk(eventData());
            if (url === EDIT_URL) return jsonOk(editInitialData());
            return jsonOk({});
        });
        renderDialog({registration: {href: EDIT_URL}, onClose});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('checkbox', {name: labels.fields.wantsSharedAccommodation}));
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

        await waitFor(() => {
            expect(onClose).toHaveBeenCalled();
        });

        const putCall = vi.mocked(authorizedFetch).mock.calls.find(([, options]) =>
            (options as RequestInit | undefined)?.method === 'PUT');
        expect(putCall?.[0]).toBe('/events/evt-1/registrations/member-1');
        expect(JSON.parse((putCall?.[1] as RequestInit).body as string)).toEqual({
            siCardNumber: '7654321',
            categoryId: 'cat-2',
            wantsSharedTransport: true,
            wantsSharedAccommodation: true,
        });
    });

    it('maps server-side field validation error onto the SI chip field', async () => {
        const user = userEvent.setup();
        mockRoutes((url, options) => {
            if (options?.method === 'PUT') {
                return Promise.reject(new FetchError(
                    'HTTP 400 (Bad Request)',
                    400,
                    'Bad Request',
                    new Headers({'Content-Type': 'application/problem+json'}),
                    JSON.stringify({fieldErrors: {siCardNumber: 'SI čip je již zaregistrovaný'}}),
                ));
            }
            if (options?.method) return writeOk();
            if (url === EVENT_URL) return jsonOk(eventData());
            if (url === EDIT_URL) return jsonOk(editInitialData());
            return jsonOk({});
        });
        renderDialog({registration: {href: EDIT_URL}});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

        expect(await screen.findByText('SI čip je již zaregistrovaný')).toBeInTheDocument();
        expect(screen.getByLabelText(/SI čip/)).toHaveClass('border-error');
    });

    it('shows alert with server message for non-validation errors and keeps entered data', async () => {
        const user = userEvent.setup();
        mockRoutes((url, options) => {
            if (options?.method === 'POST') {
                return Promise.reject(new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error', new Headers()));
            }
            if (options?.method) return writeOk();
            if (url === EVENT_URL) return jsonOk(eventData());
            if (url === PREFILL_URL) return jsonOk(prefillData({siCardNumber: ''}));
            return jsonOk({});
        });
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        await user.type(siInput, '1234567');
        await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        expect(await screen.findByText('HTTP 500 (Internal Server Error)')).toBeInTheDocument();
        expect(siInput).toHaveValue('1234567');
    });

    it('blocks submit with client-side error when a required category is not selected', async () => {
        const user = userEvent.setup();
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData({siCardNumber: ''})) : jsonOk(eventData()));
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        await user.type(siInput, '1234567');
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));

        expect(await screen.findByText(labels.events.registrationModal.categoryRequired)).toBeInTheDocument();
        expect(authorizedFetch).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({method: 'POST'}),
            expect.anything(),
        );
    });

    it('shows prefill load error alert and no form when the registration fetch fails', async () => {
        mockRoutes(() => Promise.reject(new FetchError('HTTP 500', 500, 'Internal Server Error', new Headers())));
        renderDialog();

        expect(await screen.findByText(labels.events.registrationModal.prefillLoadError)).toBeInTheDocument();
        expect(screen.queryByLabelText(/SI čip/)).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: labels.events.registrationModal.confirmNew})).not.toBeInTheDocument();
    });

    it('shows error alert without submit when the representation carries no registration affordance', async () => {
        const withoutTemplates = {...prefillData(), _templates: undefined};
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(withoutTemplates) : jsonOk(eventData()));
        renderDialog();

        expect(await screen.findByText(labels.events.registrationModal.noRegistrationAffordance)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: labels.events.registrationModal.confirmNew})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: labels.events.registrationModal.confirmEdit})).not.toBeInTheDocument();
    });

    describe('SI policy', () => {
        const RENT_LABEL = labels.events.registrationModal.rentSi;
        const submit = async (user: ReturnType<typeof userEvent.setup>) =>
            user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmNew}));
        const postBody = () => {
            const call = vi.mocked(authorizedFetch).mock.calls.find(([, o]) => (o as RequestInit | undefined)?.method === 'POST');
            return call ? JSON.parse((call[1] as RequestInit).body as string) : undefined;
        };
        const openNew = async (event: Record<string, unknown>, registration: Record<string, unknown> = {}) => {
            mockRoutes((url, options) => {
                if (options?.method === 'POST') return writeOk();
                return url === PREFILL_URL ? jsonOk(prefillData(registration)) : jsonOk(eventData(event));
            });
            renderDialog();
            await screen.findByTestId('event-registration-form');
        };

        it('NOT_USED hides SI field and rental, and does not send the prefilled profile number', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'NOT_USED', siRentalAllowed: true}, {categoryId: undefined, category: {id: 'cat-1'}});
            expect(screen.queryByLabelText(/SI čip/)).not.toBeInTheDocument();
            expect(screen.queryByLabelText(RENT_LABEL)).not.toBeInTheDocument();
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
            expect(postBody()).not.toHaveProperty('siCardNumber');
            expect(postBody()).not.toHaveProperty('wantsToRentSi');
        });

        it('OPTIONAL shows SI field without required marker, allows empty, rental only when allowed', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'OPTIONAL', siRentalAllowed: false}, {siCardNumber: '', category: {id: 'cat-1'}});
            const input = screen.getByLabelText(/SI čip/);
            expect(input.closest('div')!.parentElement!.querySelector('label span.text-red-600')).toBeNull();
            expect(screen.queryByLabelText(RENT_LABEL)).not.toBeInTheDocument();
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
        });

        it('OPTIONAL with rental allowed shows the rental tick box', async () => {
            await openNew({siUsage: 'OPTIONAL', siRentalAllowed: true});
            expect(screen.getByLabelText(RENT_LABEL)).toBeInTheDocument();
        });

        it('REQUIRED without rental shows SI as required and hides rental', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'REQUIRED', siRentalAllowed: false}, {siCardNumber: ''});
            expect(screen.queryByLabelText(RENT_LABEL)).not.toBeInTheDocument();
            await submit(user);
            expect(await screen.findByText(labels.events.registrationModal.siChipRequired)).toBeInTheDocument();
        });

        it('REQUIRED with rental requires SI unless the rental box is ticked', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'REQUIRED', siRentalAllowed: true}, {siCardNumber: ''});
            await submit(user);
            expect(await screen.findByText(labels.events.registrationModal.siChipRequiredUnlessRenting)).toBeInTheDocument();
            expect(postBody()).toBeUndefined();

            await user.click(screen.getByLabelText(RENT_LABEL));
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
            expect(postBody().wantsToRentSi).toBe(true);
        });

        it('submits wantsToRentSi as a boolean when the defaults carry no value for it', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'REQUIRED', siRentalAllowed: true}, {siCardNumber: '', wantsToRentSi: undefined});
            await user.click(screen.getByLabelText(RENT_LABEL));
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
            expect(postBody().wantsToRentSi).toBe(true);
        });

        it('never submits wantsToRentSi as an array when ticked and unticked again', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'OPTIONAL', siRentalAllowed: true}, {wantsToRentSi: undefined});
            await user.click(screen.getByLabelText(RENT_LABEL));
            await user.click(screen.getByLabelText(RENT_LABEL));
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
            expect(postBody().wantsToRentSi).toBe(false);
        });

        it('submits an unticked rental box as boolean false', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'OPTIONAL', siRentalAllowed: true}, {wantsToRentSi: undefined});
            await user.selectOptions(screen.getByLabelText(/Kategorie/), 'cat-1');
            await submit(user);
            await waitFor(() => expect(postBody()).toBeDefined());
            expect(postBody().wantsToRentSi).toBe(false);
        });

        it('still validates the format when a value is entered while renting', async () => {
            const user = userEvent.setup();
            await openNew({siUsage: 'REQUIRED', siRentalAllowed: true}, {siCardNumber: ''});
            await user.click(screen.getByLabelText(RENT_LABEL));
            await user.type(screen.getByLabelText(/SI čip/), 'abc');
            await submit(user);
            expect(await screen.findByText(labels.events.registrationModal.siChipInvalidFormat)).toBeInTheDocument();
        });

        it('edit mode applies the same rules and prefills the rental request', async () => {
            mockRoutes((url) => url === EDIT_URL
                ? jsonOk(editInitialData({siCardNumber: undefined, wantsToRentSi: true}))
                : jsonOk(eventData({siUsage: 'REQUIRED', siRentalAllowed: true})));
            renderDialog({registration: {href: EDIT_URL}});
            expect(await screen.findByLabelText(RENT_LABEL)).toBeChecked();
        });

        describe('edit mode of a registration that does not fit the policy', () => {
            const confirmEdit = (user: ReturnType<typeof userEvent.setup>) =>
                user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));
            const putCalls = () => vi.mocked(authorizedFetch).mock.calls
                .filter(([, o]) => (o as RequestInit | undefined)?.method === 'PUT');
            const openEdit = async (event: Record<string, unknown>, registration: Record<string, unknown>) => {
                mockRoutes((url, options) => {
                    if (options?.method === 'PUT') return writeOk();
                    return url === EDIT_URL ? jsonOk(editInitialData(registration)) : jsonOk(eventData(event));
                });
                renderDialog({registration: {href: EDIT_URL}});
                await screen.findByLabelText(/SI čip/);
            };

            it('REQUIRED without rental: no SI number and no rental shows the required error and blocks submit until a number is entered', async () => {
                const user = userEvent.setup();
                await openEdit({siUsage: 'REQUIRED', siRentalAllowed: false}, {siCardNumber: undefined});
                await confirmEdit(user);
                expect(await screen.findByText(labels.events.registrationModal.siChipRequired)).toBeInTheDocument();
                expect(putCalls()).toHaveLength(0);

                await user.type(screen.getByLabelText(/SI čip/), '1234567');
                await confirmEdit(user);
                await waitFor(() => expect(putCalls()).toHaveLength(1));
            });

            it('REQUIRED with rental allowed: neither number nor tick shows the error', async () => {
                const user = userEvent.setup();
                await openEdit({siUsage: 'REQUIRED', siRentalAllowed: true}, {siCardNumber: undefined, wantsToRentSi: false});
                await confirmEdit(user);
                expect(await screen.findByText(labels.events.registrationModal.siChipRequiredUnlessRenting)).toBeInTheDocument();
                expect(putCalls()).toHaveLength(0);
            });

            it('NOT_USED never shows an SI error', async () => {
                const user = userEvent.setup();
                mockRoutes((url, options) => {
                    if (options?.method === 'PUT') return writeOk();
                    return url === EDIT_URL
                        ? jsonOk(editInitialData({siCardNumber: undefined}))
                        : jsonOk(eventData({siUsage: 'NOT_USED'}));
                });
                renderDialog({registration: {href: EDIT_URL}});
                await screen.findByLabelText(/Kategorie/);
                await confirmEdit(user);
                await waitFor(() => expect(putCalls()).toHaveLength(1));
                expect(screen.queryByText(labels.events.registrationModal.siChipRequired)).not.toBeInTheDocument();
            });
        });

        it('read-only detail shows the rental request', async () => {
            mockRoutes((url) => url === EDIT_URL
                ? jsonOk(editInitialData({registeredAt: '2026-03-10T10:00:00', _templates: undefined, wantsToRentSi: true}))
                : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});
            const detail = await screen.findByTestId('event-registration-detail');
            expect(detail).toHaveTextContent(RENT_LABEL);
        });
    });

    describe('read-only detail', () => {
        const readOnlyData = (overrides: Record<string, unknown> = {}) => editInitialData({
            registeredAt: '2026-03-10T10:00:00',
            _templates: undefined,
            ...overrides,
        });

        const expectReadOnlyDetail = async () => {
            expect(await screen.findByTestId('event-registration-detail')).toBeInTheDocument();
            expect(screen.getByText('7654321')).toBeInTheDocument();
            expect(screen.getByText('D21')).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: labels.events.registrationModal.confirmEdit})).not.toBeInTheDocument();
            expect(screen.queryByText(labels.events.registrationModal.noRegistrationAffordance)).not.toBeInTheDocument();
        };

        it('shows the registration without inputs for an event coordinator (no register / edit template)', async () => {
            mockRoutes((url) => url === EDIT_URL ? jsonOk(readOnlyData()) : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});

            await expectReadOnlyDetail();
            expect(screen.getByText('Jana Nováková')).toBeInTheDocument();
        });

        it('shows notes read-only only when present', async () => {
            mockRoutes((url) => url === EDIT_URL
                ? jsonOk(readOnlyData({internalNote: 'tajná', orisNote: ''}))
                : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});

            await expectReadOnlyDetail();
            expect(screen.getByText(labels.fields.internalNote)).toBeInTheDocument();
            expect(screen.getByText('tajná')).toBeInTheDocument();
            expect(screen.queryByText(labels.fields.orisNote)).not.toBeInTheDocument();
        });

        it('shows the registration read-only for a member after the edit window has closed', async () => {
            mockRoutes((url) => url === EDIT_URL
                ? jsonOk(readOnlyData({_templates: {unregisterFromEvent: {method: 'DELETE', target: EDIT_URL, properties: []}}}))
                : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});

            await expectReadOnlyDetail();
        });

        it('shows the notes read-only for a member after the edit window has closed', async () => {
            mockRoutes((url) => url === EDIT_URL
                ? jsonOk(readOnlyData({
                    internalNote: 'moje interní',
                    orisNote: 'moje oris',
                    _templates: {unregisterFromEvent: {method: 'DELETE', target: EDIT_URL, properties: []}},
                }))
                : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});

            await expectReadOnlyDetail();
            expect(screen.getByText('moje interní')).toBeInTheDocument();
            expect(screen.getByText('moje oris')).toBeInTheDocument();
        });

        it.each([undefined, null])('shows the no-affordance error, not an empty detail, when registeredAt is %s', async (registeredAt) => {
            const data = editInitialData({registeredAt, _templates: undefined});
            mockRoutes((url) => url === EDIT_URL ? jsonOk(data) : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}});

            expect(await screen.findByText(labels.events.registrationModal.noRegistrationAffordance)).toBeInTheDocument();
            expect(screen.queryByTestId('event-registration-detail')).not.toBeInTheDocument();
        });

        it('closes via the close button', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();
            mockRoutes((url) => url === EDIT_URL ? jsonOk(readOnlyData()) : jsonOk(eventData()));
            renderDialog({registration: {href: EDIT_URL}, onClose});

            await screen.findByTestId('event-registration-detail');
            await user.click(screen.getAllByRole('button', {name: labels.buttons.close})[0]);

            expect(onClose).toHaveBeenCalled();
        });
    });

    it('cancel button invokes onClose', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        mockRoutes((url) => url === PREFILL_URL ? jsonOk(prefillData()) : jsonOk(eventData()));
        renderDialog({onClose});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.buttons.cancel}));

        expect(onClose).toHaveBeenCalled();
    });

    it('does not render anything and does not fetch when registration link is null', () => {
        renderDialog({registration: null});

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        const fetchedUrls = vi.mocked(authorizedFetch as Mock).mock.calls.map(([url]) => url);
        expect(fetchedUrls).not.toContain(PREFILL_URL);
        expect(fetchedUrls).not.toContain(EDIT_URL);
        expect(fetchedUrls).not.toContain(EVENT_URL);
    });
});
