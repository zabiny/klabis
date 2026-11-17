import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type {UserEvent} from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {UseQueryResult} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ImportOrisEventsButton} from './ImportOrisEventsButton';
import {mockHalFormsTemplate, mockHalResponse} from '../../__mocks__/halData';
import {createTestQueryClient} from '../../__mocks__/halDialogTest.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch';
import {authorizedFetch} from '../../api/authorizedFetch';
import {ApiError} from '../../api/apiError';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {HalFormsTemplate, HalResponse} from '../../api';
import type {BulkImportResult, OrisEvent} from '../../api/orisEvents';

vi.mock('../../hooks/useAuthorizedFetch', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../hooks/useAuthorizedFetch')>()),
    useAuthorizedQuery: vi.fn(),
}));

vi.mock('../../api/authorizedFetch', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../api/authorizedFetch')>()),
    authorizedFetch: vi.fn(),
}));

const importTemplate = (overrides: Partial<HalFormsTemplate> = {}): HalFormsTemplate =>
    mockHalFormsTemplate({
        method: 'POST',
        target: '/api/events/importFromOris',
        properties: [{name: 'orisIds', type: 'array', multi: true, max: 50}],
        ...overrides,
    });

const eventsResource = (templates: Record<string, HalFormsTemplate> = {}): HalResponse =>
    mockHalResponse({_templates: templates});

const batchResource = (template: HalFormsTemplate = importTemplate()): HalResponse =>
    eventsResource({importEventsBatch: template});

const orisEvent = (id: number, name: string): OrisEvent => ({
    id,
    name,
    date: '2025-04-10',
    location: 'Brno',
    organizer: 'ZBM',
});

const FEED: OrisEvent[] = [
    {id: 101, name: 'Jarní sprint', date: '2025-04-10', location: 'Brno', organizer: 'ZBM'},
    {id: 202, name: 'Letní závod', date: '2025-07-20', location: null, organizer: 'HBT'},
    {id: 303, name: 'Podzimní tour', date: '2025-09-15', location: 'Praha', organizer: 'SKP'},
];

const MIXED_RESULT: BulkImportResult = {
    totalProcessed: 2,
    successCount: 1,
    failureCount: 1,
    results: [
        {orisId: 101, name: 'Jarní sprint', date: '2025-04-10', status: 'IMPORTED'},
        {orisId: 404, name: null, date: null, status: 'FAILED', error: 'Duplikátní import'},
    ],
};

const SUCCESS_RESULT: BulkImportResult = {
    totalProcessed: 1,
    successCount: 1,
    failureCount: 0,
    results: [{orisId: 101, name: 'Jarní sprint', date: '2025-04-10', status: 'IMPORTED'}],
};

const importButton = () => screen.queryByRole('button', {name: /importovat z oris/i});
const checkbox = (name: string) => screen.getByRole('checkbox', {name});
const submitButton = (selected: number) =>
    screen.getByRole('button', {name: new RegExp(`importovat vybrané \\(${selected}\\)`, 'i')});
const selectAllCheckbox = () => screen.getByRole('checkbox', {name: 'Vybrat vše'});

