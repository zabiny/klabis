import '@testing-library/jest-dom';
import {render as rtlRender, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import type {ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {useLegalGuardians} from '../../hooks/useLegalGuardians.ts';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {useToast} from '../../contexts/toastContext.ts';
import {stubFetch} from '../../__mocks__/halDialogTest.ts';
import {LegalGuardiansSection} from './LegalGuardiansSection';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

const loadedGuardians = [{userId: 'u-1', firstName: 'Jana', lastName: 'Nováková', _links: {member: {href: '/api/members/u-1'}}}];
vi.mock('../../hooks/useLegalGuardians.ts', () => ({useLegalGuardians: vi.fn()}));

const ToastProbe = () => {
    const {toasts} = useToast();
    return <ul>{toasts.map(t => <li key={t.id} data-testid="toast">{t.message}</li>)}</ul>;
};

const render = (ui: ReactElement) => rtlRender(
    <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}})}>
        <MemoryRouter>
            <ToastProvider>
                {ui}
                <ToastProbe/>
            </ToastProvider>
        </MemoryRouter>
    </QueryClientProvider>
);

const navigateToResource = vi.fn();
const refetch = vi.fn();

let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

afterEach(() => {
    vi.unstubAllGlobals();
});

beforeEach(() => {
    vi.clearAllMocks();
    ({writeCalls} = stubFetch());
    vi.mocked(useLegalGuardians).mockReturnValue({guardians: loadedGuardians, isLoading: false});
    vi.mocked(useHalPageData).mockReturnValue({
        route: {pathname: '/members/m-1', navigateToResource, refetch},
    } as unknown as ReturnType<typeof useHalPageData>);
});

describe('LegalGuardiansSection', () => {
    it('offers no edit action without the template', () => {
        render(<LegalGuardiansSection guardiansLink={{href: '/api/x/guardians'}}/>);
        expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
    });

    it('opens the edit dialog, submits the current guardians to the template target and shows the saved toast', async () => {
        const user = userEvent.setup();
        const onEditStarted = vi.fn();
        render(<LegalGuardiansSection
            guardiansLink={{href: '/api/x/guardians'}}
            editTemplate={mockHalFormsTemplate({
                method: 'PUT',
                target: '/api/x/legal-guardians',
                properties: [],
            })}
            editTemplateName="setMemberLegalGuardians"
            onEditStarted={onEditStarted}
        />);

        await user.click(screen.getByRole('button', {name: 'Upravit zástupce'}));

        expect(onEditStarted).toHaveBeenCalled();
        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Upravit zástupce');
        await user.click(await screen.findByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/x/legal-guardians');
        expect((writeCalls()[0][1] as RequestInit).method).toBe('PUT');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getByTestId('toast')).toHaveTextContent('Úspěšně uloženo');
    });

    it('disables the edit action until the guardians are loaded', () => {
        vi.mocked(useLegalGuardians).mockReturnValue({guardians: [], isLoading: true});
        render(<LegalGuardiansSection
            guardiansLink={{href: '/api/x/guardians'}}
            editTemplate={mockHalFormsTemplate({method: 'PUT', target: '/api/x/legal-guardians'})}
            editTemplateName="setMemberLegalGuardians"
        />);
        expect(screen.getByRole('button', {name: 'Upravit zástupce'})).toBeDisabled();
    });

    it('navigates to the guardian resource on click', async () => {
        render(<LegalGuardiansSection guardiansLink={{href: '/api/x/guardians'}}/>);
        await userEvent.click(screen.getByRole('button', {name: 'Jana Nováková'}));
        expect(navigateToResource).toHaveBeenCalledWith({href: '/api/members/u-1'});
    });
});
