import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {MemberDetailPage} from './MemberDetailPage';
import {vi} from 'vitest';
import type {HalFormsTemplate, HalResponse} from '../../api';

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
        refetch: vi.fn(),
        status: 'success',
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

const adminEditTemplate: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno'},
        {name: 'lastName', type: 'text', prompt: 'Příjmení'},
        {name: 'email', type: 'email', prompt: 'E-mail'},
    ],
};


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
        getResourceLink: vi.fn().mockImplementation((rel: string) =>
            resourceData?._links?.[rel as keyof typeof resourceData._links] ?? null
        ),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => false),
    getEmbeddedItems: vi.fn(() => []),
    isCollection: vi.fn(() => false),
    hasLink: vi.fn((rel: string) => !!(resourceData?._links?.[rel as keyof typeof resourceData._links])),
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

describe('MemberDetailPage — group navigation buttons', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders "Tréninková skupina" button when trainingGroup link is present', () => {
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: {href: '/api/training-groups/tg-uuid'},
            },
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /tréninková skupina/i})).toBeInTheDocument();
    });

    it('clicking "Tréninková skupina" button calls navigateToResource with the trainingGroup link', async () => {
        const user = userEvent.setup();
        const navigateToResource = vi.fn();
        const trainingGroupLink = {href: '/api/training-groups/tg-uuid'};
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: trainingGroupLink,
            },
            _templates: {updateMember: adminEditTemplate},
        });
        const pageData = createMockPageData(data, {
            route: {
                pathname: '/members/123',
                navigateToResource,
                refetch: async () => {},
                queryState: 'success' as const,
                getResourceLink: vi.fn().mockImplementation((rel: string) =>
                    rel === 'trainingGroup' ? trainingGroupLink : null
                ),
            },
        });
        renderPage(pageData);

        await user.click(screen.getByRole('button', {name: /tréninková skupina/i}));

        expect(navigateToResource).toHaveBeenCalledWith(trainingGroupLink);
    });

    it('renders "Rodina" button when familyGroup link is present', () => {
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                familyGroup: {href: '/api/family-groups/fg-uuid'},
            },
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /rodina/i})).toBeInTheDocument();
    });

    it('clicking "Rodina" button calls navigateToResource with the familyGroup link', async () => {
        const user = userEvent.setup();
        const navigateToResource = vi.fn();
        const familyGroupLink = {href: '/api/family-groups/fg-uuid'};
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                familyGroup: familyGroupLink,
            },
            _templates: {updateMember: adminEditTemplate},
        });
        const pageData = createMockPageData(data, {
            route: {
                pathname: '/members/123',
                navigateToResource,
                refetch: async () => {},
                queryState: 'success' as const,
                getResourceLink: vi.fn().mockImplementation((rel: string) =>
                    rel === 'familyGroup' ? familyGroupLink : null
                ),
            },
        });
        renderPage(pageData);

        await user.click(screen.getByRole('button', {name: /rodina/i}));

        expect(navigateToResource).toHaveBeenCalledWith(familyGroupLink);
    });

    it('renders neither group button when both links are absent', () => {
        const data = mockMemberDetailData({
            _templates: {updateMember: adminEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.queryByRole('button', {name: /tréninková skupina/i})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /rodina/i})).not.toBeInTheDocument();
    });

    it('renders group navigation buttons even without an edit template (other-member view)', () => {
        // The backend emits trainingGroup/familyGroup links for any reader of the member detail,
        // regardless of whether the reader has edit permissions. Buttons must render on HAL link
        // presence alone — no _templates.updateMember required.
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/123'},
                trainingGroup: {href: '/api/training-groups/tg-uuid'},
                familyGroup: {href: '/api/family-groups/fg-uuid'},
            },
            // deliberately no _templates
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /tréninková skupina/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /rodina/i})).toBeInTheDocument();
    });

});