describe('ImportOrisEventsButton', () => {
    let queryClient: QueryClient;

    const stubFeed = (state: {data?: OrisEvent[]; isError?: boolean; isSuccess?: boolean} = {}) => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: undefined,
            isError: false,
            isSuccess: false,
            ...state,
        } as unknown as UseQueryResult<unknown, ApiError>);
    };

    const renderModule = (resource: HalResponse = batchResource()) =>
        render(
            <QueryClientProvider client={queryClient}>
                <ImportOrisEventsButton resource={resource}/>
            </QueryClientProvider>,
        );

    const openModule = async ({
        resource = batchResource(),
        feed = {data: FEED, isSuccess: true},
    }: {
        resource?: HalResponse;
        feed?: {data?: OrisEvent[]; isError?: boolean; isSuccess?: boolean};
    } = {}): Promise<UserEvent> => {
        const user = userEvent.setup();
        stubFeed(feed);
        renderModule(resource);
        await user.click(importButton() as HTMLElement);
        return user;
    };

    const importTwoEvents = async (user: UserEvent) => {
        await user.click(checkbox('Jarní sprint'));
        await user.click(checkbox('Letní závod'));
        await user.click(submitButton(2));
    };

    beforeEach(() => {
        queryClient = createTestQueryClient();
        vi.mocked(useAuthorizedQuery).mockReset();
        vi.mocked(authorizedFetch).mockReset();
        stubFeed();
    });

    describe('presence rule', () => {
        it('renders nothing when the resource carries no import template', () => {
            renderModule(eventsResource());

            expect(importButton()).toBeNull();
        });

        it('renders the button when the resource carries only the single-event import template', () => {
            renderModule(eventsResource({importEvent: importTemplate()}));

            expect(importButton()).toBeInTheDocument();
        });

        it('renders the button when the resource carries the batch import template', () => {
            renderModule(eventsResource({importEventsBatch: importTemplate()}));

            expect(importButton()).toBeInTheDocument();
        });
    });

    describe('feed states', () => {
        it('does not query the feed while the dialog is closed', () => {
            renderModule();

            expect(useAuthorizedQuery).toHaveBeenLastCalledWith(undefined);
        });

        it('loads the ORIS feed for the default region when the dialog opens', async () => {
            await openModule();

            expect(useAuthorizedQuery).toHaveBeenLastCalledWith('/api/oris/events?region=JIHOMORAVSKA');
            expect(screen.getByTestId('modal-title')).toHaveTextContent('Importovat z ORIS');
        });

        it('shows the loading state while the feed is being fetched', async () => {
            await openModule({feed: {}});

            expect(screen.getByText(/komunikuji s oris/i)).toBeInTheDocument();
            expect(submitButton(0)).toBeDisabled();
        });

        it('shows the failure text when the feed cannot be loaded', async () => {
            await openModule({feed: {isError: true}});

            expect(screen.getByText('Nepodařilo se načíst akce z ORIS.')).toBeInTheDocument();
            expect(submitButton(0)).toBeDisabled();
        });

        it('shows the empty state when the region offers no events', async () => {
            await openModule({feed: {data: [], isSuccess: true}});

            expect(screen.getByText('Žádné závody k importu')).toBeInTheDocument();
            expect(screen.getByText('Zkuste změnit oblast.')).toBeInTheDocument();
            expect(submitButton(0)).toBeDisabled();
        });

        it('renders one row per feed event with name, date, organizer and location', async () => {
            await openModule();

            expect(screen.getByText('Jarní sprint')).toBeInTheDocument();
            expect(screen.getAllByText(/2025-04-10/)[0]).toBeInTheDocument();
            expect(screen.getByText(/ZBM/)).toBeInTheDocument();
            expect(screen.getByText(/Brno/)).toBeInTheDocument();
            expect(screen.getByText('Podzimní tour')).toBeInTheDocument();
        });

        it('shows the shown-of counter only when more than ten events are offered', async () => {
            await openModule({
                feed: {data: Array.from({length: 11}, (_, i) => orisEvent(i + 1, `Závod ${i + 1}`)), isSuccess: true},
            });

            expect(screen.getByText('zobrazeno 11 z 11')).toBeInTheDocument();
        });

        it('hides the shown-of counter for a short list', async () => {
            await openModule();

            expect(screen.queryByText(/zobrazeno/i)).not.toBeInTheDocument();
        });
    });

    describe('region switch', () => {
        it('refetches the feed for the picked region', async () => {
            const user = await openModule();

            await user.click(screen.getByRole('radio', {name: 'ČR'}));

            expect(useAuthorizedQuery).toHaveBeenLastCalledWith('/api/oris/events?region=CR');
        });

        it('clears the selection when the region changes', async () => {
            const user = await openModule();
            await user.click(checkbox('Jarní sprint'));
            expect(submitButton(1)).toBeEnabled();

            await user.click(screen.getByRole('radio', {name: 'Žebříček Morava'}));

            expect(checkbox('Jarní sprint')).not.toBeChecked();
            expect(submitButton(0)).toBeDisabled();
        });
    });

    describe('selection limit', () => {
        const limitedResource = (max: number) => batchResource(importTemplate({
            properties: [{name: 'orisIds', type: 'array', multi: true, max}],
        }));

        it('takes the limit from the multi property max', async () => {
            const user = await openModule({resource: limitedResource(2)});

            await user.click(checkbox('Jarní sprint'));
            await user.click(checkbox('Letní závod'));

            expect(screen.getByText('Můžete vybrat nejvýše 2 závodů.')).toBeInTheDocument();
            expect(checkbox('Podzimní tour')).toBeDisabled();
        });

        it('falls back to a limit of 50 when the multi property has no max', async () => {
            const user = await openModule({
                resource: batchResource(importTemplate({
                    properties: [{name: 'orisIds', type: 'array', multi: true}],
                })),
                feed: {data: Array.from({length: 51}, (_, i) => orisEvent(i + 1, `Závod ${i + 1}`)), isSuccess: true},
            });

            await user.click(selectAllCheckbox());

            expect(submitButton(50)).toBeInTheDocument();
            expect(screen.getByText('Můžete vybrat nejvýše 50 závodů.')).toBeInTheDocument();
        });

        it('does not add another event once the limit is reached, but allows removing one', async () => {
            const user = await openModule({resource: limitedResource(2)});
            await user.click(checkbox('Jarní sprint'));
            await user.click(checkbox('Letní závod'));

            expect(checkbox('Podzimní tour')).toBeDisabled();
            expect(submitButton(2)).toBeInTheDocument();

            await user.click(checkbox('Jarní sprint'));
            expect(submitButton(1)).toBeInTheDocument();
            expect(checkbox('Podzimní tour')).toBeEnabled();
        });

        it('select-all selects at most the limit and leaves the box indeterminate', async () => {
            const user = await openModule({resource: limitedResource(2)});

            await user.click(selectAllCheckbox());

            expect(submitButton(2)).toBeInTheDocument();
            expect(selectAllCheckbox()).not.toBeChecked();
            expect((selectAllCheckbox() as HTMLInputElement).indeterminate).toBe(true);
        });

        it('select-all deselects everything when the limit is reached', async () => {
            const user = await openModule({resource: limitedResource(2)});
            await user.click(selectAllCheckbox());
            expect(submitButton(2)).toBeInTheDocument();

            await user.click(selectAllCheckbox());

            expect(submitButton(0)).toBeDisabled();
            expect((selectAllCheckbox() as HTMLInputElement).indeterminate).toBe(false);
        });
    });

    describe('submitting the batch import', () => {
        it('submits the selection to the template target and renders the per-event result', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(MIXED_RESULT));
            const user = await openModule();

            await importTwoEvents(user);

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            const [url, options] = vi.mocked(authorizedFetch).mock.calls[0];
            expect(url).toBe('/api/events/importFromOris');
            expect((options as RequestInit).method).toBe('POST');
            expect(JSON.parse(String((options as RequestInit).body))).toEqual({orisIds: [101, 202]});

            expect(await screen.findByText('Výsledek importu')).toBeInTheDocument();
            expect(screen.getByText('Naimportováno 1 z 2')).toBeInTheDocument();
            expect(screen.getByText('Jarní sprint')).toBeInTheDocument();
            expect(screen.getByText('ORIS #404')).toBeInTheDocument();
            expect(screen.getByText('Duplikátní import')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Hotovo'})).toBeInTheDocument();
            expect(screen.queryByLabelText('Vybrat vše')).not.toBeInTheDocument();
        });

        it('submits to the batch template target when both import templates are present', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(SUCCESS_RESULT));
            const user = await openModule({
                resource: eventsResource({
                    importEventsBatch: importTemplate({target: '/api/events/import-batch'}),
                    importEvent: importTemplate({target: '/api/events/import-single'}),
                }),
            });

            await user.click(checkbox('Jarní sprint'));
            await user.click(submitButton(1));

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            expect(vi.mocked(authorizedFetch).mock.calls[0][0]).toBe('/api/events/import-batch');
        });

        it('normalises a lowercase imported status into the success rendering', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse({
                totalProcessed: 2,
                successCount: 1,
                failureCount: 1,
                results: [
                    {orisId: 101, name: 'Jarní sprint', date: '2025-04-10', status: 'imported'},
                    {orisId: 202, name: 'Letní závod', date: '2025-07-20', status: 'failed'},
                ],
            } as unknown as BulkImportResult));
            const user = await openModule();

            await importTwoEvents(user);

            const imported = (await screen.findByText('Jarní sprint')).closest('li');
            expect(imported?.querySelector('svg')).toHaveClass('text-green-600');
            expect(screen.getByText('Jarní sprint')).not.toHaveClass('text-red-700');

            const failed = screen.getByText('Letní závod').closest('li');
            expect(failed?.querySelector('svg')).toHaveClass('text-red-600');
            expect(screen.getByText('Letní závod')).toHaveClass('text-red-700');
        });

        it('falls back to POST when the template carries no method', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(MIXED_RESULT));
            const user = await openModule({resource: batchResource(importTemplate({method: undefined}))});

            await importTwoEvents(user);

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            expect((vi.mocked(authorizedFetch).mock.calls[0][1] as RequestInit).method).toBe('POST');
        });

        it('uses the template method when it is not POST', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(MIXED_RESULT));
            const user = await openModule({resource: batchResource(importTemplate({method: 'PUT'}))});

            await importTwoEvents(user);

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            expect((vi.mocked(authorizedFetch).mock.calls[0][1] as RequestInit).method).toBe('PUT');
        });

        it('uses the multi property name as the body key', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(MIXED_RESULT));
            const user = await openModule({
                resource: batchResource(importTemplate({
                    properties: [{name: 'eventOrisIds', type: 'array', multi: true, max: 10}],
                })),
            });
            await user.click(checkbox('Jarní sprint'));

            await user.click(submitButton(1));

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            const body = JSON.parse(String((vi.mocked(authorizedFetch).mock.calls[0][1] as RequestInit).body));
            expect(body).toEqual({eventOrisIds: [101]});
        });

        it('falls back to orisIds as the body key when the template has no multi property', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(MIXED_RESULT));
            const user = await openModule({resource: batchResource(importTemplate({properties: []}))});
            await user.click(checkbox('Jarní sprint'));

            await user.click(submitButton(1));

            await waitFor(() => expect(authorizedFetch).toHaveBeenCalled());
            const body = JSON.parse(String((vi.mocked(authorizedFetch).mock.calls[0][1] as RequestInit).body));
            expect(body).toEqual({orisIds: [101]});
        });

        it('shows the failure text and keeps the selection when the submit fails', async () => {
            vi.mocked(authorizedFetch).mockRejectedValue(new ApiError(500));
            const user = await openModule();

            await importTwoEvents(user);

            expect(await screen.findByText('Import se nezdařil. Zkuste to prosím znovu.')).toBeInTheDocument();
            expect(submitButton(2)).toBeEnabled();
            expect(screen.queryByText('Výsledek importu')).not.toBeInTheDocument();
        });

        it('shows progress and disables closing while the import runs', async () => {
            vi.mocked(authorizedFetch).mockReturnValue(new Promise<Response>(() => undefined));
            const user = await openModule();
            await user.click(checkbox('Jarní sprint'));

            await user.click(submitButton(1));

            expect(await screen.findByRole('button', {name: /importuji/i})).toBeDisabled();
            expect(screen.getByRole('button', {name: 'Zrušit'})).toBeDisabled();
        });
    });

    describe('reopening the dialog', () => {
        it('clears the previous result, the selection and the region', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(SUCCESS_RESULT));
            const user = await openModule();
            await user.click(screen.getByRole('radio', {name: 'ČR'}));
            await user.click(checkbox('Jarní sprint'));
            await user.click(submitButton(1));
            await screen.findByText('Výsledek importu');

            await user.click(screen.getByRole('button', {name: 'Hotovo'}));
            expect(screen.queryByText('Výsledek importu')).not.toBeInTheDocument();

            await user.click(importButton() as HTMLElement);

            expect(screen.queryByText('Výsledek importu')).not.toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).toBeChecked();
            expect(useAuthorizedQuery).toHaveBeenLastCalledWith('/api/oris/events?region=JIHOMORAVSKA');
            expect(checkbox('Jarní sprint')).not.toBeChecked();
            expect(submitButton(0)).toBeDisabled();
        });

        it('clears a submit error', async () => {
            vi.mocked(authorizedFetch).mockRejectedValue(new ApiError(500));
            const user = await openModule();
            await user.click(checkbox('Jarní sprint'));
            await user.click(submitButton(1));
            await screen.findByText('Import se nezdařil. Zkuste to prosím znovu.');

            await user.click(screen.getByRole('button', {name: 'Zrušit'}));
            await user.click(importButton() as HTMLElement);

            expect(screen.queryByText('Import se nezdařil. Zkuste to prosím znovu.')).not.toBeInTheDocument();
        });
    });

    describe('cache invalidation', () => {
        it('refetches the authorized queries after a successful batch import', async () => {
            vi.mocked(authorizedFetch).mockResolvedValue(createMockResponse(SUCCESS_RESULT));
            const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
            const user = await openModule();

            await user.click(checkbox('Jarní sprint'));
            await user.click(submitButton(1));

            await screen.findByText('Výsledek importu');
            expect(invalidate).toHaveBeenCalledWith({queryKey: ['authorized']});
        });
    });
});
