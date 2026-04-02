import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {MemberDetailPage} from './MemberDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(() => ({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        error: null,
    })),
    useAuthorizedQuery: vi.fn(() => ({
        data: undefined,
        isLoading: false,
        error: null,
    })),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
}));

vi.mock('../../contexts/ToastContext', () => ({
    useToast: vi.fn(() => ({
        addToast: vi.fn(),
    })),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/hateoas', () => ({
    submitHalFormsData: vi.fn(),
    isFormValidationError: vi.fn((error) => {
        return error && typeof error === 'object' && 'validationErrors' in error;
    }),
    toFormValidationError: vi.fn((error) => error),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: any) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {title && <h4>{title}</h4>}
                {children}
                <button onClick={onClose}>Close</button>
            </div>
        ) : null
    ),
}));

const mockMemberDetailData = (overrides?: Partial<any>): HalResponse => ({
    id: '123e4567-e89b-12d3-a456-426614174000',
    registrationNumber: 'SKI2601',
    firstName: 'Jan',
    lastName: 'Novák',
    dateOfBirth: '1990-03-15',
    nationality: 'CZ',
    gender: 'MALE',
    email: 'jan.novak@email.cz',
    phone: '+420777123456',
    address: {street: 'Hlavní 15', city: 'Praha', postalCode: '11000', country: 'CZ'},
    active: true,
    _links: {
        self: {href: '/api/members/123e4567-e89b-12d3-a456-426614174000'},
    },
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/members/123e4567-e89b-12d3-a456-426614174000',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: '/api/members/123e4567-e89b-12d3-a456-426614174000'}),
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

const renderPage = (pageData: any) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members/123e4567-e89b-12d3-a456-426614174000']}>
                <HalFormProvider>
                    <HalFormsPageLayout>
                        <MemberDetailPage/>
                    </HalFormsPageLayout>
                </HalFormProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MemberDetailPage — group membership sections', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('Training group section (task 4.4)', () => {
        it('shows training group section when trainingGroup data is present', () => {
            const data = mockMemberDetailData({
                trainingGroup: {
                    name: 'Mladí závodníci',
                    owners: [{name: 'Petr Trenér', email: 'petr@klub.cz'}],
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('TRÉNINKOVÁ SKUPINA')).toBeInTheDocument();
            expect(screen.getByText('Mladí závodníci')).toBeInTheDocument();
        });

        it('shows owner name and email in training group section', () => {
            const data = mockMemberDetailData({
                trainingGroup: {
                    name: 'Mladí závodníci',
                    owners: [{name: 'Petr Trenér', email: 'petr@klub.cz'}],
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('Petr Trenér')).toBeInTheDocument();
            expect(screen.getByText('petr@klub.cz')).toBeInTheDocument();
        });

        it('shows multiple owners in training group section', () => {
            const data = mockMemberDetailData({
                trainingGroup: {
                    name: 'Elitní tým',
                    owners: [
                        {name: 'Petr Trenér', email: 'petr@klub.cz'},
                        {name: 'Jana Koučová', email: 'jana@klub.cz'},
                    ],
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('Petr Trenér')).toBeInTheDocument();
            expect(screen.getByText('Jana Koučová')).toBeInTheDocument();
        });

        it('shows "Není přiřazen" when trainingGroup is null', () => {
            const data = mockMemberDetailData({trainingGroup: null});
            renderPage(createMockPageData(data));
            expect(screen.getByText('TRÉNINKOVÁ SKUPINA')).toBeInTheDocument();
            expect(screen.getByText('Není přiřazen')).toBeInTheDocument();
        });

        it('does NOT show training group section when trainingGroup field is absent', () => {
            renderPage(createMockPageData(mockMemberDetailData()));
            expect(screen.queryByText('TRÉNINKOVÁ SKUPINA')).not.toBeInTheDocument();
        });

        it('shows training group section when trainingGroup has no owners', () => {
            const data = mockMemberDetailData({
                trainingGroup: {name: 'Bezprizorní', owners: []},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('TRÉNINKOVÁ SKUPINA')).toBeInTheDocument();
            expect(screen.getByText('Bezprizorní')).toBeInTheDocument();
        });
    });

    describe('Family group section (task 5.4)', () => {
        it('shows family group section when familyGroup data is present', () => {
            const data = mockMemberDetailData({
                familyGroup: {name: 'Rodina Novákových'},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('RODINNÁ SKUPINA')).toBeInTheDocument();
            expect(screen.getByText('Rodina Novákových')).toBeInTheDocument();
        });

        it('does NOT show family group section when familyGroup field is absent', () => {
            renderPage(createMockPageData(mockMemberDetailData()));
            expect(screen.queryByText('RODINNÁ SKUPINA')).not.toBeInTheDocument();
        });

        it('does NOT show family group section when familyGroup is null', () => {
            const data = mockMemberDetailData({familyGroup: null});
            renderPage(createMockPageData(data));
            expect(screen.queryByText('RODINNÁ SKUPINA')).not.toBeInTheDocument();
        });
    });
});
