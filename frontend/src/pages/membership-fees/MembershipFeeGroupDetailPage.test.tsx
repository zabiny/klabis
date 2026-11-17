import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/halRouteContext.ts';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeGroupDetailPage} from './MembershipFeeGroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {
    halRouteValue,
    stubFetch,
    ToastProbe,
    createTestQueryClient,
} from '../../__mocks__/halDialogTest.tsx';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

let rulesRoute: HalRouteContextValue;

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({subresourceLinkName, children}: {subresourceLinkName: string; children: React.ReactNode}) => (
        <div data-testid={`subresource-${subresourceLinkName}`}>
            <HalRouteContext.Provider value={rulesRoute}>{children}</HalRouteContext.Provider>
        </div>
    ),
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

type RouteOverrides = Partial<Pick<HalRouteContextValue, 'isLoading' | 'error'>>;

const createMockPageData = (resourceData: HalResponse | null, overrides?: RouteOverrides): HalRouteContextValue => ({
    ...halRouteValue(resourceData, '/administration/membership-fee-groups/group-1'),
    ...overrides,
});

// The rules subresource is a separate HAL route; an empty HAL collection has no `_embedded` block.
const createMockRulesPageData = (): HalRouteContextValue =>
    halRouteValue(
        {_links: {self: {href: '/api/membership-fee-groups/group-1/rules'}}},
        '/administration/membership-fee-groups/group-1/rules',
    );

const renderPage = (pageData: HalRouteContextValue) => {
    rulesRoute = createMockRulesPageData();
    const queryClient = createTestQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/membership-fee-groups/group-1']}>
                <ToastProvider>
                    <HalRouteContext.Provider value={pageData}>
                        <MembershipFeeGroupDetailPage/>
                    </HalRouteContext.Provider>
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
