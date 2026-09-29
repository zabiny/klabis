import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {FamilyGroupDetailPage} from './FamilyGroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
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
}));

vi.mock('../../contexts/halRouteContext.ts', () => ({
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jana', lastName: 'Rodičová', registrationNumber: 'ZBM2000', _links: {self: {href: '/api/members/parent-1'}}},
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
        pathname: '/family-groups/fg-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups/fg-1'}),
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
            <MemoryRouter initialEntries={['/family-groups/fg-1']}>
                <FamilyGroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildFamilyGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'fg-1',
    name: 'Novákovi',
    parents: [],
    members: [],
    _links: {self: {href: '/api/groups/fg-1'}},
    ...overrides,
});

// A parent is a user of the system, not necessarily a club member: the row carries only the user
// id and has no "member" link. The "self" link with the remove affordance is added by the backend
// only when the caller may manage members and the group has more than one parent.
const PARENT_USER_ID = '6f1a0f9c-2b0a-4a1c-9d3e-0a1b2c3d4e5f';

const buildParent = (overrides?: Record<string, unknown>) => ({
    userId: PARENT_USER_ID,
    _links: {},
    ...overrides,
});

const buildRemovableParent = () => ({
    ...buildParent(),
    _links: {self: {href: `/api/family-groups/fg-1/parents/${PARENT_USER_ID}`}},
    _templates: {
        removeFamilyGroupParent: mockHalFormsTemplate({
            title: 'Odebrat rodiče',
            method: 'DELETE',
            target: `/api/family-groups/fg-1/parents/${PARENT_USER_ID}`,
        }),
    },
});

describe('FamilyGroupDetailPage — parent management', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows parents section label "RODIČE"', () => {
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/rodiče/i)).toBeInTheDocument();
    });

    it('adding parent is accessible via role picker: "Přidat člena" -> "Rodič"', () => {
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent()],
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.getByRole('button', {name: /rodič/i})).toBeInTheDocument();
    });

    it('selecting "Rodič" in role picker shows HalFormDisplay', () => {
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent()],
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        fireEvent.click(screen.getByRole('button', {name: /rodič/i}));
        expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
    });

    it('does NOT show "Přidat člena" button when addFamilyGroupParent template is absent', () => {
        renderPage(createMockPageData(buildFamilyGroupDetail({parents: [buildParent()]})));
        expect(screen.queryByRole('button', {name: /přidat člena/i})).not.toBeInTheDocument();
    });

    it('shows "Odebrat rodiče" button per parent when removeFamilyGroupParent template and self link exist on parent', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildRemovableParent()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat rodiče/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat rodiče" button when removeFamilyGroupParent template is absent on parent', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildParent()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat rodiče/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat rodiče" opens confirmation modal', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildRemovableParent()]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat rodiče/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    // A parent need not have a member profile, so the row has no "member" link and there is nothing
    // to resolve to a name: the raw user id is what the user sees.
    it('lists a parent by raw user id when the row has no member link', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildParent()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(PARENT_USER_ID)).toBeInTheDocument();
    });

    it('does not resolve a parent row through a member profile link', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildParent()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByText(/Jana Rodičová/)).not.toBeInTheDocument();
    });

    it('lists every parent by its own user id', () => {
        const secondParentId = '11111111-2222-3333-4444-555555555555';
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent(), buildParent({userId: secondParentId})],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(PARENT_USER_ID)).toBeInTheDocument();
        expect(screen.getByText(secondParentId)).toBeInTheDocument();
    });
});
