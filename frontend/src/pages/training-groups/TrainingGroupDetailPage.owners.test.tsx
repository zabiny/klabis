import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {TrainingGroupDetailPage} from './TrainingGroupDetailPage';
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
        resourceData: {firstName: 'Petr', lastName: 'Trenér', registrationNumber: 'ZBM1000', _links: {self: {href: '/api/members/owner-1'}}},
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
        pathname: '/training-groups/tg-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups/tg-1'}),
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
            <MemoryRouter initialEntries={['/training-groups/tg-1']}>
                <TrainingGroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildTrainingGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'tg-1',
    name: 'Tréninková skupina U10',
    minAge: 8,
    maxAge: 10,
    owners: [],
    members: [],
    _links: {self: {href: '/api/groups/tg-1'}},
    ...overrides,
});

const buildOwner = (overrides?: Record<string, unknown>) => ({
    memberId: 'owner-1',
    _links: {member: {href: '/api/members/owner-1'}},
    ...overrides,
});

describe('TrainingGroupDetailPage — owner management (task 6.3)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows "Přidat správce" button when addOwner template exists on resource', () => {
        const resourceData = buildTrainingGroupDetail({
            owners: [buildOwner()],
            _templates: {addTrainingGroupOwner: mockHalFormsTemplate({title: 'Přidat správce', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat správce" button when addOwner template is absent', () => {
        renderPage(createMockPageData(buildTrainingGroupDetail({owners: [buildOwner()]})));
        expect(screen.queryByRole('button', {name: /přidat správce/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat správce" opens modal', () => {
        const resourceData = buildTrainingGroupDetail({
            owners: [buildOwner()],
            _templates: {addTrainingGroupOwner: mockHalFormsTemplate({title: 'Přidat správce', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat správce/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows "Odebrat správce" button per owner when removeOwner template exists on owner', () => {
        const ownerWithTemplate = {
            ...buildOwner(),
            _templates: {removeTrainingGroupOwner: mockHalFormsTemplate({title: 'Odebrat správce', method: 'DELETE', target: '/api/groups/tg-1/owners/owner-1'})},
        };
        const resourceData = buildTrainingGroupDetail({owners: [ownerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat správce" button when removeOwner template is absent on owner', () => {
        const resourceData = buildTrainingGroupDetail({owners: [buildOwner()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat správce/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat správce" opens confirmation modal', () => {
        const ownerWithTemplate = {
            ...buildOwner(),
            _links: {
                member: {href: '/api/members/owner-1'},
                self: {href: '/api/groups/tg-1/owners/owner-1'},
            },
            _templates: {removeTrainingGroupOwner: mockHalFormsTemplate({title: 'Odebrat správce', method: 'DELETE', target: '/api/groups/tg-1/owners/owner-1'})},
        };
        const resourceData = buildTrainingGroupDetail({owners: [ownerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat správce/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });
});
