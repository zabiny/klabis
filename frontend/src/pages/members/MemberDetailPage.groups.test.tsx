import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {MemberDetailPage} from './MemberDetailPage';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {makePageData} from '../../__mocks__/pageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {ApiError} from '../../api/authorizedFetch';
import {labels} from '../../localization';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(() => ({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        error: null,
    })),
    useAuthorizedQuery: vi.fn(() => ({
        data: undefined,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
        status: 'success',
    })),
}));

vi.mock('../../hooks/useHalFormGetAvailability', () => ({
    useHalFormGetAvailability: vi.fn(() => ({isGetAllowed: false, isLoading: false, error: null})),
}));

vi.mock('../../contexts/ToastContext', () => ({
    ToastProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/toastContext', () => ({
    useToast: vi.fn(() => ({
        addToast: vi.fn(),
    })),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/hateoas', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../api/hateoas')>()),
    submitHalFormsData: vi.fn(),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: {isOpen: boolean; children: React.ReactNode; onClose: () => void; title?: string}) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {title && <h4>{title}</h4>}
                {children}
                <button onClick={onClose}>Close</button>
            </div>
        ) : null
    ),
}));

const adminEditTemplate: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno'},
        {name: 'lastName', type: 'text', prompt: 'Příjmení'},
        {name: 'email', type: 'email', prompt: 'E-mail'},
    ],
};


const mockMemberDetailData = (overrides?: Partial<HalResponse & Record<string, unknown>>): HalResponse => ({
    id: '123e4567-e89b-12d3-a456-426614174000',
    registrationNumber: 'SKI2601',
    firstName: 'Jan',
    lastName: 'Novák',
    dateOfBirth: '1990-03-15',
    nationality: 'CZ',
    gender: 'MALE',
    email: 'jan.novak@email.cz',
    phone: '+420777123456',
    address: {street: 'Hlavní 15', city: 'Praha', postalCode: '11000', country: 'CZ'},
    active: true,
    _links: {
        self: {href: '/api/members/123e4567-e89b-12d3-a456-426614174000'},
    },
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides: Partial<ReturnType<typeof makePageData>> = {}) =>
    makePageData({
        resourceData,
        getResourceLink: vi.fn().mockImplementation((rel: string) =>
            resourceData?._links?.[rel] ?? null
        ),
        ...overrides,
    });

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members/123e4567-e89b-12d3-a456-426614174000']}>
                <HalFormProvider>
                    <HalFormsPageLayout>
                        <MemberDetailPage/>
                    </HalFormsPageLayout>
                </HalFormProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MemberDetailPage — group navigation buttons', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders "Tréninková skupina" button when trainingGroup link is present', () => {
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: {href: '/api/training-groups/tg-uuid'},
            },
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /tréninková skupina/i})).toBeInTheDocument();
    });

    it('clicking "Tréninková skupina" button calls navigateToResource with the trainingGroup link', async () => {
        const user = userEvent.setup();
        const navigateToResource = vi.fn();
        const trainingGroupLink = {href: '/api/training-groups/tg-uuid'};
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: trainingGroupLink,
            },
            _templates: {updateMember: adminEditTemplate},
        });
        const pageData = createMockPageData(data, {
            navigateToResource,
            refetch: async () => {},
            getResourceLink: vi.fn().mockImplementation((rel: string) =>
                rel === 'trainingGroup' ? trainingGroupLink : null
            ),
        });
        renderPage(pageData);

        await user.click(screen.getByRole('button', {name: /tréninková skupina/i}));

        expect(navigateToResource).toHaveBeenCalledWith(trainingGroupLink);
    });

    it('renders "Zákonní zástupci" button when legalGuardianGroup link is present', () => {
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                legalGuardianGroup: {href: '/api/legal-guardian-groups/lg-uuid'},
            },
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /zákonní zástupci/i})).toBeInTheDocument();
    });

    it('clicking "Zákonní zástupci" button calls navigateToResource with the legalGuardianGroup link', async () => {
        const user = userEvent.setup();
        const navigateToResource = vi.fn();
        const legalGuardianGroupLink = {href: '/api/legal-guardian-groups/lg-uuid'};
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                legalGuardianGroup: legalGuardianGroupLink,
            },
            _templates: {updateMember: adminEditTemplate},
        });
        const pageData = createMockPageData(data, {
            navigateToResource,
            refetch: async () => {},
            getResourceLink: vi.fn().mockImplementation((rel: string) =>
                rel === 'legalGuardianGroup' ? legalGuardianGroupLink : null
            ),
        });
        renderPage(pageData);

        await user.click(screen.getByRole('button', {name: /zákonní zástupci/i}));

        expect(navigateToResource).toHaveBeenCalledWith(legalGuardianGroupLink);
    });

    it('renders neither group button when both links are absent', () => {
        const data = mockMemberDetailData({
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.queryByRole('button', {name: /tréninková skupina/i})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /zákonní zástupci/i})).not.toBeInTheDocument();
    });

    it('renders group navigation buttons even without an edit template (other-member view)', () => {
        // The backend emits trainingGroup/legalGuardianGroup links for any reader of the member detail,
        // regardless of whether the reader has edit permissions. Buttons must render on HAL link
        // presence alone — no _templates.updateMember required.
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: {href: '/api/training-groups/tg-uuid'},
                legalGuardianGroup: {href: '/api/legal-guardian-groups/lg-uuid'},
            },
            // deliberately no _templates
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /tréninková skupina/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /zákonní zástupci/i})).toBeInTheDocument();
    });

});

