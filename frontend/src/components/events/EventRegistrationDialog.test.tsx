import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {EventRegistrationDialog, type EventRegistrationDialogProps} from './EventRegistrationDialog.tsx';
import {createMockResponse, createDelayedMockResponse} from '../../__mocks__/mockFetch.ts';
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

const MOCK_EVENT = {
    name: 'Jarní závod',
    eventDate: '2026-04-15',
    location: 'Brno - Bystrc',
    deadlines: ['2026-01-15', '2026-03-15'],
    sharedTransportEnabled: true,
    sharedAccommodationEnabled: true,
};

const CATEGORY_OPTIONS = {inline: [{value: 'cat-1', prompt: 'H21'}, {value: 'cat-2', prompt: 'D21'}]};

const newRegistrationTemplate = (): HalFormsTemplate => mockHalFormsTemplate({
    method: 'POST',
    target: '/api/events/evt-1/registrations',
    title: 'Přihlásit se',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true, regex: '\\d{6,7}'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', required: true, options: CATEGORY_OPTIONS},
        {name: 'wantsSharedTransport', prompt: 'Společná doprava', type: 'Boolean'},
        {name: 'wantsSharedAccommodation', prompt: 'Společné ubytování', type: 'Boolean'},
    ],
});

const editRegistrationTemplate = (): HalFormsTemplate => mockHalFormsTemplate({
    method: 'PUT',
    target: '/api/events/evt-1/registrations/member-1',
    title: 'Upravit přihlášku',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true, regex: '\\d{6,7}'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', options: CATEGORY_OPTIONS},
        {name: 'wantsSharedTransport', prompt: 'Společná doprava', type: 'Boolean'},
        {name: 'wantsSharedAccommodation', prompt: 'Společné ubytování', type: 'Boolean'},
    ],
});

const prefillData = {
    siCardNumber: '1234567',
    firstName: 'Jana',
    lastName: 'Nováková',
    wantsSharedTransport: false,
    wantsSharedAccommodation: false,
    _links: {self: {href: '/api/events/evt-1/registrations/M001?newRegistration=true'}},
};

const editInitialData = {
    siCardNumber: '7654321',
    firstName: 'Jana',
    lastName: 'Nováková',
    category: {id: 'cat-2', name: 'D21'},
    wantsSharedTransport: true,
    wantsSharedAccommodation: false,
    _links: {self: {href: '/api/events/evt-1/registrations/member-1'}},
};

