import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

const {MEMBER_DATA, mockNavigate, currentMemberId} = vi.hoisted(() => ({
    // Keyed by the path HalRouteProvider requests — the /api prefix is stripped by normalizeKlabisApiPath.
    MEMBER_DATA: {
        '/members/owner-1': {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9000'},
        '/members/member-1': {firstName: 'Petr', lastName: 'Svoboda', registrationNumber: 'ZBM9500'},
    } as Record<string, {firstName: string; lastName: string; registrationNumber: string}>,
    mockNavigate: vi.fn(),
    currentMemberId: {value: 'owner-1' as string | null},
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/authContext.ts', () => ({
    useAuth: () => ({getUser: () => ({memberId: currentMemberId.value})}),
}));

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();
    return {...actual, useNavigate: () => mockNavigate};
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn((url: string) => ({data: MEMBER_DATA[url] ?? null, error: null})),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({onSubmitSuccess}: {onSubmitSuccess?: () => void}) => (
        <div data-testid="hal-form-display">
            <button onClick={() => onSubmitSuccess?.()}>submit-form</button>
        </div>
    ),
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title, context}: {
            isOpen: boolean;
            children: React.ReactNode;
            title: string;
            context?: React.ReactNode;
        }) => isOpen
            ? <div data-testid="modal-overlay" data-title={title}>
                {context ? <div data-testid="modal-note">{context}</div> : null}
                {children}
            </div>
            : null,
    };
});

const createMockPageData = (resourceData: HalResponse | null, routeOverrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/groups/group-1',
        navigateToResource: vi.fn(),
        refetch: vi.fn(async () => {}),
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups/group-1'}),
        ...routeOverrides,
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
});

const renderPage = (resourceData: HalResponse, routeOverrides?: Record<string, unknown>) => {
    vi.mocked(useHalPageData).mockReturnValue(
        createMockPageData(resourceData, routeOverrides) as ReturnType<typeof useHalPageData>
    );
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/groups/group-1']}>
                <GroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildOwner = (overrides?: Record<string, unknown>) => ({
    memberId: 'owner-1',
    _links: {member: {href: '/api/members/owner-1'}},
    ...overrides,
});

const buildMember = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    joinedAt: '2025-01-15T10:00:00Z',
    _links: {member: {href: '/api/members/member-1'}},
    ...overrides,
});

const buildGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [buildOwner()],
    members: [buildMember()],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const leaveTemplate = () => mockHalFormsTemplate({
    title: 'removeGroupMember',
    method: 'DELETE',
    target: '/api/groups/group-1/members/member-1',
});

const ownerWithRemoveTemplate = () => buildOwner({
    _links: {
        member: {href: '/api/members/owner-1'},
        self: {href: '/api/groups/group-1/owners/owner-1'},
    },
    _templates: {
        removeGroupOwner: mockHalFormsTemplate({
            title: 'Odebrat správce',
            method: 'DELETE',
            target: '/api/groups/group-1/owners/owner-1',
        }),
    },
});

describe('GroupDetailPage — owners and members are disjoint', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'owner-1';
    });

    it('shows every person exactly once — the owner is not repeated in the member list', () => {
        renderPage(buildGroupDetail());

        expect(screen.getByText('SPRÁVCI')).toBeInTheDocument();
        expect(screen.getByText('ČLENOVÉ')).toBeInTheDocument();
        expect(screen.getAllByText('Jana Nováková (ZBM9000)')).toHaveLength(1);
        expect(screen.getAllByText('Petr Svoboda (ZBM9500)')).toHaveLength(1);
    });

    it('shows no member section rows when the creator is the group\'s only person', () => {
        renderPage(buildGroupDetail({members: []}));

        expect(screen.getByText('Skupina nemá žádné členy.')).toBeInTheDocument();
        expect(screen.getAllByText('Jana Nováková (ZBM9000)')).toHaveLength(1);
    });

    it('warns that removing another owner removes them from the group, and stays on the page', () => {
        currentMemberId.value = 'someone-else';
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}));
        fireEvent.click(screen.getByRole('button', {name: /odebrat správce/i}));

        expect(screen.getByTestId('modal-overlay')).toHaveAttribute('data-title', 'Odebrat správce');
        expect(screen.getByTestId('modal-note')).toHaveTextContent(
            'Odebráním správce dotčená osoba opustí skupinu.'
        );

        fireEvent.click(screen.getByRole('button', {name: 'submit-form'}));
        expect(mockNavigate).not.toHaveBeenCalled();
    });
});

describe('GroupDetailPage — an owner giving up their own ownership', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'owner-1';
    });

    it('warns in the second person that the caller is the one leaving', () => {
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}));
        fireEvent.click(screen.getByRole('button', {name: /odebrat správce/i}));

        expect(screen.getByTestId('modal-note')).toHaveTextContent(
            'Odebráním sebe ze správců opustíte skupinu.'
        );
    });

    it('returns to the group list instead of refetching the now-unreadable detail', () => {
        const refetch = vi.fn().mockResolvedValue(undefined);
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}), {refetch});
        fireEvent.click(screen.getByRole('button', {name: /odebrat správce/i}));
        fireEvent.click(screen.getByRole('button', {name: 'submit-form'}));

        expect(mockNavigate).toHaveBeenCalledWith('/groups');
        expect(refetch).not.toHaveBeenCalled();
    });
});

describe('GroupDetailPage — member leaves the free group', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'member-1';
    });

    it('offers "Opustit skupinu" when the backend sends removeGroupMember on the self link', () => {
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));

        expect(screen.getByRole('button', {name: /opustit skupinu/i})).toBeInTheDocument();
    });

    it('offers nothing to leave with when the self link has no removeGroupMember (owners)', () => {
        renderPage(buildGroupDetail());

        expect(screen.queryByRole('button', {name: /opustit skupinu/i})).not.toBeInTheDocument();
    });

    it('asks for confirmation and explains the consequence', () => {
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        fireEvent.click(screen.getByRole('button', {name: /opustit skupinu/i}));

        expect(screen.getByTestId('modal-overlay')).toHaveAttribute('data-title', 'Opustit skupinu');
        expect(screen.getByTestId('modal-note')).toHaveTextContent(
            'Opuštěním skupiny ztratíte členství ve skupině.'
        );
    });

    it('returns the user to the group list after leaving', () => {
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        fireEvent.click(screen.getByRole('button', {name: /opustit skupinu/i}));
        fireEvent.click(screen.getByRole('button', {name: 'submit-form'}));

        expect(mockNavigate).toHaveBeenCalledWith('/groups');
    });

    it('does not navigate away when the leave is cancelled', () => {
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        fireEvent.click(screen.getByRole('button', {name: /opustit skupinu/i}));

        expect(mockNavigate).not.toHaveBeenCalled();
    });
});