describe('MemberDetailPage - suspension warning dialog', () => {
    const SUSPEND_URL = '/api/members/m1/suspend';
    const GROUP_URL = 'http://localhost/api/training-groups/g-1';
    const trainingGroup = {groupId: 'g-1', groupName: 'U10 Závoďáci', groupType: 'TRAINING', groupLink: GROUP_URL};
    const debt = {balance: {amount: -350, currency: 'CZK'}, accountLink: '/api/members/m1/account'};
    const blocked409 = (groups: unknown, debtBody: unknown = null) => new ApiError(409, {params: {groups, debt: debtBody}});
    const submitButton = /uložit|odeslat|potvrdit/i;

    const dialogTitled = (title: string) =>
        screen.getAllByRole('dialog').find(d => within(d).queryAllByRole('heading', {name: title}).length > 0)!;

    const setup = (suspendResponses: Array<{error?: unknown}>) => {
        const calls: Array<{url: string; data: unknown}> = [];
        let suspendCall = 0;
        const mutate = vi.fn((vars: {url: string; data: unknown}, opts: {onError?: (e: unknown) => void; onSuccess?: (r: unknown) => void}) => {
            calls.push(vars);
            const outcome = vars.url === SUSPEND_URL ? suspendResponses[suspendCall++] ?? {} : {};
            if (outcome.error) opts.onError?.(outcome.error);
            else opts.onSuccess?.({data: null, location: null});
        });
        vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false, error: null} as unknown as ReturnType<typeof useAuthorizedMutation>);
        vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined) => ({
            data: url === GROUP_URL ? {
                _links: {self: {href: GROUP_URL}},
                _templates: {addTrainer: mockHalFormsTemplate({method: 'POST', target: '/api/training-groups/g-1/trainers', title: 'Přidat trenéra', properties: []})},
            } : undefined,
            isLoading: false,
            error: null,
            refetch: vi.fn(),
            status: 'success',
        })) as unknown as typeof useAuthorizedQuery);

        const data = mockMemberDetailData({
            _templates: {
                updateMember: adminEditTemplate,
                suspendMember: mockHalFormsTemplate({method: 'POST', title: 'Terminate', target: SUSPEND_URL, properties: []}),
            },
        });
        renderPage(createMockPageData(data));
        return {calls};
    };

    const openSuspensionWarning = async (user: ReturnType<typeof userEvent.setup>) => {
        await user.click(screen.getByRole('button', {name: labels.templates.suspendMember}));
        await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: submitButton}));
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows debt and groups in one dialog and closes the suspension form', async () => {
        const user = userEvent.setup();
        setup([{error: blocked409({affectedGroups: [trainingGroup]}, debt)}]);
        await openSuspensionWarning(user);

        expect(await screen.findByRole('heading', {name: labels.suspensionWarning.title})).toBeInTheDocument();
        expect(screen.getAllByRole('dialog')).toHaveLength(1);
        const dialog = dialogTitled(labels.suspensionWarning.title);
        expect(within(dialog).getByText('U10 Závoďáci')).toBeInTheDocument();
        expect(within(dialog).getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
    });

    it('debt-only 409 shows the balance and the account link', async () => {
        const user = userEvent.setup();
        setup([{error: blocked409(null, debt)}]);
        await openSuspensionWarning(user);

        const dialog = dialogTitled(labels.negativeBalanceWarning.title);
        expect(within(dialog).getByText(/350/)).toBeInTheDocument();
        expect(within(dialog).getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
    });

    it('resolving the group enables "Ukončit členství" that resubmits the stored request', async () => {
        const user = userEvent.setup();
        const {calls} = setup([{error: blocked409({affectedGroups: [trainingGroup]})}, {}]);
        await openSuspensionWarning(user);
        await screen.findByRole('heading', {name: labels.suspensionWarning.title});

        await user.click(within(dialogTitled(labels.suspensionWarning.title)).getByRole('button', {name: labels.suspensionWarning.designateSuccessor}));
        await user.click(within(dialogTitled('Přidat trenéra')).getByRole('button', {name: submitButton}));

        const warning = () => dialogTitled(labels.suspensionWarning.title);
        await waitFor(() => expect(within(warning()).getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument());
        await user.click(within(warning()).getByRole('button', {name: labels.suspensionWarning.finishSuspension}));

        const suspendCalls = calls.filter(c => c.url === SUSPEND_URL);
        expect(suspendCalls).toHaveLength(2);
        expect(suspendCalls[1]).toEqual(suspendCalls[0]);
        await waitFor(() => expect(screen.queryByRole('heading', {name: labels.suspensionWarning.title})).not.toBeInTheDocument());
    });

    it('resolved group with debt offers no "Ukončit členství"', async () => {
        const user = userEvent.setup();
        setup([{error: blocked409({affectedGroups: [trainingGroup]}, debt)}]);
        await openSuspensionWarning(user);
        await screen.findByRole('heading', {name: labels.suspensionWarning.title});

        await user.click(within(dialogTitled(labels.suspensionWarning.title)).getByRole('button', {name: labels.suspensionWarning.designateSuccessor}));
        await user.click(within(dialogTitled('Přidat trenéra')).getByRole('button', {name: submitButton}));

        const warning = () => dialogTitled(labels.suspensionWarning.title);
        await waitFor(() => expect(within(warning()).getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument());
        expect(within(warning()).queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();
        expect(within(warning()).getByText(labels.negativeBalanceWarning.description)).toBeInTheDocument();
    });
});
