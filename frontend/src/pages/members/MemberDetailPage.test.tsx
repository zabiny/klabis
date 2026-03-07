import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MemberDetailPage} from './MemberDetailPage';
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
    chipNumber: '12345678',
    bankAccountNumber: 'CZ6508000000192000145399',
    dietaryRestrictions: 'Vegetarián',
    birthNumber: '9003151234',
    identityCard: {cardNumber: '123456789', validityDate: '2030-06-30'},
    medicalCourse: {completionDate: '2024-01-01', validityDate: '2026-12-31'},
    trainerLicense: {licenseNumber: 'TL-2024-001', validityDate: '2025-06-30'},
    drivingLicenseGroup: 'B',
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

describe('MemberDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders back link "Zpět na seznam"', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText(/zpět na seznam/i)).toBeInTheDocument();
        expect(screen.getByText(/zpět na seznam/i).closest('a')).toHaveAttribute('href', '/members');
    });

    it('renders member name and registration number', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByRole('heading', {level: 1, name: 'Jan Novák'})).toBeInTheDocument();
        expect(screen.getByText('SKI2601')).toBeInTheDocument();
    });

    it('renders "Aktivní" badge when member is active', () => {
        renderPage(createMockPageData(mockMemberDetailData({active: true})));
        expect(screen.getByText('Aktivní')).toBeInTheDocument();
    });

    it('renders "Neaktivní" badge when member is inactive', () => {
        renderPage(createMockPageData(mockMemberDetailData({active: false})));
        expect(screen.getByText('Neaktivní')).toBeInTheDocument();
    });

    it('shows personal info section with formatted data', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText('OSOBNÍ ÚDAJE')).toBeInTheDocument();
        expect(screen.getByText('Muž')).toBeInTheDocument();
        expect(screen.getByText('Datum narození')).toBeInTheDocument();
        expect(screen.getByText('Státní příslušnost')).toBeInTheDocument();
    });

    it('shows contact section', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText('KONTAKT')).toBeInTheDocument();
        expect(screen.getByText('jan.novak@email.cz')).toBeInTheDocument();
        expect(screen.getByText('+420777123456')).toBeInTheDocument();
    });

    it('shows address section', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText('ADRESA')).toBeInTheDocument();
        expect(screen.getByText('Hlavní 15')).toBeInTheDocument();
        expect(screen.getByText('Praha')).toBeInTheDocument();
        expect(screen.getByText('11000')).toBeInTheDocument();
    });

    it('shows guardian section when guardian data exists', () => {
        const data = mockMemberDetailData({
            guardian: {
                firstName: 'Marie',
                lastName: 'Nováková',
                relationship: 'matka',
                email: 'marie@email.cz',
                phone: '+420777999888',
            },
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('ZÁKONNÝ ZÁSTUPCE')).toBeInTheDocument();
        expect(screen.getByText('Marie Nováková')).toBeInTheDocument();
        expect(screen.getByText('matka')).toBeInTheDocument();
        expect(screen.getByText('marie@email.cz')).toBeInTheDocument();
        expect(screen.getByText('+420777999888')).toBeInTheDocument();
    });

    it('does NOT show guardian section when no guardian', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.queryByText('ZÁKONNÝ ZÁSTUPCE')).not.toBeInTheDocument();
    });

    it('shows "Upravit" button when edit template exists (label overrides template title)', () => {
        const data = mockMemberDetailData({
            _templates: {
                default: mockHalFormsTemplate({title: 'Edit Member'}),
            },
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
        expect(screen.queryByText('Edit Member')).not.toBeInTheDocument();
    });

    it('does NOT show "Upravit" button when edit template missing', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.queryByRole('button', {name: /upravit/i})).not.toBeInTheDocument();
    });

    it('shows "Ukončit členství" button when terminate template exists (label overrides template title)', () => {
        const data = mockMemberDetailData({
            active: true,
            _templates: {
                terminate: mockHalFormsTemplate({title: 'Terminate'}),
            },
        });
        renderPage(createMockPageData(data));
        expect(screen.getByRole('button', {name: /ukončit členství/i})).toBeInTheDocument();
        expect(screen.queryByText('Terminate')).not.toBeInTheDocument();
    });

    it('shows birth number masked when nationality is CZ', () => {
        renderPage(createMockPageData(mockMemberDetailData({nationality: 'CZ', birthNumber: '9003151234'})));
        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
        expect(screen.getByText(/••••••\/••••/)).toBeInTheDocument();
    });

    it('does NOT show birth number when nationality is not CZ', () => {
        renderPage(createMockPageData(mockMemberDetailData({nationality: 'SK', birthNumber: '9003151234'})));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    it('shows deactivation section when member is inactive', () => {
        const data = mockMemberDetailData({
            active: false,
            deactivationReason: 'ODHLASKA',
            deactivatedAt: '2025-06-15',
            deactivationNote: 'Osobní důvody',
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
        expect(screen.getByText('Odhlášení')).toBeInTheDocument();
        expect(screen.getByText('Osobní důvody')).toBeInTheDocument();
    });
});
