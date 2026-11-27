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
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {
    blocked409,
    blockingTrainingGroup,
    installSuspendMutation,
    SUSPENSION_GROUP_URL,
    trainingGroupResource,
    type SuspendOutcome,
} from '../../__mocks__/suspensionFlowTest';
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
    const submitButton = /uložit|odeslat|potvrdit/i;

    const dialogTitled = (title: string) =>
        screen.getAllByRole('dialog').find(d => within(d).queryAllByRole('heading', {name: title}).length > 0)!;

    const setup = (suspendResponses: SuspendOutcome[]) => {
        const {calls} = installSuspendMutation(SUSPEND_URL, suspendResponses);
        vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined) => ({
            data: url === SUSPENSION_GROUP_URL ? trainingGroupResource() : undefined,
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

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('409 closes the suspension form, lists the blocking group and, once resolved, resubmits the stored request', async () => {
        const user = userEvent.setup();
        const {calls} = setup([{error: blocked409({affectedGroups: [blockingTrainingGroup]})}, {}]);
        await user.click(screen.getByRole('button', {name: labels.templates.suspendMember}));
        await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: submitButton}));

        await screen.findByRole('heading', {name: labels.suspensionWarning.title});
        expect(screen.getAllByRole('dialog')).toHaveLength(1);
        expect(within(dialogTitled(labels.suspensionWarning.title)).getByText('U10 Závoďáci')).toBeInTheDocument();

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
});
