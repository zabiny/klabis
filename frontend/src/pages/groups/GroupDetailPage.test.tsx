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
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const buildMember = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    firstName: 'Jana',
    lastName: 'Nováková',
    registrationNumber: 'ZBM0001',
    joinedAt: '2025-01-15T00:00:00Z',
    _links: {self: {href: '/api/members/member-1'}},
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
            owners: [{id: 'owner-1', firstName: 'Petr', lastName: 'Kovář', registrationNumber: 'ZBM9000'}],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('SPRÁVCI')).toBeInTheDocument();
        expect(screen.getByText('Petr Kovář')).toBeInTheDocument();
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

    it('renders member rows with names and joined dates', () => {
        const resourceData = buildGroupDetail({
            members: [buildMember()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Nováková')).toBeInTheDocument();
        expect(screen.getByText('Jana')).toBeInTheDocument();
        expect(screen.getByText('ZBM0001')).toBeInTheDocument();
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
});
