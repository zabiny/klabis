import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {type QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {type ReactElement} from 'react';
import {vi} from 'vitest';
import {
    type TemplateDialogOptions,
    type TemplateDialogOverrides,
    useTemplateDialog,
} from './useTemplateDialog.tsx';
import {HalRouteContext} from '../../contexts/halRouteContext.ts';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {createTestQueryClient, halRouteValue, LocationProbe, stubFetch, ToastProbe} from '../../__mocks__/halDialogTest.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {type HalTemplateAction, halResource} from '../../api/halResource';
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

const actionFor = (
    templates: Record<string, HalFormsTemplate>,
    name: string,
    self?: string,
): HalTemplateAction => {
    const action = halResource(resourceWith(templates, self)).template(name);
    if (!action) throw new Error(`template ${name} is missing from the fixture`);
    return action;
};

interface HarnessProps {
    action?: HalTemplateAction;
    overrides?: TemplateDialogOverrides;
    options?: TemplateDialogOptions;
}

const Harness = ({action, overrides, options}: HarnessProps): ReactElement => {
    const d = useTemplateDialog(options);
    return (
        <div>
            <button onClick={() => d.open(action, overrides)}>open</button>
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

    describe('opening', () => {
        it('is not rendered before open()', () => {
            renderHarness({action: actionFor({publishEvent: template()}, 'publishEvent')});
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('does not open when the action is undefined', async () => {
            const user = userEvent.setup();
            renderHarness({action: undefined});
            await user.click(screen.getByText('open'));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    describe('submit URL', () => {
        const submit = async (user: ReturnType<typeof userEvent.setup>) => {
            await screen.findByRole('button', {name: /odeslat/i});
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
        };

        it('submits to the template target carried by the action', async () => {
            const user = userEvent.setup();
            const res = {publishEvent: template({target: '/api/target-url'})};
            renderHarness({action: actionFor(res, 'publishEvent', '/api/self-url')});
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/target-url');
        });

        it('submits to the resource self link when the template has no target', async () => {
            const user = userEvent.setup();
            const res = {publishEvent: template({target: undefined})};
            renderHarness({action: actionFor(res, 'publishEvent', 'https://localhost:8443/api/rows/7')});
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/rows/7');
        });

        it('falls back to the route pathname when the action has no href', async () => {
            const user = userEvent.setup();
            const res = {publishEvent: template({target: undefined})};
            renderHarness({action: actionFor(res, 'publishEvent')}, '/members/42');
            await user.click(screen.getByText('open'));
            await submit(user);
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/members/42');
        });
    });

    describe('dialog title', () => {
        it('defaults to the action dialog title', async () => {
            const user = userEvent.setup();
            renderHarness({action: actionFor({publishEvent: template()}, 'publishEvent')});
            await user.click(screen.getByText('open'));
            expect(await screen.findByRole('dialog')).toHaveTextContent('Publikování akce');
        });

        it('falls back to the action label, then the template title', async () => {
            const user = userEvent.setup();
            const {unmount} = renderHarness({action: actionFor({updateEvent: template()}, 'updateEvent')});
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Upravit');
            unmount();

            renderHarness({action: actionFor({customThing: template({title: 'Custom title'})}, 'customThing')});
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Custom title');
        });

        it('explicit string title wins over the action dialog title', async () => {
            const user = userEvent.setup();
            renderHarness({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {title: 'Moje'},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Moje');
        });

        it('title callback receives the template and its name', async () => {
            const user = userEvent.setup();
            renderHarness({
                action: actionFor({publishEvent: template({title: 'T1'})}, 'publishEvent'),
                options: {title: (t, n) => `Vlastní: ${t.title} ${n}`},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Vlastní: T1 publishEvent');
        });

        it('per-open title override wins over the option and the action', async () => {
            const user = userEvent.setup();
            renderHarness({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {title: 'Základ'},
                overrides: {title: 'Varianta'},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Varianta');
        });

        it('renders the option note under the title', async () => {
            const user = userEvent.setup();
            renderHarness({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {note: 'Opravdu?'},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByText('Opravdu?')).toBeInTheDocument();
        });
    });

    describe('overrides and initialData', () => {
        it('per-open overrides apply to one opening only', async () => {
            const user = userEvent.setup();
            const onSubmitSuccess = vi.fn();
            const action = actionFor({deleteRow: template({title: 'Delete row'})}, 'deleteRow');
            const Probe = () => {
                const d = useTemplateDialog({title: 'Základ'});
                return (
                    <div>
                        <button onClick={() => d.open(action, {title: 'Varianta', note: 'Pozn', onSubmitSuccess})}>variant</button>
                        <button onClick={() => d.open(action)}>plain</button>
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

        it('initialData replaces the prefill while the URL still comes from the action', async () => {
            const user = userEvent.setup();
            const res = {
                setGuardians: template({
                    method: 'PUT',
                    target: undefined,
                    properties: [{name: 'legalGuardians', prompt: 'Zástupci', type: 'text'}],
                }),
            };
            renderHarness({
                action: actionFor(res, 'setGuardians', '/api/members/5'),
                options: {initialData: {legalGuardians: 'u-1'}},
            });
            await user.click(screen.getByText('open'));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/members/5');
            expect(JSON.parse((writeCalls()[0][1] as RequestInit).body as string)).toMatchObject({legalGuardians: 'u-1'});
        });

        it('per-open initialData wins over the option', async () => {
            const user = userEvent.setup();
            const res = {
                editRow: template({
                    method: 'PATCH',
                    target: undefined,
                    properties: [{name: 'reason', prompt: 'Důvod', type: 'text'}],
                }),
            };
            renderHarness({
                action: actionFor(res, 'editRow', '/api/rows/3'),
                options: {initialData: {reason: 'z-option'}},
                overrides: {initialData: {reason: 'z-row'}},
            });
            await user.click(screen.getByText('open'));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(JSON.parse((writeCalls()[0][1] as RequestInit).body as string)).toMatchObject({reason: 'z-row'});
        });
    });

    describe('late-bound action', () => {
        it('opens whichever action is passed to open()', async () => {
            const user = userEvent.setup();
            const first = actionFor({deleteRow: template({title: 'Delete row', target: '/api/rows/3'})}, 'deleteRow');
            const second = actionFor({renameRow: template({title: 'Rename row', target: '/api/rows/4'})}, 'renameRow');
            const Probe = () => {
                const d = useTemplateDialog();
                return (
                    <div>
                        <button onClick={() => d.open(first)}>first</button>
                        <button onClick={() => d.open(second)}>second</button>
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

            await user.click(screen.getByText('first'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Delete row');
            await user.click(screen.getByRole('button', {name: /zrušit/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

            await user.click(screen.getByText('second'));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Rename row');
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
            const user = await open({action: actionFor({publishEvent: template()}, 'publishEvent')});
            await user.click(screen.getByRole('button', {name: /zrušit/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(writeCalls()).toHaveLength(0);
        });

        it('successful submit shows the action success message, closes and calls onSubmitSuccess', async () => {
            const onSubmitSuccess = vi.fn();
            const user = await open({
                action: actionFor({publishEvent: template({title: 'Publikovat'})}, 'publishEvent'),
                options: {onSubmitSuccess},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByTestId('toast')).toHaveTextContent('Publikovat — úspěšně uloženo');
            expect(onSubmitSuccess).toHaveBeenCalledTimes(1);
        });

        it('uses a custom successMessage option', async () => {
            const user = await open({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {successMessage: 'Hotovo'},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Hotovo'));
        });

        it('per-open successMessage override wins over the option and the action', async () => {
            const user = await open({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {successMessage: 'Hotovo'},
                overrides: {successMessage: 'Ještě lépe'},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Ještě lépe'));
        });

        it('uses a custom submitLabel', async () => {
            const user = userEvent.setup();
            renderHarness({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {submitLabel: 'Potvrdit'},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByRole('button', {name: 'Potvrdit'})).toBeInTheDocument();
        });

        it('applies postprocessPayload', async () => {
            const user = await open({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {postprocessPayload: p => ({...p, extra: 1})},
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
            const user = await open({action: actionFor({publishEvent: template()}, 'publishEvent')});
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
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {navigateOnSuccess: false},
            });
            await user.click(screen.getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByTestId('location')).toHaveTextContent('/members/1');
        });

        it('per-open navigateOnSuccess override wins over the option', async () => {
            fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
                if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
                return createMockResponse(null, 201, {Location: '/api/members/99'});
            });
            const user = await open({
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {navigateOnSuccess: true},
                overrides: {navigateOnSuccess: false},
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
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {onSubmitError},
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
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {onSubmitError},
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
                action: actionFor({publishEvent: template()}, 'publishEvent'),
                options: {fieldsFactory},
            });
            await user.click(screen.getByText('open'));
            expect(await screen.findByTestId('custom-field')).toBeInTheDocument();
        });
    });

    describe('portal', () => {
        it('renders the dialog into document.body, outside the caller tree', async () => {
            const user = userEvent.setup();
            const {container} = renderHarness({action: actionFor({publishEvent: template()}, 'publishEvent')});
            await user.click(screen.getByText('open'));
            const dialog = await screen.findByRole('dialog');
            expect(container.contains(dialog)).toBe(false);
            expect(document.body.contains(dialog)).toBe(true);
        });
    });
});
