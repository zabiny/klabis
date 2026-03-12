import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
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
    })),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
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

const adminEditTemplate: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno'},
        {name: 'lastName', type: 'text', prompt: 'Příjmení'},
        {name: 'email', type: 'email', prompt: 'E-mail'},
        {name: 'phone', type: 'tel', prompt: 'Telefon'},
        {name: 'registrationNumber', type: 'text', readOnly: true},
    ],
};

const selfEditTemplate: HalFormsTemplate = {
    method: 'PATCH',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000/profile',
    properties: [
        {name: 'email', type: 'email', prompt: 'E-mail'},
        {name: 'phone', type: 'tel', prompt: 'Telefon'},
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
        expect(screen.getByText('Marie')).toBeInTheDocument();
        expect(screen.getByText('Nováková')).toBeInTheDocument();
        expect(screen.getByText('matka')).toBeInTheDocument();
        expect(screen.getByText('marie@email.cz')).toBeInTheDocument();
        expect(screen.getByText('+420777999888')).toBeInTheDocument();
    });

    it('does NOT show guardian section when no guardian', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.queryByText('ZÁKONNÝ ZÁSTUPCE')).not.toBeInTheDocument();
    });

    it('shows birth number masked when nationality is CZ (self view)', () => {
        const data = mockMemberDetailData({
            nationality: 'CZ',
            birthNumber: '9003151234',
            _templates: {default: selfEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
        expect(screen.getByText(/••••••\/••••/)).toBeInTheDocument();
    });

    it('reveals birth number without double slash when backend already includes slash', async () => {
        const user = userEvent.setup();
        const data = mockMemberDetailData({
            nationality: 'CZ',
            birthNumber: '900515/0123',
            _templates: {default: selfEditTemplate},
        });
        renderPage(createMockPageData(data));

        await user.click(screen.getByRole('button', {name: /zobrazit/i}));

        expect(screen.getByText('900515/0123')).toBeInTheDocument();
        expect(screen.queryByText('900515//0123')).not.toBeInTheDocument();
    });

    it('reveals birth number with slash inserted when backend returns without slash', async () => {
        const user = userEvent.setup();
        const data = mockMemberDetailData({
            nationality: 'CZ',
            birthNumber: '9005150123',
            _templates: {default: selfEditTemplate},
        });
        renderPage(createMockPageData(data));

        await user.click(screen.getByRole('button', {name: /zobrazit/i}));

        expect(screen.getByText('900515/0123')).toBeInTheDocument();
    });

    it('does NOT show birth number when nationality is not CZ (self view)', () => {
        const data = mockMemberDetailData({
            nationality: 'SK',
            birthNumber: '9003151234',
            _templates: {default: selfEditTemplate},
        });
        renderPage(createMockPageData(data));
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

    it('shows deactivation section when member is inactive even without deactivation note', () => {
        const data = mockMemberDetailData({
            active: false,
            deactivationReason: 'PRESTUP',
            deactivatedAt: '2025-06-15',
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
        expect(screen.getByText('Přestup')).toBeInTheDocument();
    });

    it('shows deactivation section when member is inactive even without deactivation reason', () => {
        const data = mockMemberDetailData({
            active: false,
            deactivationReason: undefined,
            deactivatedAt: '2025-06-15',
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
    });

    it('does NOT show deactivation section when member is active', () => {
        renderPage(createMockPageData(mockMemberDetailData({active: true})));
        expect(screen.queryByText('DEAKTIVACE')).not.toBeInTheDocument();
    });

    describe('other member view (no template)', () => {
        it('shows only contact and address sections, NOT personal info', () => {
            renderPage(createMockPageData(mockMemberDetailData()));
            expect(screen.getByText('KONTAKT')).toBeInTheDocument();
            expect(screen.getByText('ADRESA')).toBeInTheDocument();
            expect(screen.queryByText('OSOBNÍ ÚDAJE')).not.toBeInTheDocument();
            expect(screen.queryByText('DOPLŇKOVÉ INFORMACE')).not.toBeInTheDocument();
            expect(screen.queryByText('DOKLADY A LICENCE')).not.toBeInTheDocument();
        });

        it('shows no action buttons', () => {
            renderPage(createMockPageData(mockMemberDetailData()));
            expect(screen.queryByRole('button', {name: /upravit/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /ukončit/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /příspěvky/i})).not.toBeInTheDocument();
        });
    });

    describe('self profile view (template without firstName)', () => {
        it('shows all sections including personal info', () => {
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('OSOBNÍ ÚDAJE')).toBeInTheDocument();
            expect(screen.getByText('KONTAKT')).toBeInTheDocument();
            expect(screen.getByText('ADRESA')).toBeInTheDocument();
            expect(screen.getByText('DOPLŇKOVÉ INFORMACE')).toBeInTheDocument();
            expect(screen.getByText('DOKLADY A LICENCE')).toBeInTheDocument();
        });

        it('shows "Upravit profil" button with pencil icon', () => {
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
        });

        it('shows "Členské příspěvky" button', () => {
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /členské příspěvky/i})).toBeInTheDocument();
        });

        it('does NOT show "Ukončit členství" button', () => {
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('button', {name: /ukončit členství/i})).not.toBeInTheDocument();
        });
    });

    describe('admin view (template with firstName)', () => {
        it('shows all sections', () => {
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('OSOBNÍ ÚDAJE')).toBeInTheDocument();
            expect(screen.getByText('KONTAKT')).toBeInTheDocument();
            expect(screen.getByText('ADRESA')).toBeInTheDocument();
            expect(screen.getByText('DOPLŇKOVÉ INFORMACE')).toBeInTheDocument();
            expect(screen.getByText('DOKLADY A LICENCE')).toBeInTheDocument();
        });

        it('shows "Upravit profil" button', () => {
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
        });

        it('shows "Ukončit členství" button when terminate template exists', () => {
            const data = mockMemberDetailData({
                _templates: {
                    default: adminEditTemplate,
                    suspendMember: mockHalFormsTemplate({title: 'Terminate'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /ukončit členství/i})).toBeInTheDocument();
        });

        it('shows "Oprávnění" button when permissions link exists', () => {
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
                _links: {
                    self: {href: '/api/members/123'},
                    permissions: {href: '/api/members/123/permissions'},
                },
            });
            const pageData = createMockPageData(data, {
                hasLink: vi.fn((name: string) => name === 'permissions'),
            });
            renderPage(pageData);
            expect(screen.getByRole('button', {name: /oprávnění/i})).toBeInTheDocument();
        });

        it('does NOT show "Oprávnění" button when permissions link missing', () => {
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('button', {name: /oprávnění/i})).not.toBeInTheDocument();
        });
    });

    describe('admin edit mode', () => {
        it('shows admin badge "Admin — editace všech polí" when editing with admin template', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByText(/Admin — editace všech polí/i)).toBeInTheDocument();
        });

        it('shows action bar at bottom of content with "Uložit změny" and "Zrušit"', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /zrušit/i})).toBeInTheDocument();
        });

        it('switches fields to editable inputs when editing', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('Jan')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Novák')).toBeInTheDocument();
        });

        it('clicking "Zrušit" exits edit mode', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));
            expect(screen.getByDisplayValue('Jan')).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: /zrušit/i}));

            expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
        });

        it('readOnly fields stay read-only in edit mode', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getAllByText('SKI2601').length).toBeGreaterThanOrEqual(1);
            expect(screen.queryByDisplayValue('SKI2601')).not.toBeInTheDocument();
        });
    });

    describe('self edit mode', () => {
        it('does NOT show admin badge when editing with self template', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.queryByText(/Admin — editace všech polí/i)).not.toBeInTheDocument();
        });

        it('shows self-edit badge "Vlastní profil — omezená editace"', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByText(/Vlastní profil — omezená editace/i)).toBeInTheDocument();
        });

        it('fields not in self template stay read-only', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {default: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('jan.novak@email.cz')).toBeInTheDocument();
            expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();
        });
    });

    describe('2-column layout', () => {
        it('uses 2-column grid layout when template exists', () => {
            const data = mockMemberDetailData({
                _templates: {default: adminEditTemplate},
            });
            const {container} = renderPage(createMockPageData(data));
            const grid = container.querySelector('.grid.lg\\:grid-cols-2');
            expect(grid).toBeInTheDocument();
        });

        it('uses 1-column layout for other member view (no template)', () => {
            const {container} = renderPage(createMockPageData(mockMemberDetailData()));
            const grid = container.querySelector('.grid.lg\\:grid-cols-2');
            expect(grid).not.toBeInTheDocument();
        });
    });

    describe('legacy compatibility', () => {
        it('shows "Ukončit členství" button when terminate template exists (label overrides template title)', () => {
            const data = mockMemberDetailData({
                active: true,
                _templates: {
                    default: adminEditTemplate,
                    suspendMember: mockHalFormsTemplate({title: 'Terminate'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /ukončit členství/i})).toBeInTheDocument();
            expect(screen.queryByText('Terminate')).not.toBeInTheDocument();
        });

        it('renders address sub-fields as editable inputs when address is in template', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {
                    default: {
                        method: 'PUT' as const,
                        properties: [
                            {name: 'firstName', type: 'text'},
                            {name: 'address', type: 'AddressRequest'},
                        ],
                    },
                },
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('Hlavní 15')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Praha')).toBeInTheDocument();
            expect(screen.getByDisplayValue('11000')).toBeInTheDocument();
        });
    });
});
