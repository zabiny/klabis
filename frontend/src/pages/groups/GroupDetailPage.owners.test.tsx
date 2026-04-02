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

describe('GroupDetailPage — owner management (task 6.3)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows "Přidat správce" button when addOwner template exists on resource', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
            _templates: {addOwner: mockHalFormsTemplate({title: 'Přidat správce', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat správce" button when addOwner template is absent', () => {
        renderPage(createMockPageData(buildGroupDetail({owners: [buildOwner()]})));
        expect(screen.queryByRole('button', {name: /přidat správce/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat správce" opens modal', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
            _templates: {addOwner: mockHalFormsTemplate({title: 'Přidat správce', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat správce/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows "Odebrat správce" button per owner when removeOwner template exists on owner', () => {
        const ownerWithTemplate = {
            ...buildOwner(),
            _templates: {removeOwner: mockHalFormsTemplate({title: 'Odebrat správce', method: 'DELETE', target: '/api/groups/group-1/owners/owner-1'})},
        };
        const resourceData = buildGroupDetail({owners: [ownerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat správce" button when removeOwner template is absent on owner', () => {
        const resourceData = buildGroupDetail({owners: [buildOwner()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat správce/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat správce" opens confirmation modal', () => {
        const ownerWithTemplate = {
            ...buildOwner(),
            _links: {
                member: {href: '/api/members/owner-1'},
                self: {href: '/api/groups/group-1/owners/owner-1'},
            },
            _templates: {removeOwner: mockHalFormsTemplate({title: 'Odebrat správce', method: 'DELETE', target: '/api/groups/group-1/owners/owner-1'})},
        };
        const resourceData = buildGroupDetail({owners: [ownerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat správce/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });
});