describe('EventRegistrationDialog', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {queries: {retry: false, gcTime: 0}},
        });
        vi.mocked(authorizedFetch).mockReset();
    });

    const renderDialog = (props: Partial<EventRegistrationDialogProps> = {}) => {
        const defaultProps: EventRegistrationDialogProps = {
            isOpen: true,
            onClose: vi.fn(),
            mode: 'new',
            template: newRegistrationTemplate(),
            event: MOCK_EVENT,
            prefillHref: '/api/events/evt-1/registrations/M001?newRegistration=true',
        };
        return render(
            <QueryClientProvider client={queryClient}>
                <EventRegistrationDialog {...defaultProps} {...props}/>
            </QueryClientProvider>,
        );
    };

    it('shows skeleton while prefill data is loading', () => {
        vi.mocked(authorizedFetch as Mock).mockReturnValue(createDelayedMockResponse(prefillData, 200));
        renderDialog();

        expect(screen.getByTestId('event-registration-dialog-skeleton')).toBeInTheDocument();
    });

    it('renders new-registration form prefilled with SI chip from member profile', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        expect(siInput).toHaveValue('1234567');
        expect(screen.getByText(labels.events.registrationModal.siChipHelperPrefilled)).toBeInTheDocument();
    });

    it('renders context strip with event name, date and location', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByTestId('modal-context')).toHaveTextContent('Jarní závod');
        expect(screen.getByTestId('modal-context')).toHaveTextContent('15. 4. 2026');
        expect(screen.getByTestId('modal-context')).toHaveTextContent('Brno - Bystrc');
    });

    it('does not show member chip in new mode even when prefill carries member names', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog();

        await screen.findByLabelText(/SI čip/);
        expect(screen.queryByTestId('registration-member-chip')).not.toBeInTheDocument();
    });

    it('shows deadline chip with the currently relevant deadline', async () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-02-01'));
        try {
            vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
            renderDialog();

            await vi.waitFor(() => {
                expect(screen.getByTestId('registration-deadline-chip')).toHaveTextContent('Přihlášky do 15. 3. 2026');
            });
        } finally {
            vi.useRealTimers();
        }
    });

    it('edit mode shows member chip, prefilled SI chip and category, and footer note', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(editInitialData));
        renderDialog({
            mode: 'edit',
            template: editRegistrationTemplate(),
            prefillHref: undefined,
            initialValuesHref: '/api/events/evt-1/registrations/member-1',
        });

        const siInput = await screen.findByLabelText(/SI čip/);
        expect(siInput).toHaveValue('7654321');
        expect(screen.getByTestId('registration-member-chip')).toHaveTextContent('Jana Nováková');
        expect(screen.getByTestId('registration-member-chip')).toHaveTextContent(labels.events.registrationModal.editingCaption);
        expect(screen.getByLabelText(/Kategorie/)).toHaveValue('cat-2');
        expect(screen.getByTestId('modal-footer-note')).toHaveTextContent(labels.events.registrationModal.editFooterNote);
        expect(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit})).toBeInTheDocument();
    });

    it('renders category select and shared-service checkboxes only when template has the properties', async () => {
        const siOnlyTemplate = mockHalFormsTemplate({
            method: 'POST',
            target: '/api/events/evt-1/registrations',
            title: 'Přihlásit se',
            properties: [{name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true}],
        });
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse({siCardNumber: ''}));
        renderDialog({template: siOnlyTemplate});

        await screen.findByLabelText(/SI čip/);
        expect(screen.queryByLabelText(/Kategorie/)).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedTransport})).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('hides shared-service checkboxes the event does not offer even when the template has the properties', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog({event: {...MOCK_EVENT, sharedTransportEnabled: false, sharedAccommodationEnabled: false}});

        await screen.findByLabelText(/SI čip/);
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedTransport})).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('omits shared-service fields from the POST body when the event offers neither service', async () => {
        const user = userEvent.setup();
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'POST') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse(prefillData));
        }) as typeof authorizedFetch);
        renderDialog({event: {...MOCK_EVENT, sharedTransportEnabled: false, sharedAccommodationEnabled: false}});

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

    it('hides only the shared-service checkbox whose offer is disabled', async () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog({event: {...MOCK_EVENT, sharedAccommodationEnabled: false}});

        await screen.findByLabelText(/SI čip/);
        expect(screen.getByRole('checkbox', {name: labels.fields.wantsSharedTransport})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: labels.fields.wantsSharedAccommodation})).not.toBeInTheDocument();
    });

    it('edit mode omits hidden shared-service fields from the PUT body', async () => {
        const user = userEvent.setup();
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'PUT') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse(editInitialData));
        }) as typeof authorizedFetch);
        renderDialog({
            mode: 'edit',
            template: editRegistrationTemplate(),
            event: {...MOCK_EVENT, sharedTransportEnabled: false, sharedAccommodationEnabled: false},
            prefillHref: undefined,
            initialValuesHref: '/api/events/evt-1/registrations/member-1',
        });

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
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse({siCardNumber: ''}));
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
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse({siCardNumber: ''}));
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
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'POST') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse(prefillData));
        }) as typeof authorizedFetch);
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

    it('edit mode submits PUT with values from fetched registration', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'PUT') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse(editInitialData));
        }) as typeof authorizedFetch);
        renderDialog({
            mode: 'edit',
            template: editRegistrationTemplate(),
            prefillHref: undefined,
            initialValuesHref: '/api/events/evt-1/registrations/member-1',
            onClose,
        });

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
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'PUT') {
                return Promise.reject(new FetchError(
                    'HTTP 400 (Bad Request)',
                    400,
                    'Bad Request',
                    new Headers({'Content-Type': 'application/problem+json'}),
                    JSON.stringify({fieldErrors: {siCardNumber: 'SI čip je již zaregistrovaný'}}),
                ));
            }
            return Promise.resolve(createMockResponse(editInitialData));
        }) as typeof authorizedFetch);
        renderDialog({
            mode: 'edit',
            template: editRegistrationTemplate(),
            prefillHref: undefined,
            initialValuesHref: '/api/events/evt-1/registrations/member-1',
        });

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.events.registrationModal.confirmEdit}));

        expect(await screen.findByText('SI čip je již zaregistrovaný')).toBeInTheDocument();
        expect(screen.getByLabelText(/SI čip/)).toHaveClass('border-error');
    });

    it('shows alert with server message for non-validation errors and keeps entered data', async () => {
        const user = userEvent.setup();
        vi.mocked(authorizedFetch as Mock).mockImplementation(((_url: string, options?: RequestInit) => {
            if (options?.method === 'POST') {
                return Promise.reject(new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error', new Headers()));
            }
            return Promise.resolve(createMockResponse({siCardNumber: ''}));
        }) as typeof authorizedFetch);
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
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse({siCardNumber: ''}));
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

    it('shows prefill load error alert and renders empty form when prefill fetch fails', async () => {
        vi.mocked(authorizedFetch as Mock).mockRejectedValue(new FetchError('HTTP 500', 500, 'Internal Server Error', new Headers()));
        renderDialog();

        const siInput = await screen.findByLabelText(/SI čip/);
        expect(siInput).toHaveValue('');
        expect(await screen.findByText(labels.events.registrationModal.prefillLoadError)).toBeInTheDocument();
    });

    it('cancel button invokes onClose', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog({onClose});

        await screen.findByLabelText(/SI čip/);
        await user.click(screen.getByRole('button', {name: labels.buttons.cancel}));

        expect(onClose).toHaveBeenCalled();
    });

    it('does not render anything when isOpen is false', () => {
        vi.mocked(authorizedFetch as Mock).mockResolvedValue(createMockResponse(prefillData));
        renderDialog({isOpen: false});

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
