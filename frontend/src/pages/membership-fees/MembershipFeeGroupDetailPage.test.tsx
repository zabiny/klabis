import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeGroupDetailPage} from './MembershipFeeGroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
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

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/membership-fee-groups/group-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-groups/group-1'}),
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
            <MemoryRouter initialEntries={['/administration/membership-fee-groups/group-1']}>
                <MembershipFeeGroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembershipFeeGroupDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Chyba serveru')}));
        expect(screen.getByText('Chyba serveru')).toBeInTheDocument();
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
});
