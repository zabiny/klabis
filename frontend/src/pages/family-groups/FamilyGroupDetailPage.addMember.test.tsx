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

const buildChild = (overrides?: Record<string, unknown>) => ({
    memberId: 'child-1',
    joinedAt: '2024-01-01T00:00:00Z',
    _links: {member: {href: '/api/members/child-1'}},
    ...overrides,
});

describe('FamilyGroupDetailPage — unified Add member button (task 10.x)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows "Přidat člena" button when addFamilyGroupParent template exists', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat člena/i})).toBeInTheDocument();
    });

    it('shows "Přidat člena" button when addFamilyGroupChild template exists', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat člena/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat člena" button when neither addFamilyGroupParent nor addFamilyGroupChild template exists', () => {
        renderPage(createMockPageData(buildFamilyGroupDetail()));
        expect(screen.queryByRole('button', {name: /přidat člena/i})).not.toBeInTheDocument();
    });

    it('does NOT show old "Přidat rodiče" button as a standalone button', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /^přidat rodiče$/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat člena" opens role picker modal', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'}),
                addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('role picker shows "Rodič" option when addFamilyGroupParent template exists', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.getByRole('button', {name: /rodič/i})).toBeInTheDocument();
    });

    it('role picker shows "Dítě" option when addFamilyGroupChild template exists', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.getByRole('button', {name: /dítě/i})).toBeInTheDocument();
    });

    it('role picker does NOT show "Rodič" option when addFamilyGroupParent template is absent', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(screen.queryByRole('button', {name: /rodič/i})).not.toBeInTheDocument();
    });

    it('selecting "Rodič" in role picker shows HalFormDisplay', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'}),
                addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        fireEvent.click(screen.getByRole('button', {name: /rodič/i}));
        expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
    });

    it('selecting "Dítě" in role picker shows HalFormDisplay', () => {
        const resourceData = buildFamilyGroupDetail({
            _templates: {
                addFamilyGroupParent: mockHalFormsTemplate({title: 'Přidat rodiče', method: 'POST'}),
                addFamilyGroupChild: mockHalFormsTemplate({title: 'Přidat dítě', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        fireEvent.click(screen.getByRole('button', {name: /dítě/i}));
        expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
    });

    it('shows children section with "DĚTI" label when members array has entries', () => {
        const resourceData = buildFamilyGroupDetail({
            members: [buildChild()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/děti/i)).toBeInTheDocument();
    });

    it('shows "Odebrat" button per child when removeFamilyGroupChild template exists on child', () => {
        const childWithTemplate = {
            ...buildChild(),
            _links: {
                member: {href: '/api/members/child-1'},
                self: {href: '/api/groups/fg-1/children/child-1'},
            },
            _templates: {removeFamilyGroupChild: mockHalFormsTemplate({title: 'Odebrat dítě', method: 'DELETE', target: '/api/groups/fg-1/children/child-1'})},
        };
        const resourceData = buildFamilyGroupDetail({members: [childWithTemplate]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat" button on child when removeFamilyGroupChild template is absent', () => {
        const resourceData = buildFamilyGroupDetail({members: [buildChild()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat" on child opens confirmation modal', () => {
        const childWithTemplate = {
            ...buildChild(),
            _links: {
                member: {href: '/api/members/child-1'},
                self: {href: '/api/groups/fg-1/children/child-1'},
            },
            _templates: {removeFamilyGroupChild: mockHalFormsTemplate({title: 'Odebrat dítě', method: 'DELETE', target: '/api/groups/fg-1/children/child-1'})},
        };
        const resourceData = buildFamilyGroupDetail({members: [childWithTemplate]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });
});
