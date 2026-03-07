import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembersPage} from './MembersPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
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
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, onClose}: any) => (
        <div data-testid="hal-forms-display">
            <h3>{template.title || templateName}</h3>
            <button onClick={onClose} data-testid="close-form-button">Close</button>
        </div>
    ),
}));

vi.mock('../../components/UI/ModalOverlay.tsx', () => ({
    ModalOverlay: ({isOpen, children, onClose}: any) => (
        isOpen ? <div data-testid="modal-overlay" role="dialog">{children}<button onClick={onClose}>Close</button></div> : null
    ),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/members',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
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
            <MemoryRouter initialEntries={['/members']}>
                <HalFormProvider>
                    <HalFormsPageLayout>
                        <MembersPage/>
                    </HalFormsPageLayout>
                </HalFormProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembersPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Členové"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Členové')).toBeInTheDocument();
    });

    it('renders "Registrovat člena" button when template exists (label overrides template title)', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {
                default: mockHalFormsTemplate({title: 'Create Member'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /registrovat člena/i})).toBeInTheDocument();
        expect(screen.queryByText('Create Member')).not.toBeInTheDocument();
    });

    it('does NOT render "Registrovat člena" button when template does not exist', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /registrovat člena/i})).not.toBeInTheDocument();
    });

    it('renders table columns', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Reg. číslo')).toBeInTheDocument();
        expect(screen.getByText('Příjmení')).toBeInTheDocument();
        expect(screen.getByText('Jméno')).toBeInTheDocument();
        expect(screen.getByText('E-mail')).toBeInTheDocument();
        expect(screen.getByText('Stav')).toBeInTheDocument();
    });

    it('shows loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(screen.getByText(/načítání/i)).toBeInTheDocument();
    });
});
