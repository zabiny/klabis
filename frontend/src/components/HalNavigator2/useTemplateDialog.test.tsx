import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {type QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {type ReactElement, useRef} from 'react';
import {vi} from 'vitest';
import {useTemplateDialog, type TemplateDialogOptions} from './useTemplateDialog.tsx';
import {HalRouteContext} from '../../contexts/halRouteContext.ts';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {createTestQueryClient, halRouteValue, LocationProbe, stubFetch, ToastProbe} from '../../__mocks__/halDialogTest.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import type {HalFormsTemplate, HalResponse} from '../../api';
import type {HalFormFieldFactory} from './halforms';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

const template = (overrides: Partial<HalFormsTemplate> = {}): HalFormsTemplate => mockHalFormsTemplate({
    method: 'POST',
    target: '/api/members/1/publish',
    title: 'Template title',
    properties: [{name: 'reason', prompt: 'Důvod', type: 'text'}],
    ...overrides,
});

const resourceWith = (templates: Record<string, HalFormsTemplate>, self?: string): HalResponse => ({
    _links: self ? {self: {href: self}} : {},
    _templates: templates,
} as HalResponse);

interface HarnessProps {
    name?: string;
    options?: TemplateDialogOptions;
    row?: HalResponse;
    rowTemplateName?: string;
    onApi?: (api: ReturnType<typeof useTemplateDialog>) => void;
}

const Harness = ({name, options, row, rowTemplateName, onApi}: HarnessProps): ReactElement => {
    const d = useTemplateDialog(name, options);
    onApi?.(d);
    const lastApi = useRef(d);
    lastApi.current = d;
    return (
        <div>
            <span data-testid="available">{String(d.available)}</span>
            {row && rowTemplateName && (
                <span data-testid="available-on-row">{String(d.availableOn(row, rowTemplateName))}</span>
            )}
            <button onClick={() => d.open()}>open</button>
            {row && <button onClick={() => d.open(row, rowTemplateName)}>open-row</button>}
            {d.dialog}
        </div>
    );
};

describe('useTemplateDialog', () => {
    let fetchSpy: ReturnType<typeof stubFetch>['fetchSpy'];
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = createTestQueryClient();
        ({fetchSpy, writeCalls} = stubFetch());
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    const renderHarness = (props: HarnessProps, pathname = '/members/1') =>
        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[pathname]}>
                    <HalRouteContext.Provider value={halRouteValue(null, pathname)}>
                        <ToastProvider>
                            <Harness {...props}/>
                            <ToastProbe/>
                            <LocationProbe/>
                        </ToastProvider>
                    </HalRouteContext.Provider>
                </MemoryRouter>
            </QueryClientProvider>
        );

    describe('availability', () => {
        it('is available when the resource has the named template', () => {
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            expect(screen.getByTestId('available')).toHaveTextContent('true');
        });

        it('is unavailable when the resource lacks the template', () => {
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({})}});
            expect(screen.getByTestId('available')).toHaveTextContent('false');
        });

        it('is unavailable without a resource', () => {
            renderHarness({name: 'publishEvent'});
            expect(screen.getByTestId('available')).toHaveTextContent('false');
        });

        it('is unavailable without a name', () => {
            renderHarness({options: {resource: resourceWith({publishEvent: template()})}});
            expect(screen.getByTestId('available')).toHaveTextContent('false');
        });

        it('availableOn checks a given resource and template name', () => {
            const row = resourceWith({editRow: template()});
            renderHarness({row, rowTemplateName: 'editRow'});
            expect(screen.getByTestId('available-on-row')).toHaveTextContent('true');
        });

        it('availableOn is false when the row lacks the template', () => {
            const row = resourceWith({});
            renderHarness({row, rowTemplateName: 'editRow'});
            expect(screen.getByTestId('available-on-row')).toHaveTextContent('false');
        });
    });

    describe('dialog title', () => {
        it('is not rendered before open()', () => {
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('defaults to the localized dialog title', async () => {
            const user = userEvent.setup();
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByRole('dialog')).toHaveTextContent('Publikování akce');
        });

        it('falls back to the localized template label, then template title', async () => {
            const user = userEvent.setup();
            const {unmount} = renderHarness({name: 'updateEvent', options: {resource: resourceWith({updateEvent: template()})}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Upravit');
            unmount();

            renderHarness({name: 'customThing', options: {resource: resourceWith({customThing: template({title: 'Custom title'})})}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Custom title');
        });

        it('explicit string title wins', async () => {
            const user = userEvent.setup();
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()}), title: 'Moje'}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Moje');
        });

        it('title callback receives the template and its name', async () => {
            const user = userEvent.setup();
            renderHarness({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template({title: 'T1'})}), title: (t, n) => `Vlastní: ${t.title} ${n}`},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Vlastní: T1 publishEvent');
        });

        it('renders the note under the title', async () => {
            const user = userEvent.setup();
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()}), note: 'Opravdu?'}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByText('Opravdu?')).toBeInTheDocument();
        });
    });

    describe('overrides and initialData', () => {
        it('overrides apply to one opening only', async () => {
            const user = userEvent.setup();
            const onSubmitSuccess = vi.fn();
            const row = resourceWith({deleteRow: template({title: 'Delete row'})});
            const Probe = () => {
                const d = useTemplateDialog('deleteRow', {title: 'Základ'});
                return (
                    <div>
                        <button onClick={() => d.open(row, undefined, {title: 'Varianta', note: 'Pozn', onSubmitSuccess})}>variant</button>
                        <button onClick={() => d.open(row)}>plain</button>
                        {d.dialog}
                    </div>
                );
            };
            render(
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter>
                        <HalRouteContext.Provider value={halRouteValue(null, '/members/1')}>
                            <ToastProvider><Probe/></ToastProvider>
                        </HalRouteContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            );

            await user.click(screen.getByText('variant'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Varianta');
            expect(screen.getByText('Pozn')).toBeInTheDocument();
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(onSubmitSuccess).toHaveBeenCalledTimes(1));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

            await user.click(screen.getByText('plain'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Základ');
            expect(screen.queryByText('Pozn')).not.toBeInTheDocument();
        });

        it('initialData replaces the prefill while the URL still comes from the resource', async () => {
            const user = userEvent.setup();
            const res = resourceWith({
                setGuardians: template({
                    method: 'PUT',
                    target: undefined,
                    properties: [{name: 'legalGuardians', prompt: 'Zástupci', type: 'text'}],
                }),
            }, '/api/members/5');
            renderHarness({
                name: 'setGuardians',
                options: {resource: res, initialData: {legalGuardians: 'u-1'}},
            });
            await user.click(screen.getByText('open'));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/members/5');
            expect(JSON.parse((writeCalls()[0][1] as RequestInit).body as string)).toMatchObject({legalGuardians: 'u-1'});
        });
    });

    describe('submit URL', () => {
        const submit = async (user: ReturnType<typeof userEvent.setup>) => {
            await screen.findByRole('button', {name: /odeslat/i});
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
        };

        it('uses template.target first', async () => {
            const user = userEvent.setup();
            const res = resourceWith({publishEvent: template({target: '/api/target-url'})}, '/api/self-url');
            renderHarness({name: 'publishEvent', options: {resource: res}});
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/target-url');
        });

        it('uses the self link path when the template has no target', async () => {
            const user = userEvent.setup();
            const res = resourceWith({publishEvent: template({target: undefined})}, 'https://localhost:8443/api/rows/7');
            renderHarness({name: 'publishEvent', options: {resource: res}});
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/rows/7');
        });

        it('falls back to the route pathname without target and self link', async () => {
            const user = userEvent.setup();
            const res = resourceWith({publishEvent: template({target: undefined})});
            renderHarness({name: 'publishEvent', options: {resource: res}}, '/members/42');
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/members/42');
        });
    });

    describe('late-bound name', () => {
        it('open(row, name) opens the row template', async () => {
            const user = userEvent.setup();
            const row = resourceWith({deleteRow: template({title: 'Delete row', target: '/api/rows/3'})}, '/api/rows/3');
            renderHarness({row, rowTemplateName: 'deleteRow'});
            await user.click(screen.getByText('open-row'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Delete row');
        });

        it('open() does nothing when the template is missing', async () => {
            const user = userEvent.setup();
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({})}});
            await user.click(screen.getByText('open'));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    describe('closing and submitting', () => {
        const open = async (props: HarnessProps, pathname?: string) => {
            const user = userEvent.setup();
            renderHarness(props, pathname);
            await user.click(screen.getByText('open'));
            await screen.findByRole('button', {name: /odeslat/i});
            return user;
        };

        it('cancel closes the dialog without a request', async () => {
            const user = await open({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            await user.click(screen.getByRole('button', {name: /zrušit/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(writeCalls()).toHaveLength(0);
        });

        it('successful submit shows the default toast, closes and calls onSubmitSuccess', async () => {
            const onSubmitSuccess = vi.fn();
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template({title: 'Publikovat'})}), onSubmitSuccess},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByTestId('toast')).toHaveTextContent('Publikovat — úspěšně uloženo');
            expect(onSubmitSuccess).toHaveBeenCalledTimes(1);
        });

        it('uses a custom successMessage', async () => {
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), successMessage: 'Hotovo'},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Hotovo'));
        });

        it('uses a custom submitLabel', async () => {
            const user = userEvent.setup();
            renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()}), submitLabel: 'Potvrdit'}});
            await user.click(screen.getByText('open'));
            expect(await screen.findByRole('button', {name: 'Potvrdit'})).toBeInTheDocument();
        });

        it('applies postprocessPayload', async () => {
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), postprocessPayload: p => ({...p, extra: 1})},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(JSON.parse((writeCalls()[0][1] as RequestInit).body as string)).toMatchObject({extra: 1});
        });

        it('navigates to Location after POST when navigateOnSuccess is on', async () => {
            fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
                if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
                return createMockResponse(null, 201, {Location: '/api/members/99'});
            });
            const user = await open({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/members/99'));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        });

        it('does not navigate when navigateOnSuccess is false and just closes', async () => {
            fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
                if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
                return createMockResponse(null, 201, {Location: '/api/members/99'});
            });
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), navigateOnSuccess: false},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByTestId('location')).toHaveTextContent('/members/1');
        });

        it('onSubmitError returning true closes the dialog', async () => {
            fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
                if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
                return createMockResponse({}, 500);
            });
            const onSubmitError = vi.fn(() => true);
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), onSubmitError},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(onSubmitError).toHaveBeenCalledTimes(1);
        });

        it('onSubmitError returning nothing keeps the dialog open', async () => {
            fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
                if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
                return createMockResponse({}, 500);
            });
            const onSubmitError = vi.fn();
            const user = await open({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), onSubmitError},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(onSubmitError).toHaveBeenCalled());
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });
    });

    describe('fieldsFactory', () => {
        it('is used to render fields', async () => {
            const user = userEvent.setup();
            const fieldsFactory: HalFormFieldFactory = vi.fn(() => <span data-testid="custom-field">custom</span>);
            renderHarness({
                name: 'publishEvent',
                options: {resource: resourceWith({publishEvent: template()}), fieldsFactory},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('custom-field')).toBeInTheDocument();
        });
    });

    describe('portal', () => {
        it('renders the dialog into document.body, outside the caller tree', async () => {
            const user = userEvent.setup();
            const {container} = renderHarness({name: 'publishEvent', options: {resource: resourceWith({publishEvent: template()})}});
            await user.click(screen.getByText('open'));
            const dialog = await screen.findByRole('dialog');
            expect(container.contains(dialog)).toBe(false);
            expect(document.body.contains(dialog)).toBe(true);
        });
    });
});
