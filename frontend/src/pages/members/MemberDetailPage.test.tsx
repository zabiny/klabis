import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {MemberDetailPage} from './MemberDetailPage';
import {vi} from 'vitest';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {labels} from '../../localization';

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
    };
});

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
    displayContact: {
        email: {value: 'jan.novak@email.cz'},
        phone: {value: '+420777123456'},
    },
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

const createMockPageData = (resourceData: HalResponse | null, overrides: Partial<ReturnType<typeof makePageData>> = {}) =>
    makePageData({
        resourceData,
        getResourceLink: vi.fn().mockReturnValue({href: '/api/members/123e4567-e89b-12d3-a456-426614174000'}),
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

    it.each([
        ['active', true, 'Aktivní'],
        ['inactive', false, 'Neaktivní'],
        ['absent from the response', undefined, null],
    ])('renders the active/inactive badge when member is %s', (_label, active, badge) => {
        renderPage(createMockPageData(mockMemberDetailData({active})));
        for (const text of ['Aktivní', 'Neaktivní']) {
            if (text === badge) {
                expect(screen.getByText(text)).toBeInTheDocument();
            } else {
                expect(screen.queryByText(text)).not.toBeInTheDocument();
            }
        }
    });

    it('shows contact section', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText('KONTAKT')).toBeInTheDocument();
        expect(screen.getByText('jan.novak@email.cz')).toBeInTheDocument();
        expect(screen.getByText('+420777123456')).toBeInTheDocument();
    });

    it('minor via guardian as admin shows marker with guardian name per field', () => {
        renderPage(createMockPageData(mockMemberDetailData({
            email: 'raw-should-not-show@email.cz',
            phone: undefined,
            displayContact: {
                email: {value: 'jan.zastupce@email.cz', fromGuardian: true, guardianName: 'Jan Novák'},
                phone: {value: '+420111222333', fromGuardian: true, guardianName: 'Eva Svobodová'},
            },
        })));
        expect(screen.getByText('jan.zastupce@email.cz')).toBeInTheDocument();
        expect(screen.getByText('+420111222333')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'kontakt zákonného zástupce (Jan Novák)'})).toBeInTheDocument();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'kontakt zákonného zástupce (Eva Svobodová)'})).toBeInTheDocument();
        expect(screen.queryByText('raw-should-not-show@email.cz')).not.toBeInTheDocument();
    });

    it('shows address section', () => {
        renderPage(createMockPageData(mockMemberDetailData()));
        expect(screen.getByText('ADRESA')).toBeInTheDocument();
        expect(screen.getByText('Hlavní 15')).toBeInTheDocument();
        expect(screen.getByText('Praha')).toBeInTheDocument();
        expect(screen.getByText('11000')).toBeInTheDocument();
    });

    describe('country names', () => {
        const withEditTemplate = {_templates: {updateMember: selfEditTemplateWithReservedFields}};

        it('shows nationality and address country by Czech name', () => {
            renderPage(createMockPageData(mockMemberDetailData({
                nationality: 'SK',
                address: {street: 'Hlavní 15', city: 'Praha', postalCode: '11000', country: 'CZ'},
                ...withEditTemplate,
            })));
            expect(screen.getByText('Slovensko')).toBeInTheDocument();
            expect(screen.getByText('Česko')).toBeInTheDocument();
            expect(screen.queryByText('SK')).not.toBeInTheDocument();
            expect(screen.queryByText('CZ')).not.toBeInTheDocument();
        });

        it('falls back to the raw code for an unknown country code', () => {
            renderPage(createMockPageData(mockMemberDetailData({
                nationality: 'ZZ1',
                address: {street: 'Hlavní 15', city: 'Praha', postalCode: '11000', country: 'XX1'},
                ...withEditTemplate,
            })));
            expect(screen.getByText('ZZ1')).toBeInTheDocument();
            expect(screen.getByText('XX1')).toBeInTheDocument();
        });
    });

    it('renders address section gracefully with a placeholder when member has no address (imported member)', () => {
        renderPage(createMockPageData(mockMemberDetailData({address: undefined})));
        expect(screen.getByText('ADRESA')).toBeInTheDocument();
        expect(screen.queryByText('Hlavní 15')).not.toBeInTheDocument();
    });

    describe('missing data warning', () => {
        it.each([
            ['empty', []],
            ['absent from the response', undefined],
        ])('shows no warning when missingData is %s', (_label, missingData) => {
            renderPage(createMockPageData(mockMemberDetailData({missingData})));
            expect(screen.queryByText(/^Chybí:/)).not.toBeInTheDocument();
        });

        it('shows "Chybí: rodné číslo, zákonný zástupce" for an incomplete member', () => {
            renderPage(createMockPageData(mockMemberDetailData({
                missingData: ['BIRTH_NUMBER', 'GUARDIAN'],
            })));
            expect(screen.getByText('Chybí: rodné číslo, zákonný zástupce')).toBeInTheDocument();
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

    it('does NOT show the legal guardians section without the link, even for a minor', () => {
        const minorBirthYear = new Date().getFullYear() - 10;
        renderPage(createMockPageData(mockMemberDetailData({dateOfBirth: `${minorBirthYear}-01-01`})));
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

    it.each([
        ['backend already includes the slash', '900515/0123'],
        ['backend returns it without the slash', '9005150123'],
    ])('reveals birth number as 900515/0123 when %s', async (_label, birthNumber) => {
        const user = userEvent.setup();
        renderPage(createMockPageData(mockMemberDetailData({
            nationality: 'CZ',
            birthNumber,
            _templates: {updateMember: selfEditTemplate},
        })));

        await user.click(screen.getByRole('button', {name: /zobrazit/i}));

        expect(screen.getByText('900515/0123')).toBeInTheDocument();
        expect(screen.queryByText('900515//0123')).not.toBeInTheDocument();
    });

    it.each([
        ['nationality is not CZ', 'SK', '9003151234'],
        ['nationality is CZ but birthNumber is null', 'CZ', null],
    ])('does NOT show birth number when %s (view mode)', (_label, nationality, birthNumber) => {
        renderPage(createMockPageData(mockMemberDetailData({
            nationality,
            birthNumber,
            _templates: {updateMember: selfEditTemplate},
        })));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    describe('deactivation section', () => {
        it('shows reason, note and the deactivating user for an inactive member', () => {
            const deactivatedByUuid = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
            renderPage(createMockPageData(mockMemberDetailData({
                active: false,
                suspensionReason: 'ODHLASKA',
                suspendedAt: '2025-06-15',
                suspensionNote: 'Osobní důvody',
                suspendedBy: deactivatedByUuid,
            })));
            expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
            expect(screen.getByText('Odhlášení')).toBeInTheDocument();
            expect(screen.getByText('Osobní důvody')).toBeInTheDocument();
            expect(screen.getByText('Deaktivoval/a')).toBeInTheDocument();
            expect(screen.getByText(deactivatedByUuid)).toBeInTheDocument();
        });

        it.each([
            ['inactive without note and deactivating user', {active: false, suspensionReason: 'PRESTUP', suspendedAt: '2025-06-15'}, true],
            ['inactive without reason', {active: false, suspensionReason: undefined, suspendedAt: '2025-06-15'}, true],
            ['active', {active: true}, false],
        ])('shows the section only for an inactive member (%s)', (_label, overrides, visible) => {
            renderPage(createMockPageData(mockMemberDetailData(overrides)));
            if (visible) {
                expect(screen.getByText('DEAKTIVACE')).toBeInTheDocument();
                expect(screen.queryByText('Deaktivoval/a')).not.toBeInTheDocument();
            } else {
                expect(screen.queryByText('DEAKTIVACE')).not.toBeInTheDocument();
            }
        });
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

        it('tells in the suspension form that a guardian with another guardian is removed from the guardians', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {
                    updateMember: adminEditTemplate,
                    suspendMember: mockHalFormsTemplate({method: 'POST', title: 'Terminate', target: '/api/members/m1/suspend', properties: []}),
                },
            });
            renderPage(createMockPageData(data));

            await user.click(screen.getByRole('button', {name: /ukončit členství/i}));

            expect(within(screen.getByRole('dialog')).getByText(labels.suspensionWarning.suspendFormGuardianNote)).toBeInTheDocument();
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

    describe('action buttons are shown iff the resource offers the link or template', () => {
        const selfHref = '/api/members/123e4567-e89b-12d3-a456-426614174000';
        const withLink = (rel: string, overrides: Record<string, unknown> = {}) => mockMemberDetailData({
            _links: {self: {href: selfHref}, [rel]: {href: `${selfHref}/${rel}`}},
            ...overrides,
        });

        it.each<[string, () => HalResponse, string | RegExp, boolean]>([
            ['Založit účet with the sendMemberAccountActivation template',
                () => mockMemberDetailData({_templates: {sendMemberAccountActivation: mockHalFormsTemplate({title: 'x'})}}), /založit účet/i, true],
            ['Založit účet without the template',
                () => mockMemberDetailData({_templates: {updateMember: adminEditTemplate}}), /založit účet/i, false],
            ['Oprávnění with the permissions link',
                () => withLink('permissions', {_templates: {updateMember: adminEditTemplate}}), /oprávnění/i, true],
            ['Oprávnění without the link',
                () => mockMemberDetailData({_templates: {updateMember: adminEditTemplate}}), /oprávnění/i, false],
            ['Změnit heslo with the changePassword link',
                () => withLink('changePassword'), /změnit heslo/i, true],
            ['Změnit heslo without the link',
                () => mockMemberDetailData(), /změnit heslo/i, false],
            ['Účet with the account link',
                () => withLink('account'), labels.finance.openMemberAccount, true],
            ['Účet without the link',
                () => mockMemberDetailData(), labels.finance.openMemberAccount, false],
            ['Členské příspěvky with the feeHistory link of an active member',
                () => withLink('feeHistory', {active: true}), 'Členské příspěvky', true],
            ['Členské příspěvky with the feeHistory link when active is absent',
                () => withLink('feeHistory', {active: undefined}), 'Členské příspěvky', true],
            ['Členské příspěvky without the link',
                () => mockMemberDetailData({active: true}), 'Členské příspěvky', false],
            ['Členské příspěvky for an inactive member',
                () => withLink('feeHistory', {active: false}), 'Členské příspěvky', false],
        ])('%s', (_title, buildData, name, visible) => {
            renderPage(createMockPageData(buildData()));
            const button = screen.queryByRole('button', {name});
            if (visible) {
                expect(button).toBeInTheDocument();
            } else {
                expect(button).not.toBeInTheDocument();
            }
        });

        it.each([
            ['the sync link is present', true],
            ['the sync link is absent', false],
        ])('mounts the sync status indicator only when %s', (_label, hasSync) => {
            renderPage(createMockPageData(hasSync ? withLink('sync') : mockMemberDetailData()));
            if (hasSync) {
                expect(screen.getByTestId('sync-error')).toBeInTheDocument();
            } else {
                expect(screen.queryByTestId('sync-error')).not.toBeInTheDocument();
            }
        });

        it('navigates to the fee history resource when clicked', async () => {
            const feeHistoryLink = {href: '/api/members/123/fee-history'};
            const data = mockMemberDetailData({
                active: true,
                _links: {
                    self: {href: '/api/members/123'},
                    feeHistory: feeHistoryLink,
                },
            });
            const pageData = createMockPageData(data, {
                getResourceLink: vi.fn().mockReturnValue(feeHistoryLink),
            });
            renderPage(pageData);
            await userEvent.click(screen.getByRole('button', {name: 'Členské příspěvky'}));
            expect(pageData.navigateToResource).toHaveBeenCalledWith(feeHistoryLink);
        });
    });

    describe('edit mode', () => {
        it('switches fields to editable inputs with the action bar and leaves edit mode on "Zrušit"', async () => {
            const user = userEvent.setup();
            const data = mockMemberDetailData({
                _templates: {updateMember: adminEditTemplate},
            });
            renderPage(createMockPageData(data));

            expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: /upravit profil/i}));

            expect(screen.getByDisplayValue('Jan')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Novák')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /uložit změny/i})).toBeInTheDocument();

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

                expect(screen.getByLabelText('E-mail')).toBeInTheDocument();
                expect(screen.queryByDisplayValue('Jan')).not.toBeInTheDocument();
                expect(screen.queryByDisplayValue('Novák')).not.toBeInTheDocument();
                for (const label of ['Jméno', 'Příjmení', 'Datum narození', 'Pohlaví', 'Rodné číslo']) {
                    expect(screen.queryByLabelText(label)).not.toBeInTheDocument();
                }
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

        describe('nationality marked readOnly or absent in the edit template', () => {
            const withNationalityReadOnly: HalFormsTemplate = {
                ...selfEditTemplateWithReservedFields,
                properties: [
                    ...selfEditTemplateWithReservedFields.properties!,
                    {name: 'nationality', type: 'text', prompt: 'Státní příslušnost', readOnly: true},
                ],
            };
            const withoutNationality: HalFormsTemplate = selfEditTemplateWithReservedFields;

            it.each([
                ['readOnly', withNationalityReadOnly],
                ['absent', withoutNationality],
            ])('shows nationality value without input and keeps birth number read-only when %s', async (_label, template) => {
                const user = userEvent.setup();
                renderPage(createMockPageData(mockMemberDetailData({
                    nationality: 'CZ',
                    birthNumber: '9003151234',
                    _templates: {updateMember: template},
                })));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.queryByLabelText('Státní příslušnost')).not.toBeInTheDocument();
                expect(screen.queryByDisplayValue('CZ')).not.toBeInTheDocument();
                expect(screen.getAllByText('Česko').length).toBeGreaterThan(0);
                expect(screen.queryByText('CZ')).not.toBeInTheDocument();
                expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
                expect(screen.queryByLabelText('Rodné číslo')).not.toBeInTheDocument();
            });

            it('hides birth number for a non-CZ nationality shown read-only', async () => {
                const user = userEvent.setup();
                renderPage(createMockPageData(mockMemberDetailData({
                    nationality: 'SK',
                    birthNumber: null,
                    _templates: {updateMember: withNationalityReadOnly},
                })));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));

                expect(screen.getByText('Slovensko')).toBeInTheDocument();
                expect(screen.queryByText('SK')).not.toBeInTheDocument();
                expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
            });

            it('omits nationality from the PATCH body', async () => {
                const user = userEvent.setup();
                const mutate = vi.fn();
                vi.mocked(useAuthorizedMutation).mockReturnValue({
                    mutate,
                    mutateAsync: vi.fn(),
                    isPending: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedMutation>);
                renderPage(createMockPageData(mockMemberDetailData({
                    _templates: {updateMember: withNationalityReadOnly},
                })));

                await user.click(screen.getByRole('button', {name: /upravit profil/i}));
                await user.click(screen.getByRole('button', {name: /uložit změny/i}));

                await vi.waitFor(() => expect(mutate).toHaveBeenCalled());
                const body = mutate.mock.calls[0][0].data as Record<string, unknown>;
                expect(body).not.toHaveProperty('nationality');
                expect(body).not.toHaveProperty('birthNumber');
            });
        });

        describe('admin edit template', () => {
            it('shows a gender select and sends reserved fields in the body when they are editable', async () => {
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
                expect(screen.getByRole('combobox', {name: 'Pohlaví'})).toHaveValue('MALE');
                await user.click(screen.getByRole('button', {name: /uložit změny/i}));

                await vi.waitFor(() => expect(mutate).toHaveBeenCalled());
                const body = mutate.mock.calls[0][0].data as Record<string, unknown>;
                expect(body).toMatchObject({firstName: 'Jan', lastName: 'Novák', gender: 'MALE'});
            });
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
    });

    describe('holder of profile editing over another member (legal guardian of a minor)', () => {
        const minorId = '223e4567-e89b-12d3-a456-426614174000';
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

        it('offers editing and lists the guardians without the "Upravit zástupce" action', () => {
            renderPage(createMockPageData(mockMemberDetailData({
                id: minorId,
                firstName: 'Sofie',
                lastName: 'Svobodová',
                dateOfBirth: '2015-05-01',
                _links: {
                    self: {href: `/api/members/${minorId}`},
                    legalGuardians: {href: '/api/legal-guardian-groups/g-1/guardians'},
                },
                _templates: {updateMember: selfEditTemplateWithReservedFields},
            })));
            expect(screen.getByRole('button', {name: /upravit profil/i})).toBeInTheDocument();
            expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Eva Svobodová'})).toBeInTheDocument();
            expect(screen.getByText('eva@example.com')).toBeInTheDocument();
            expect(screen.getByText('+420777000111')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
        });
    });
});
