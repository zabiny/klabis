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

const buildParent = (overrides?: Record<string, unknown>) => ({
    memberId: 'parent-1',
    _links: {member: {href: '/api/members/parent-1'}},
    ...overrides,
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

    it('shows "Přidat rodiče" button when addFamilyGroupParent template exists on resource', () => {
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent()],
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat rodiče/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat rodiče" button when addFamilyGroupParent template is absent', () => {
        renderPage(createMockPageData(buildFamilyGroupDetail({parents: [buildParent()]})));
        expect(screen.queryByRole('button', {name: /přidat rodiče/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat rodiče" opens modal', () => {
        const resourceData = buildFamilyGroupDetail({
            parents: [buildParent()],
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat rodiče/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows "Odebrat rodiče" button per parent when removeFamilyGroupParent template and self link exist on parent', () => {
        const parentWithTemplate = {
            ...buildParent(),
            _links: {
                member: {href: '/api/members/parent-1'},
                self: {href: '/api/groups/fg-1/parents/parent-1'},
            },
            _templates: {removeFamilyGroupParent: mockHalFormsTemplate({title: 'Odebrat rodiče', method: 'DELETE', target: '/api/groups/fg-1/parents/parent-1'})},
        };
        const resourceData = buildFamilyGroupDetail({parents: [parentWithTemplate]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat rodiče/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat rodiče" button when removeFamilyGroupParent template is absent on parent', () => {
        const resourceData = buildFamilyGroupDetail({parents: [buildParent()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat rodiče/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat rodiče" opens confirmation modal', () => {
        const parentWithTemplate = {
            ...buildParent(),
            _links: {
                member: {href: '/api/members/parent-1'},
                self: {href: '/api/groups/fg-1/parents/parent-1'},
            },
            _templates: {removeFamilyGroupParent: mockHalFormsTemplate({title: 'Odebrat rodiče', method: 'DELETE', target: '/api/groups/fg-1/parents/parent-1'})},
        };
        const resourceData = buildFamilyGroupDetail({parents: [parentWithTemplate]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat rodiče/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });
});
