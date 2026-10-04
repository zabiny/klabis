import '@testing-library/jest-dom';
import React from 'react';
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
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

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

vi.mock('../../hooks/useHalFormGetAvailability', () => ({
    useHalFormGetAvailability: vi.fn(() => ({
        isGetAllowed: false,
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
    ToastProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/toastContext', () => ({
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

vi.mock('../../api/hateoas', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../api/hateoas')>();
    return {
        ...actual,
        submitHalFormsData: vi.fn(),
        isFormValidationError: vi.fn((error) => {
            return error && typeof error === 'object' && 'validationErrors' in error;
        }),
        toFormValidationError: vi.fn((error) => error),
    };
});

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: {isOpen: boolean; children: React.ReactNode; onClose: () => void; title?: string}) => (
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
        {name: 'phone', type: 'tel', prompt: 'Telefon'},
        {name: 'registrationNumber', type: 'text', readOnly: true},
    ],
};

const adminEditTemplateWithNationality: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno'},
        {name: 'lastName', type: 'text', prompt: 'Příjmení'},
        {name: 'nationality', type: 'text', prompt: 'Státní příslušnost'},
        {name: 'birthNumber', type: 'text', prompt: 'Rodné číslo'},
        {name: 'email', type: 'email', prompt: 'E-mail'},
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

const selfEditTemplateWithReservedFields: HalFormsTemplate = {
    method: 'PATCH',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno', readOnly: true},
        {name: 'lastName', type: 'text', prompt: 'Příjmení', readOnly: true},
        {name: 'dateOfBirth', type: 'date', prompt: 'Datum narození', readOnly: true},
        {name: 'gender', type: 'Gender', prompt: 'Pohlaví', readOnly: true, options: {inline: ['MALE', 'FEMALE']}},
        {name: 'birthNumber', type: 'text', prompt: 'Rodné číslo', readOnly: true},
        {name: 'email', type: 'email', prompt: 'E-mail'},
        {name: 'phone', type: 'tel', prompt: 'Telefon'},
    ],
};

const adminEditTemplateWithGender: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/123e4567-e89b-12d3-a456-426614174000',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno'},
        {name: 'lastName', type: 'text', prompt: 'Příjmení'},
        {name: 'gender', type: 'Gender', prompt: 'Pohlaví', options: {inline: ['MALE', 'FEMALE']}},
        {name: 'email', type: 'email', prompt: 'E-mail'},
    ],
};

