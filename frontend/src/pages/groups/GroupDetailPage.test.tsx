import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: () => <div data-testid="hal-form-display"/>,
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
            isOpen ? <div data-testid="modal-overlay" data-title={title}>{children}</div> : null,
    };
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9500', _links: {self: {href: '/api/members/member-1'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/groups/group-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups/group-1'}),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => false),
    getEmbeddedItems: vi.fn(() => []),
    isCollection: vi.fn(() => false),
    hasLink: vi.fn(() => false),
    hasTemplate: vi.fn(() => false),
    hasForms: vi.fn(() => false),
    getPageMetadata: vi.fn(() => undefined),
    ...overrides,
});

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/groups/group-1']}>
                <GroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [],
    members: [],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const buildOwner = (overrides?: Record<string, unknown>) => ({
    memberId: 'owner-1',
    _links: {member: {href: '/api/members/owner-1'}},
    ...overrides,
});

const buildMember = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    joinedAt: '2025-01-15T00:00:00Z',
    _links: {
        self: {href: '/api/groups/group-1/members/member-1'},
        member: {href: '/api/members/member-1'},
    },
    ...overrides,
});

const buildPendingInvitation = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-1',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-1'},
        accept: {href: '/api/groups/group-1/invitations/inv-1/accept'},
        reject: {href: '/api/groups/group-1/invitations/inv-1/reject'},
        invitedMember: {href: '/api/members/member-2'},
    },
    ...overrides,
});

describe('GroupDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Skupina nenalezena')}));
        expect(screen.getByText('Skupina nenalezena')).toBeInTheDocument();
    });

    it('renders group name as page heading', () => {
        renderPage(createMockPageData(buildGroupDetail({name: 'Závoďáci'})));
        expect(screen.getByRole('heading', {level: 1, name: 'Závoďáci'})).toBeInTheDocument();
    });

    it('renders back link to /groups', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        const backLink = screen.getByRole('link', {name: /zpět na seznam/i});
        expect(backLink).toHaveAttribute('href', '/groups');
    });

    it('renders "Členové" section heading', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.getByText('ČLENOVÉ')).toBeInTheDocument();
    });

    it('renders owners section when owners exist', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('SPRÁVCI')).toBeInTheDocument();
    });

    it('resolves owner name with registration number via HAL member link', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Jana Nováková (ZBM9500)')).toBeInTheDocument();
    });

    it('does not render owners section when owners list is empty', () => {
        renderPage(createMockPageData(buildGroupDetail({owners: []})));
        expect(screen.queryByText('SPRÁVCI')).not.toBeInTheDocument();
    });

    it('shows "edit name" button when updateGroup template exists', () => {
        const resourceData = buildGroupDetail({
            _templates: {updateGroup: mockHalFormsTemplate({title: 'Upravit název', method: 'PATCH'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /upravit název/i})).toBeInTheDocument();
    });

    it('does not show "edit name" button when updateGroup template is absent', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.queryByRole('button', {name: /upravit název/i})).not.toBeInTheDocument();
    });

    it('clicking "edit name" button shows inline form', () => {
        const resourceData = buildGroupDetail({
            _templates: {updateGroup: mockHalFormsTemplate({title: 'Upravit název', method: 'PATCH'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /upravit název/i}));
        expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
    });

    it('shows "Přidat člena" button when addGroupMember template exists', () => {
        const resourceData = buildGroupDetail({
            _templates: {addGroupMember: mockHalFormsTemplate({title: 'Přidat člena', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat člena/i})).toBeInTheDocument();
    });

    it('does not show "Přidat člena" button when addGroupMember template is absent', () => {
        renderPage(createMockPageData(buildGroupDetail()));
        expect(screen.queryByRole('button', {name: /přidat člena/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat člena" opens modal', () => {
        const resourceData = buildGroupDetail({
            _templates: {addGroupMember: mockHalFormsTemplate({title: 'Přidat člena', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('renders member rows with resolved names, registration numbers, and joined dates', () => {
        const resourceData = buildGroupDetail({
            members: [buildMember()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Jana Nováková (ZBM9500)')).toBeInTheDocument();
        expect(screen.getByText('15. 1. 2025')).toBeInTheDocument();
    });

    it('renders "Odebrat" button per member when removeGroupMember template exists on member', () => {
        const member = buildMember({
            _templates: {removeGroupMember: mockHalFormsTemplate({title: 'Odebrat', method: 'DELETE', target: '/api/groups/group-1/members/member-1'})},
        });
        const resourceData = buildGroupDetail({members: [member]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat/i})).toBeInTheDocument();
    });

    it('does not render "Odebrat" button when removeGroupMember template is absent on member', () => {
        const resourceData = buildGroupDetail({members: [buildMember()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat" opens confirmation modal', () => {
        const member = buildMember({
            _templates: {removeGroupMember: mockHalFormsTemplate({title: 'Odebrat', method: 'DELETE', target: '/api/groups/group-1/members/member-1'})},
        });
        const resourceData = buildGroupDetail({members: [member]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows empty state message when no members', () => {
        renderPage(createMockPageData(buildGroupDetail({members: []})));
        expect(screen.getByText('Skupina nemá žádné členy.')).toBeInTheDocument();
    });

    describe('pending invitations (owner view)', () => {
        it('shows pending invitations section heading when invitations exist', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitation()],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.getByText('ČEKAJÍCÍ POZVÁNKY')).toBeInTheDocument();
        });

        it('does not show pending invitations section when list is empty', () => {
            renderPage(createMockPageData(buildGroupDetail({pendingInvitations: []})));
            expect(screen.queryByText('ČEKAJÍCÍ POZVÁNKY')).not.toBeInTheDocument();
        });

        it('does not show pending invitations section when field is absent', () => {
            renderPage(createMockPageData(buildGroupDetail()));
            expect(screen.queryByText('ČEKAJÍCÍ POZVÁNKY')).not.toBeInTheDocument();
        });

        it('renders invited member entry when invitedMember link exists', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitation()],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByText('inv-1')).not.toBeInTheDocument();
        });

        it('falls back to invitationId when member link is absent', () => {
            const invitation = buildPendingInvitation({_links: {self: {href: '/api/groups/group-1/invitations/inv-1'}}});
            const resourceData = buildGroupDetail({pendingInvitations: [invitation]});
            renderPage(createMockPageData(resourceData));
            expect(screen.getByText('inv-1')).toBeInTheDocument();
        });
    });

    describe('invite member (owner)', () => {
        it('shows "Pozvat člena" button when inviteMember template exists', () => {
            const resourceData = buildGroupDetail({
                _templates: {inviteMember: mockHalFormsTemplate({title: 'Pozvat člena', method: 'POST'})},
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /pozvat člena/i})).toBeInTheDocument();
        });

        it('does not show "Pozvat člena" button when inviteMember template is absent', () => {
            renderPage(createMockPageData(buildGroupDetail()));
            expect(screen.queryByRole('button', {name: /pozvat člena/i})).not.toBeInTheDocument();
        });

        it('clicking "Pozvat člena" opens modal', () => {
            const resourceData = buildGroupDetail({
                _templates: {inviteMember: mockHalFormsTemplate({title: 'Pozvat člena', method: 'POST'})},
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /pozvat člena/i}));
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });
    });
});
