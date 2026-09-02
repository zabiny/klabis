import '@testing-library/jest-dom';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useRegistrationDialogData} from './useRegistrationDialogData';
import {createDelayedMockResponse, createMockResponse} from '../__mocks__/mockFetch';
import {authorizedFetch, FetchError} from '../api/authorizedFetch';
import {mockHalFormsTemplate, mockLink} from '../__mocks__/halData';
import {type Mock, vi} from 'vitest';

vi.mock('../api/authorizedFetch', () => ({
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
const EDIT_URL = '/api/events/evt-1/registrations/member-1';
const EVENT_URL = '/api/events/evt-1';

const CATEGORY_OPTIONS = {inline: [{value: 'cat-1', prompt: 'H21'}, {value: 'cat-2', prompt: 'D21'}]};

const newRegistrationTemplate = () => mockHalFormsTemplate({
    method: 'POST',
    target: '/api/events/evt-1/registrations',
    title: 'Přihlásit se',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true, regex: '\\d{4,8}'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', required: true, options: CATEGORY_OPTIONS},
        {name: 'wantsSharedTransport', prompt: 'Společná doprava', type: 'Boolean'},
        {name: 'wantsSharedAccommodation', prompt: 'Společné ubytování', type: 'Boolean'},
    ],
});

const editRegistrationTemplate = () => mockHalFormsTemplate({
    method: 'PUT',
    target: EDIT_URL,
    title: 'Upravit přihlášku',
    properties: [
        {name: 'siCardNumber', prompt: 'SI čip', type: 'text', required: true, regex: '\\d{4,8}'},
        {name: 'categoryId', prompt: 'Kategorie', type: 'text', options: CATEGORY_OPTIONS},
    ],
});

const prefillRegistration = {
    siCardNumber: '1234567',
    firstName: 'Jana',
    lastName: 'Nováková',
    wantsSharedTransport: false,
    wantsSharedAccommodation: false,
    _links: {
        self: mockLink(PREFILL_URL),
        event: mockLink(EVENT_URL),
    },
    _templates: {registerForEvent: newRegistrationTemplate()},
};

const editRegistration = {
    siCardNumber: '7654321',
    firstName: 'Jana',
    lastName: 'Nováková',
    category: {id: 'cat-2', name: 'D21'},
    wantsSharedTransport: true,
    _links: {
        self: mockLink(EDIT_URL),
        event: mockLink(EVENT_URL),
    },
    _templates: {editRegistration: editRegistrationTemplate()},
};

const eventData = {
    name: 'Jarní závod',
    eventDate: '2026-04-15',
    location: 'Brno - Bystrc',
    deadlines: ['2026-01-15', '2026-03-15'],
    sharedTransportEnabled: true,
    sharedAccommodationEnabled: false,
    _links: {self: mockLink(EVENT_URL)},
};

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return ({children}: {children: ReactNode}) =>
        React.createElement(QueryClientProvider, {client: queryClient}, children);
};

const mockRoutes = (routes: Record<string, () => Promise<Response> | Response>) => {
    vi.mocked(authorizedFetch as Mock).mockImplementation(((url: string) => {
        const route = Object.keys(routes).find(pattern => url === pattern || url.startsWith(pattern));
        if (route) return routes[route]();
        return Promise.resolve(createMockResponse({}));
    }) as typeof authorizedFetch);
};

describe('useRegistrationDialogData', () => {
    beforeEach(() => {
        vi.mocked(authorizedFetch as Mock).mockReset();
    });

    it('fetches the registration representation first and the event second', async () => {
        mockRoutes({
            [PREFILL_URL]: () => createMockResponse(prefillRegistration),
            [EVENT_URL]: () => createMockResponse(eventData),
        });
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.eventContext).toBeDefined());

        const calledUrls = vi.mocked(authorizedFetch as Mock).mock.calls.map(([url]) => url);
        expect(calledUrls).toEqual([PREFILL_URL, EVENT_URL]);
    });

    it('derives new mode, template, member name and prefill values from registerForEvent affordance', async () => {
        mockRoutes({
            [PREFILL_URL]: () => createMockResponse(prefillRegistration),
            [EVENT_URL]: () => createMockResponse(eventData),
        });
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.mode).toBe('new'));

        expect(result.current.template?.target).toBe('/api/events/evt-1/registrations');
        expect(result.current.template?.method).toBe('POST');
        expect(result.current.memberName).toBe('Jana Nováková');
        expect(result.current.initialValues.siCardNumber).toBe('1234567');
        expect(result.current.initialValues.categoryId).toBe('');
        expect(result.current.initialValues.wantsSharedTransport).toBe(false);
        expect(result.current.initialValues.wantsSharedAccommodation).toBe(false);
    });

    it('derives edit mode and prefilled category from editRegistration affordance', async () => {
        mockRoutes({
            [EDIT_URL]: () => createMockResponse(editRegistration),
            [EVENT_URL]: () => createMockResponse(eventData),
        });
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(EDIT_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.mode).toBe('edit'));

        expect(result.current.template?.method).toBe('PUT');
        expect(result.current.memberName).toBe('Jana Nováková');
        expect(result.current.initialValues.siCardNumber).toBe('7654321');
        expect(result.current.initialValues.categoryId).toBe('cat-2');
        expect(result.current.initialValues.wantsSharedTransport).toBe(true);
    });

    it('builds event context from the event representation', async () => {
        mockRoutes({
            [PREFILL_URL]: () => createMockResponse(prefillRegistration),
            [EVENT_URL]: () => createMockResponse(eventData),
        });
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.eventContext).toBeDefined());

        expect(result.current.eventContext).toEqual({
            name: 'Jarní závod',
            eventDate: '2026-04-15',
            location: 'Brno - Bystrc',
            deadlines: ['2026-01-15', '2026-03-15'],
            sharedTransportEnabled: true,
            sharedAccommodationEnabled: false,
        });
    });

    it('stays loading while the registration and event fetches are chained', async () => {
        vi.mocked(authorizedFetch as Mock).mockImplementation(((url: string) => {
            if (url === PREFILL_URL) return createDelayedMockResponse(prefillRegistration, 30);
            if (url === EVENT_URL) return createDelayedMockResponse(eventData, 30);
            return Promise.resolve(createMockResponse({}));
        }) as typeof authorizedFetch);
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        expect(result.current.isLoading).toBe(true);

        await vi.waitFor(() => expect(result.current.eventContext).toBeDefined());
        expect(result.current.isLoading).toBe(false);
    });

    it('propagates an error from the registration fetch', async () => {
        vi.mocked(authorizedFetch as Mock).mockRejectedValue(
            new FetchError('HTTP 500', 500, 'Internal Server Error', new Headers()));
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.error).toBeTruthy());
        expect(result.current.mode).toBeUndefined();
    });

    it('propagates an error from the event fetch after the registration loaded', async () => {
        vi.mocked(authorizedFetch as Mock).mockImplementation(((url: string) => {
            if (url === EDIT_URL) return Promise.resolve(createMockResponse(editRegistration));
            return Promise.reject(new FetchError('HTTP 500', 500, 'Internal Server Error', new Headers()));
        }) as typeof authorizedFetch);
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(EDIT_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.error).toBeTruthy());
        expect(result.current.mode).toBe('edit');
        expect(result.current.eventContext).toBeUndefined();
    });

    it('has no mode when the registration carries no write affordance', async () => {
        const withoutTemplates = {...prefillRegistration, _templates: undefined};
        mockRoutes({
            [PREFILL_URL]: () => createMockResponse(withoutTemplates),
            [EVENT_URL]: () => createMockResponse(eventData),
        });
        const {result} = renderHook(() => useRegistrationDialogData(mockLink(PREFILL_URL)), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(result.current.mode).toBeUndefined();
        expect(result.current.template).toBeUndefined();
        expect(result.current.memberName).toBe('Jana Nováková');
    });

    it('does not fetch when registration link is null', () => {
        const {result} = renderHook(() => useRegistrationDialogData(null), {wrapper: createWrapper()});

        expect(result.current.isLoading).toBe(false);
        expect(result.current.mode).toBeUndefined();
        expect(authorizedFetch).not.toHaveBeenCalled();
    });
});