const mockMemberDetailData = (overrides?: Partial<HalResponse & Record<string, unknown>>): HalResponse => ({
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

const createMockPageData = (resourceData: HalResponse | null, overrides?: Partial<ReturnType<typeof useHalPageData>>) => ({
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

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
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

    it('shows no active/inactive badge when caller lacks the field (active absent from response)', () => {
        const data = mockMemberDetailData();
        delete (data as Record<string, unknown>).active;
        renderPage(createMockPageData(data));
        expect(screen.queryByText('Aktivní')).not.toBeInTheDocument();
        expect(screen.queryByText('Neaktivní')).not.toBeInTheDocument();
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

    it('renders address section gracefully with a placeholder when member has no address (imported member)', () => {
        renderPage(createMockPageData(mockMemberDetailData({address: undefined})));
        expect(screen.getByText('ADRESA')).toBeInTheDocument();
        expect(screen.queryByText('Hlavní 15')).not.toBeInTheDocument();
    });

    describe('missing data warning', () => {
        it('shows no warning for a complete member (missingData empty or absent)', () => {
            renderPage(createMockPageData(mockMemberDetailData({missingData: []})));
            expect(screen.queryByText(/^Chybí:/)).not.toBeInTheDocument();
        });

        it('shows "Chybí: rodné číslo, zákonný zástupce" for an incomplete member', () => {
            renderPage(createMockPageData(mockMemberDetailData({
                missingData: ['BIRTH_NUMBER', 'GUARDIAN'],
            })));
            expect(screen.getByText('Chybí: rodné číslo, zákonný zástupce')).toBeInTheDocument();
        });

        it('shows no warning when caller lacks the field (missingData absent from response)', () => {
            const data = mockMemberDetailData();
            delete (data as Record<string, unknown>).missingData;
            renderPage(createMockPageData(data));
            expect(screen.queryByText(/^Chybí:/)).not.toBeInTheDocument();
        });
    });

    it('shows the legal guardians section for a member with the legalGuardians link', () => {
        const data = mockMemberDetailData({
            _links: {
                self: {href: '/api/members/1'},
                legalGuardians: {href: '/api/legal-guardian-groups/g-1/guardians'},
            },
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
    });

    it('does NOT show the legal guardians section for an adult without the link', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.queryByText('ZÁKONNÍ ZÁSTUPCI')).not.toBeInTheDocument();
    });

    it('shows an empty legal guardians section for a minor without a group (template offered by backend)', () => {
        renderPage(createMockPageData(mockMemberDetailData({
            _templates: {
                setMemberLegalGuardians: mockHalFormsTemplate({method: 'PUT', target: '/api/members/1/legal-guardians'}),
            },
        })));
        expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
        expect(screen.getByText('Bez zákonného zástupce')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upravit zástupce'})).toBeInTheDocument();
    });

    it('does not derive the guardians section from the date of birth', () => {
        const year = new Date().getFullYear() - 10;
        renderPage(createMockPageData(mockMemberDetailData({dateOfBirth: `${year}-01-01`})));
        expect(screen.queryByText('ZÁKONNÍ ZÁSTUPCI')).not.toBeInTheDocument();
    });

    it('offers "Upravit zástupce" only with the setMemberLegalGuardians template', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
    });

    it('shows birth number masked when nationality is CZ (self view)', () => {
        const data = mockMemberDetailData({
            nationality: 'CZ',
            birthNumber: '9003151234',
            _templates: {updateMember: selfEditTemplate},
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
            _templates: {updateMember: selfEditTemplate},
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
            _templates: {updateMember: selfEditTemplate},
        });
        renderPage(createMockPageData(data));

        await user.click(screen.getByRole('button', {name: /zobrazit/i}));

        expect(screen.getByText('900515/0123')).toBeInTheDocument();
    });

    it('does NOT show birth number when nationality is not CZ (self view)', () => {
        const data = mockMemberDetailData({
            nationality: 'SK',
            birthNumber: '9003151234',
            _templates: {updateMember: selfEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    it('does NOT show birth number when nationality is CZ but birthNumber is null (view mode)', () => {
        const data = mockMemberDetailData({
            nationality: 'CZ',
            birthNumber: null,
            _templates: {updateMember: selfEditTemplate},
        });
        renderPage(createMockPageData(data));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    it('shows deactivation section when member is inactive', () => {
        const data = mockMemberDetailData({
            active: false,
            suspensionReason: 'ODHLASKA',
            suspendedAt: '2025-06-15',
            suspensionNote: 'Osobní důvody',
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
        expect(screen.getByText('Odhlášení')).toBeInTheDocument();
        expect(screen.getByText('Osobní důvody')).toBeInTheDocument();
    });

    it('shows deactivatedBy when present for inactive member', () => {
        const deactivatedByUuid = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
        const data = mockMemberDetailData({
            active: false,
            suspendedAt: '2025-06-15',
            suspendedBy: deactivatedByUuid,
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('Deaktivoval/a')).toBeInTheDocument();
        expect(screen.getByText(deactivatedByUuid)).toBeInTheDocument();
    });

    it('does NOT show deactivatedBy label when deactivatedBy is absent', () => {
        const data = mockMemberDetailData({
            active: false,
            suspendedAt: '2025-06-15',
            suspendedBy: undefined,
        });
        renderPage(createMockPageData(data));
        expect(screen.queryByText('Deaktivoval/a')).not.toBeInTheDocument();
    });

    it('shows deactivation section when member is inactive even without deactivation note', () => {
        const data = mockMemberDetailData({
            active: false,
            suspensionReason: 'PRESTUP',
            suspendedAt: '2025-06-15',
        });
        renderPage(createMockPageData(data));
        expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
        expect(screen.getByText('Přestup')).toBeInTheDocument();
    });

    it('shows deactivation section when member is inactive even without deactivation reason', () => {
        const data = mockMemberDetailData({
            active: false,
            suspensionReason: undefined,
            suspendedAt: '2025-06-15',
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

    describe('view with edit template (self or admin)', () => {
        it('shows all sections including personal info', () => {
            const data = mockMemberDetailData({
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByText('OSOBNÍ ÚDAJE')).toBeInTheDocument();
            expect(screen.getByText('KONTAKT')).toBeInTheDocument();
            expect(screen.getByText('ADRESA')).toBeInTheDocument();
            expect(screen.getByText('DOPLŇKOVÉ INFORMACE')).toBeInTheDocument();
            expect(screen.getByText('DOKLADY A LICENCE')).toBeInTheDocument();
        });

        it('shows "Upravit profil" button when template exists', () => {
            const data = mockMemberDetailData({
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
        });

        it('shows "Ukončit členství" button when suspendMember template exists', () => {
            const data = mockMemberDetailData({
                _templates: {
                    updateMember: adminEditTemplate,
                    suspendMember: mockHalFormsTemplate({title: 'Terminate'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /ukončit členství/i})).toBeInTheDocument();
        });

        it('shows "Založit účet" button and confirmation dialog when sendMemberAccountActivation template exists', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {
                    sendMemberAccountActivation: mockHalFormsTemplate({
                        title: 'sendMemberAccountActivation',
                        target: '/api/members/123/account-activation',
                    }),
                },
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /založit účet/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Založení účtu')).toBeInTheDocument();
            expect(screen.getByText(/aktivační odkaz/i)).toBeInTheDocument();
        });

        it('does NOT show "Založit účet" button without sendMemberAccountActivation template', () => {
            const data = mockMemberDetailData({_templates: {updateMember: adminEditTemplate}});
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('button', {name: /založit účet/i})).not.toBeInTheDocument();
        });

        it('shows "Oprávnění" button when permissions link exists', () => {
            const data = mockMemberDetailData({
                _templates: {updateMember: adminEditTemplate},
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
                _templates: {updateMember: adminEditTemplate},
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('button', {name: /oprávnění/i})).not.toBeInTheDocument();
        });

        it('shows "Reaktivace člena" as dialog title when resumeMember button is clicked', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {
                    updateMember: adminEditTemplate,
                    resumeMember: mockHalFormsTemplate({title: 'resumeMember'}),
                },
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /reaktivovat/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Reaktivace člena')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('shows action bar at bottom of content with "Uložit změny" and "Zrušit"', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {updateMember: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /zrušit/i})).toBeInTheDocument();
        });

        it('switches fields to editable inputs when editing with admin template', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {updateMember: adminEditTemplate},
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
                _templates: {updateMember: adminEditTemplate},
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
                _templates: {updateMember: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getAllByText('SKI2601').length).toBeGreaterThanOrEqual(1);
            expect(screen.queryByDisplayValue('SKI2601')).not.toBeInTheDocument();
        });

        it('fields not in template stay read-only when editing with self template', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('jan.novak@email.cz')).toBeInTheDocument();
            expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();
        });

        it('shows localized gender label "Muž" instead of raw "MALE" when editing', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                gender: 'MALE',
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByText('Muž')).toBeInTheDocument();
            expect(screen.queryByText('MALE')).not.toBeInTheDocument();
        });

        it('shows localized gender label "Žena" instead of raw "FEMALE" when editing', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                gender: 'FEMALE',
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByText('Žena')).toBeInTheDocument();
            expect(screen.queryByText('FEMALE')).not.toBeInTheDocument();
        });

        it('shows a gender select when the edit template includes gender (admin)', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                gender: 'MALE',
                _templates: {updateMember: adminEditTemplateWithGender},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            const genderSelect = document.querySelector('select[name="gender"]');
            expect(genderSelect).toBeInTheDocument();
            expect(genderSelect).toHaveValue('MALE');
        });

        describe('self-edit template with reserved fields marked readOnly', () => {
            const selfData = () => mockMemberDetailData({
                gender: 'FEMALE',
                birthNumber: '9003151234',
                _templates: {updateMember: selfEditTemplateWithReservedFields},
            });

            it('renders the five reserved fields without inputs', async () => {
                const user = userEvent.setup();
                renderPage(createMockPageData(selfData()));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();
                expect(screen.queryByDisplayValue('Novák')).not.toBeInTheDocument();
                expect(document.querySelector('[name="firstName"]')).not.toBeInTheDocument();
                expect(document.querySelector('[name="lastName"]')).not.toBeInTheDocument();
                expect(document.querySelector('[name="dateOfBirth"]')).not.toBeInTheDocument();
                expect(document.querySelector('[name="gender"]')).not.toBeInTheDocument();
                expect(document.querySelector('[name="birthNumber"]')).not.toBeInTheDocument();
            });

            it('still displays the reserved values, localized, while editing', async () => {
                const user = userEvent.setup();
                renderPage(createMockPageData(selfData()));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.getByText('Jan')).toBeInTheDocument();
                expect(screen.getByText('Novák')).toBeInTheDocument();
                expect(screen.getByText('Žena')).toBeInTheDocument();
                expect(screen.getByText('15. 3. 1990')).toBeInTheDocument();
                expect(screen.queryByText('FEMALE')).not.toBeInTheDocument();
            });

            it('keeps the non-reserved fields editable', async () => {
                const user = userEvent.setup();
                renderPage(createMockPageData(selfData()));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.getByDisplayValue('jan.novak@email.cz')).toBeInTheDocument();
                expect(screen.getByDisplayValue('+420777123456')).toBeInTheDocument();
            });

            it('omits the reserved fields from the PATCH body', async () => {
                const user = userEvent.setup();
                const mutate = vi.fn();
                vi.mocked(useAuthorizedMutation).mockReturnValue({
                    mutate,
                    mutateAsync: vi.fn(),
                    isPending: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedMutation>);
                renderPage(createMockPageData(selfData()));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));
                const phone = screen.getByDisplayValue('+420777123456');
                await user.clear(phone);
                await user.type(phone, '+420111222333');
                await user.click(screen.getByRole('button', {name: /uložit změny/i}));

                await vi.waitFor(() => expect(mutate).toHaveBeenCalled());
                const body = mutate.mock.calls[0][0].data as Record<string, unknown>;
                expect(body.phone).toBe('+420111222333');
                for (const reserved of ['firstName', 'lastName', 'dateOfBirth', 'gender', 'birthNumber']) {
                    expect(body).not.toHaveProperty(reserved);
                }
            });
        });

        describe('admin edit template', () => {
            it('sends reserved fields in the body when they are editable', async () => {
                const user = userEvent.setup();
                const mutate = vi.fn();
                vi.mocked(useAuthorizedMutation).mockReturnValue({
                    mutate,
                    mutateAsync: vi.fn(),
                    isPending: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedMutation>);
                renderPage(createMockPageData(mockMemberDetailData({
                    gender: 'MALE',
                    _templates: {updateMember: adminEditTemplateWithGender},
                })));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));
                await user.click(screen.getByRole('button', {name: /uložit změny/i}));

                await vi.waitFor(() => expect(mutate).toHaveBeenCalled());
                const body = mutate.mock.calls[0][0].data as Record<string, unknown>;
                expect(body).toMatchObject({firstName: 'Jan', lastName: 'Novák', gender: 'MALE'});
            });
        });

        describe('birth number conditional on nationality in edit mode', () => {
            it('shows birth number input when nationality is CZ in edit mode', async () => {
                const user = userEvent.setup();
                const data = mockMemberDetailData({
                    nationality: 'CZ',
                    birthNumber: '9003151234',
                    _templates: {updateMember: adminEditTemplateWithNationality},
                });
                renderPage(createMockPageData(data));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
            });

            it('hides birth number input when nationality is non-CZ in edit mode', async () => {
                const user = userEvent.setup();
                const data = mockMemberDetailData({
                    nationality: 'SK',
                    birthNumber: null,
                    _templates: {updateMember: adminEditTemplateWithNationality},
                });
                renderPage(createMockPageData(data));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
            });

            it('hides birth number field and clears value when nationality changes from CZ to non-CZ', async () => {
                const user = userEvent.setup();
                const data = mockMemberDetailData({
                    nationality: 'CZ',
                    birthNumber: '9003151234',
                    _templates: {updateMember: adminEditTemplateWithNationality},
                });
                renderPage(createMockPageData(data));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
                expect(screen.getByDisplayValue('9003151234')).toBeInTheDocument();

                const nationalityInput = screen.getByDisplayValue('CZ');
                await user.clear(nationalityInput);
                await user.type(nationalityInput, 'SK');

                expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
                expect(screen.queryByDisplayValue('9003151234')).not.toBeInTheDocument();
            });

            it('shows birth number field when nationality changes from non-CZ to CZ', async () => {
                const user = userEvent.setup();
                const data = mockMemberDetailData({
                    nationality: 'SK',
                    birthNumber: null,
                    _templates: {updateMember: adminEditTemplateWithNationality},
                });
                renderPage(createMockPageData(data));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();

                const nationalityInput = screen.getByDisplayValue('SK');
                await user.clear(nationalityInput);
                await user.type(nationalityInput, 'CZ');

                expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
            });
        });
    });

    describe('holder of profile editing over another member (legal guardian of a minor)', () => {
        const minorId = '223e4567-e89b-12d3-a456-426614174000';
        const holderLinks = {
            self: {href: `/api/members/${minorId}`},
            legalGuardians: {href: '/api/legal-guardian-groups/g-1/guardians'},
        };
        const holderData = () => mockMemberDetailData({
            id: minorId,
            firstName: 'Sofie',
            lastName: 'Svobodová',
            dateOfBirth: '2015-05-01',
            _links: holderLinks,
            _templates: {updateMember: selfEditTemplateWithReservedFields},
        });
        const guardiansResponse = {
            _embedded: {
                legalGuardianGroupGuardianResponseList: [{
                    userId: 'u-eva',
                    firstName: 'Eva',
                    lastName: 'Svobodová',
                    email: 'eva@example.com',
                    phone: '+420777000111',
                    _links: {member: {href: '/api/members/eva'}},
                }],
            },
        };

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockImplementation(((url: string) => ({
                data: url.includes('/guardians') ? guardiansResponse : undefined,
                isLoading: false,
                error: null,
            })) as unknown as typeof useAuthorizedQuery);
        });

        afterEach(() => {
            vi.mocked(useAuthorizedQuery).mockImplementation((() => ({
                data: undefined,
                isLoading: false,
                error: null,
            })) as unknown as typeof useAuthorizedQuery);
        });

        it('shows the edit button and the full two-column layout driven by the update template', () => {
            const {container} = renderPage(createMockPageData(holderData()));
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
            expect(container.querySelector('.grid.lg\\:grid-cols-2')).toBeInTheDocument();
            expect(screen.getByText('OSOBNÍ ÚDAJE')).toBeInTheDocument();
        });

        it('shows no fee, permissions, suspension or account actions', () => {
            renderPage(createMockPageData(holderData()));
            expect(screen.queryByRole('button', {name: /členské příspěvky/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('heading', {name: /Členský příspěvek/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /oprávnění/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /ukončit členství/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /založit účet/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /změnit heslo/i})).not.toBeInTheDocument();
        });

        it('lists the guardians without the "Upravit zástupce" action', () => {
            renderPage(createMockPageData(holderData()));
            expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Eva Svobodová'})).toBeInTheDocument();
            expect(screen.getByText('eva@example.com')).toBeInTheDocument();
            expect(screen.getByText('+420777000111')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
        });

        it('shows the birth number of the minor masked', () => {
            const data = holderData();
            data.birthNumber = '1505010012';
            renderPage(createMockPageData(data));
            expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
        });

        it('keeps reserved fields read-only and editable fields editable in the edit form', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(holderData()));
            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('+420777123456')).toBeInTheDocument();
            expect(document.querySelector('[name="firstName"]')).not.toBeInTheDocument();
            expect(document.querySelector('[name="dateOfBirth"]')).not.toBeInTheDocument();
            expect(document.querySelector('[name="birthNumber"]')).not.toBeInTheDocument();
            expect(screen.getAllByText('Sofie').length).toBeGreaterThan(0);
        });
    });

    describe('2-column layout', () => {
        it('uses 2-column grid layout when template exists', () => {
            const data = mockMemberDetailData({
                _templates: {updateMember: adminEditTemplate},
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

    describe('fee section', () => {
        it('shows MemberFeeSection when feeSummary link is present and member is active', () => {
            const data = mockMemberDetailData({
                active: true,
                _links: {
                    self: {href: '/api/members/123'},
                    feeSummary: {href: '/api/members/123/fee-summary/2026'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
        });

        it('does NOT show MemberFeeSection when feeSummary link is absent (admin viewing another member)', () => {
            const data = mockMemberDetailData({
                active: true,
                _links: {
                    self: {href: '/api/members/123'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('heading', {name: /Členský příspěvek/i})).not.toBeInTheDocument();
        });

        it('does NOT show MemberFeeSection when member is inactive even with feeSummary link', () => {
            const data = mockMemberDetailData({
                active: false,
                _links: {
                    self: {href: '/api/members/123'},
                    feeSummary: {href: '/api/members/123/fee-summary/2026'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('heading', {name: /Členský příspěvek/i})).not.toBeInTheDocument();
        });

        it('shows MemberFeeSection for regular user without MEMBERS_MANAGE (active absent from response) when feeSummary link is present', () => {
            const data = mockMemberDetailData({
                _links: {
                    self: {href: '/api/members/123'},
                    feeSummary: {href: '/api/members/123/fee-summary/2026'},
                },
            });
            delete (data as Record<string, unknown>).active;
            renderPage(createMockPageData(data));
            expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
        });
    });

    describe('calendar feed section', () => {
        it('shows CalendarFeedSection when ical-token link is present on member detail resource', () => {
            const data = mockMemberDetailData({
                _links: {
                    self: {href: '/api/members/123'},
                    'ical-token': {href: '/api/me/ical-token'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('heading', {name: /Kalendářový feed/i})).toBeInTheDocument();
        });

        it('does NOT show CalendarFeedSection when ical-token link is absent on member detail resource', () => {
            const data = mockMemberDetailData({
                _links: {
                    self: {href: '/api/members/123'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('heading', {name: /Kalendářový feed/i})).not.toBeInTheDocument();
        });

        it('does NOT show CalendarFeedSection in edit mode even when ical-token link is present', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _links: {
                    self: {href: '/api/members/123'},
                    'ical-token': {href: '/api/me/ical-token'},
                },
                _templates: {updateMember: selfEditTemplate},
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.queryByRole('heading', {name: /Kalendářový feed/i})).not.toBeInTheDocument();
        });
    });

    describe('legacy compatibility', () => {
        it('shows "Ukončit členství" button when terminate template exists (label overrides template title)', () => {
            const data = mockMemberDetailData({
                active: true,
                _templates: {
                    updateMember: adminEditTemplate,
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
                    updateMember: {
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

    describe('sync status indicator (4.4)', () => {
        it('renders SyncStatusIndicator when member._links.sync is present', () => {
            const data = mockMemberDetailData({
                _links: {
                    self: {href: '/api/members/123e4567-e89b-12d3-a456-426614174000'},
                    sync: {href: '/api/members/123e4567-e89b-12d3-a456-426614174000/sync'},
                },
            });
            renderPage(createMockPageData(data));

            // Behavioural coverage of SyncStatusIndicator lives in SyncStatusIndicator.test.tsx;
            // here we only assert the indicator is mounted when the sync link is present.
            expect(screen.getByTestId('sync-error')).toBeInTheDocument();
        });

        it('does not render SyncStatusIndicator when member._links.sync is absent', () => {
            renderPage(createMockPageData(mockMemberDetailData()));

            expect(screen.queryByTestId('sync-error')).not.toBeInTheDocument();
            expect(screen.queryByTestId('sync-loading')).not.toBeInTheDocument();
            expect(screen.queryByTestId(/^sync-status-/)).not.toBeInTheDocument();
        });
    });
});
