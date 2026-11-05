import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeGroupDetailPage} from './MembershipFeeGroupDetailPage';
import {vi} from 'vitest';
import {ApiError} from '../../api/apiError';
import type {HalResponse} from '../../api';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {stubFetch, ToastProbe, createTestQueryClient} from '../../__mocks__/halDialogTest.tsx';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

const buildGroupDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'group-1',
    name: 'Základní členství',
    yearlyFeeAmount: 500,
    status: 'EDITABLE',
    _embedded: {
        members: [
            {
                memberId: 'member-1',
                firstName: 'Jan',
                lastName: 'Novák',
                registrationNumber: 'ZBM1234',
                joinedAt: '2025-01-15',
                source: 'MEMBER_CHOICE',
            },
            {
                memberId: 'member-2',
                firstName: 'Petra',
                lastName: 'Svobodová',
                registrationNumber: 'ZBM5678',
                joinedAt: '2025-02-10',
                source: 'ADMIN_ASSIGNMENT',
            },
        ],
    },
    _links: {self: {href: '/api/membership-fee-groups/group-1'}},
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    navigateToResource: vi.fn(),
    refetch: async () => {},
    getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-groups/group-1'}),
    ...overrides,
});

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = createTestQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/membership-fee-groups/group-1']}>
                <ToastProvider>
                    <MembershipFeeGroupDetailPage/>
                    <ToastProbe/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembershipFeeGroupDetailPage', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch());
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows Czech text when the resource is not found', () => {
        renderPage(createMockPageData(null, {error: new ApiError(404, {type: '/errors/not-found', detail: 'Tier 123 not found'})}));
        expect(screen.getByText('Požadovaná položka nebyla nalezena.')).toBeInTheDocument();
        expect(screen.queryByText(/HTTP 404/)).not.toBeInTheDocument();
    });

    it('back link leads to the membership fees page', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByRole('link', {name: /zpět na seznam/i})).toHaveAttribute('href', '/membership-fee-tiers');
    });

    it('renders group name as heading', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByRole('heading', {name: /základní členství/i})).toBeInTheDocument();
    });

    it('renders annual fee snapshot', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText(/500/)).toBeInTheDocument();
    });

    it('renders EDITABLE status as "Editovatelná"', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('Editovatelná')).toBeInTheDocument();
    });

    it('renders FROZEN status as "Zmrazená"', () => {
        renderPage(createMockPageData(buildGroupDetail({status: 'FROZEN'})));
        expect(screen.getByText('Zmrazená')).toBeInTheDocument();
    });

    it('renders members section', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText(/členové/i)).toBeInTheDocument();
    });

    it('renders member full name from embedded members', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('Jan Novák')).toBeInTheDocument();
        expect(screen.getByText('Petra Svobodová')).toBeInTheDocument();
    });

    it('renders member registration number from embedded members', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('ZBM1234')).toBeInTheDocument();
        expect(screen.getByText('ZBM5678')).toBeInTheDocument();
    });

    it('renders MEMBER_CHOICE source as "Vlastní volba"', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('Vlastní volba')).toBeInTheDocument();
    });

    it('renders ADMIN_ASSIGNMENT source as "Přiřazeno adminem"', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('Přiřazeno adminem')).toBeInTheDocument();
    });

    it('shows empty state when no members in embedded', () => {
        const resourceData = buildGroupDetail({_embedded: {members: []}});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/žádní členové ve skupině/i)).toBeInTheDocument();
    });

    it('shows empty state when embedded is absent', () => {
        const resourceData = buildGroupDetail({_embedded: undefined});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/žádní členové ve skupině/i)).toBeInTheDocument();
    });

    it('renders edit snapshot button when editSnapshot template exists and status is EDITABLE', () => {
        const resourceData = buildGroupDetail({
            _templates: {
                editSnapshot: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
    });

    it('renders assign member button when assignMember template exists', () => {
        const resourceData = buildGroupDetail({
            _templates: {
                assignMember: mockHalFormsTemplate({title: 'Přiřadit člena', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přiřadit člena/i})).toBeInTheDocument();
    });

    it('does not render edit button when status is FROZEN', () => {
        const resourceData = buildGroupDetail({
            status: 'FROZEN',
            _templates: {
                editSnapshot: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /upravit/i})).not.toBeInTheDocument();
    });

    describe('assign member dialog', () => {
        const assignMemberGroup = () => buildGroupDetail({
            _templates: {
                assignMember: mockHalFormsTemplate({
                    title: 'Přiřadit člena',
                    method: 'POST',
                    target: '/api/membership-fee-groups/group-1/members',
                    properties: [],
                }),
            },
        });

        it('opens the dialog with the assign member title', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(assignMemberGroup()));

            await user.click(screen.getByRole('button', {name: /přiřadit člena/i}));

            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přiřadit člena');
        });

        it('submits to the template target, shows the saved toast and closes the dialog', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(assignMemberGroup()));

            await user.click(screen.getByRole('button', {name: /přiřadit člena/i}));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/membership-fee-groups/group-1/members');
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByTestId('toast')).toHaveTextContent('Úspěšně uloženo');
        });

        it('closes without a request when cancelled', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(assignMemberGroup()));

            await user.click(screen.getByRole('button', {name: /přiřadit člena/i}));
            await user.click(await screen.findByRole('button', {name: /zrušit/i}));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(writeCalls()).toHaveLength(0);
        });
    });
});
