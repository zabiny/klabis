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
        resourceData: {firstName: 'Petr', lastName: 'Trenér', registrationNumber: 'ZBM1000', _links: {self: {href: '/api/members/trainer-1'}}},
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
    trainers: [],
    members: [],
    _links: {self: {href: '/api/groups/tg-1'}},
    ...overrides,
});

const buildTrainer = (overrides?: Record<string, unknown>) => ({
    memberId: 'trainer-1',
    _links: {member: {href: '/api/members/trainer-1'}},
    ...overrides,
});

describe('TrainingGroupDetailPage — trainer management', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows trainers section label', () => {
        const resourceData = buildTrainingGroupDetail({
            trainers: [buildTrainer()],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/trenéři/i)).toBeInTheDocument();
    });

    it('shows "Přidat trenéra" button when addTrainer template exists on resource', () => {
        const resourceData = buildTrainingGroupDetail({
            trainers: [buildTrainer()],
            _templates: {addTrainer: mockHalFormsTemplate({title: 'Přidat trenéra', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat trenéra/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat trenéra" button when addTrainer template is absent', () => {
        renderPage(createMockPageData(buildTrainingGroupDetail({trainers: [buildTrainer()]})));
        expect(screen.queryByRole('button', {name: /přidat trenéra/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat trenéra" opens modal', () => {
        const resourceData = buildTrainingGroupDetail({
            trainers: [buildTrainer()],
            _templates: {addTrainer: mockHalFormsTemplate({title: 'Přidat trenéra', method: 'POST'})},
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /přidat trenéra/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows "Odebrat trenéra" button per trainer when removeTrainer template and self link exist on trainer', () => {
        const trainerWithTemplate = {
            ...buildTrainer(),
            _links: {
                member: {href: '/api/members/trainer-1'},
                self: {href: '/api/groups/tg-1/trainers/trainer-1'},
            },
            _templates: {removeTrainer: mockHalFormsTemplate({title: 'Odebrat trenéra', method: 'DELETE', target: '/api/groups/tg-1/trainers/trainer-1'})},
        };
        const resourceData = buildTrainingGroupDetail({trainers: [trainerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /odebrat trenéra/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat trenéra" button when removeTrainer template is absent on trainer', () => {
        const resourceData = buildTrainingGroupDetail({trainers: [buildTrainer()]});
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /odebrat trenéra/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat trenéra" opens confirmation modal', () => {
        const trainerWithTemplate = {
            ...buildTrainer(),
            _links: {
                member: {href: '/api/members/trainer-1'},
                self: {href: '/api/groups/tg-1/trainers/trainer-1'},
            },
            _templates: {removeTrainer: mockHalFormsTemplate({title: 'Odebrat trenéra', method: 'DELETE', target: '/api/groups/tg-1/trainers/trainer-1'})},
        };
        const resourceData = buildTrainingGroupDetail({trainers: [trainerWithTemplate]});
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /odebrat trenéra/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });

    it('shows single edit button when updateTrainingGroup template exists', () => {
        const resourceData = buildTrainingGroupDetail({
            _templates: {updateTrainingGroup: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'})},
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /upravit skupinu/i})).toBeInTheDocument();
    });

    it('does NOT show separate "Změnit věkové rozmezí" button (merged into single edit)', () => {
        const resourceData = buildTrainingGroupDetail({
            _templates: {
                updateTrainingGroup: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /věkové rozmezí/i})).not.toBeInTheDocument();
    });
});
