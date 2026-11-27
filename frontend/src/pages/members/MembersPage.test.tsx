import '@testing-library/jest-dom';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {Link, MemoryRouter, useLocation} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {
    buildMemberRow,
    manageMembersResource,
    MEMBERS_URL,
    membersCollection,
    plainMembersResource,
} from '../../__mocks__/membersPageTest';
import {MembersPage} from './MembersPage';
import {vi} from 'vitest';
import {act} from '@testing-library/react';
import {useHalForm} from '../../contexts/halFormContext.ts';
import type {HalResponse} from '../../api';
import {labels} from '../../localization';
import userEvent from '@testing-library/user-event';

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

vi.mock('../../components/members/PermissionsDialog', () => ({
    PermissionsDialog: ({isOpen, memberName}: {isOpen: boolean; memberName: string}) =>
        isOpen ? <div data-testid="permissions-dialog">{memberName}</div> : null,
}));

vi.mock('../../hooks/useHalFormData', () => ({
    useHalFormData: (_template: unknown, resourceData: Record<string, unknown>) => ({
        formData: resourceData ?? {},
        isLoadingTargetData: false,
        targetFetchError: null,
        refetchTargetData: vi.fn(),
    }),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../hooks/usePermissionsEditor', () => ({
    usePermissionsEditor: vi.fn().mockReturnValue({
        permissions: undefined,
        isLoading: false,
        onSave: vi.fn(),
        isSaving: false,
        error: null,
    }),
}));

vi.mock('../../components/finance/FinanceTransactionDialog', () => ({
    FinanceTransactionDialog: ({isOpen, accountLink, onClose}: {isOpen: boolean; accountLink: {href: string}; onClose: () => void}) =>
        isOpen ? (
            <div data-testid="finance-transaction-dialog" data-account-href={accountLink.href}>
                <button onClick={onClose}>close-dialog</button>
            </div>
        ) : null,
}));

const createMockPageData = (resourceData: HalResponse | null, overrides: Partial<ReturnType<typeof makePageData>> = {}) =>
    makePageData({
        resourceData,
        getResourceLink: vi.fn().mockReturnValue({href: MEMBERS_URL}),
        ...overrides,
    });

const queryReturning = (data: HalResponse | undefined) =>
    vi.mocked(useAuthorizedQuery).mockReturnValue({data, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);

const renderPage = (pageData: ReturnType<typeof createMockPageData>, initialUrl = '/members') => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialUrl]}>
                <Link to="/members">menu-members</Link>
                <ToastProvider>
                    <MembersPage/>
                </ToastProvider>
                <SearchReader/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const SearchReader = () => {
    const location = useLocation();
    return <span data-testid="search-string">{location.search || '(none)'}</span>;
};

const renderPageWithMembers = (members: unknown[], routeOverrides?: Partial<ReturnType<typeof makePageData>>) => {
    const resourceData = membersCollection(members);
    queryReturning(resourceData);
    return renderPage(createMockPageData(resourceData, routeOverrides));
};

const requestedUrlsOf = () => vi.mocked(useAuthorizedQuery).mock.calls.map(([url]) => String(url));

describe('MembersPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Členové"', () => {
        renderPage(createMockPageData(plainMembersResource));
        expect(screen.getByText('Členové')).toBeInTheDocument();
    });

    it.each([
        ['Registrační číslo'],
        ['Příjmení'],
        ['Jméno'],
        ['Kontakt'],
        ['Akce'],
    ])('renders column header "%s"', (header) => {
        renderPage(createMockPageData(plainMembersResource));
        expect(screen.getByText(header)).toBeInTheDocument();
    });

    const onlyDefaultTemplate: HalResponse = {
        _links: {self: {href: '/api/members'}},
        _templates: {default: mockHalFormsTemplate({title: 'Get Members', method: 'PATCH'})},
    };

    it.each([
        {
            button: 'registerMember',
            label: 'Registrovat člena',
            template: mockHalFormsTemplate({title: 'Registrovat člena', method: 'POST'}),
        },
        {
            button: 'importFromOris',
            label: 'Synchronizovat s ORIS',
            template: mockHalFormsTemplate({
                title: 'Synchronizovat s ORIS',
                method: 'POST',
                target: 'http://localhost/api/members/oris-import',
                properties: [],
            }),
        },
    ])('toolbar button "$label" is shown iff the $button template is offered', ({button, label, template}) => {
        const withTemplate: HalResponse = {
            ...onlyDefaultTemplate,
            _templates: {...onlyDefaultTemplate._templates, [button]: template},
        };

        const {unmount} = renderPage(createMockPageData(withTemplate));
        expect(screen.getByTestId(`form-template-button-${button}`)).toBeInTheDocument();
        expect(screen.getByText(label)).toBeInTheDocument();
        unmount();

        renderPage(createMockPageData(onlyDefaultTemplate));
        expect(screen.queryByTestId(`form-template-button-${button}`)).not.toBeInTheDocument();
    });
});

describe('MembersPage — row data rendering', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows email and phone from displayContact in the contact column', () => {
        renderPageWithMembers([buildMemberRow({
            displayContact: {email: {value: 'test@example.com'}, phone: {value: '+420123456789'}},
        })]);
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
        expect(screen.getByText('+420123456789')).toBeInTheDocument();
    });

    it('shows guardian marker next to value taken from guardian', () => {
        renderPageWithMembers([buildMemberRow({
            displayContact: {
                email: {value: 'rodic@example.com', fromGuardian: true, guardianName: 'Jana Nováková'},
                phone: {value: '+420111222333'},
            },
        })]);
        expect(screen.getByText('rodic@example.com')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: labels.ui.guardianContact('Jana Nováková')})).toHaveLength(1);
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('does not render contact column when displayContact is absent', () => {
        renderPageWithMembers([buildMemberRow({email: undefined})]);
        expect(screen.queryByText(labels.fields.contact)).not.toBeInTheDocument();
    });

    const rowAffordances = [
        {
            icon: 'edit',
            affordance: '_templates.updateMember',
            row: () => buildMemberRow({_templates: {updateMember: mockHalFormsTemplate({method: 'PUT'})}}),
            query: () => screen.queryByRole('button', {name: /upravit/i}),
        },
        {
            icon: 'shield',
            affordance: '_links.permissions',
            row: () => buildMemberRow({
                _links: {self: {href: '/api/members/member-1'}, permissions: {href: '/api/members/member-1/permissions'}},
            }),
            query: () => screen.queryByRole('button', {name: /oprávnění/i}),
        },
        {
            icon: 'suspend',
            affordance: '_templates.suspendMember',
            row: () => buildMemberRow({_templates: {suspendMember: mockHalFormsTemplate({method: 'POST'})}}),
            query: () => screen.queryByRole('button', {name: /ukončit členství/i}),
        },
        {
            icon: 'resume',
            affordance: '_templates.resumeMember',
            row: () => buildMemberRow({_templates: {resumeMember: mockHalFormsTemplate({method: 'POST'})}}),
            query: () => screen.queryByRole('button', {name: /reaktivovat/i}),
        },
        {
            icon: 'banknote',
            affordance: '_links.account',
            row: () => buildMemberRow({
                _links: {self: {href: '/api/members/member-1'}, account: {href: '/api/members/member-1/account'}},
            }),
            query: () => screen.queryByRole('button', {name: labels.finance.openTransactionDialogAriaLabel}),
        },
        {
            icon: 'sync indicator',
            affordance: '_links.sync',
            row: () => buildMemberRow({
                _links: {self: {href: '/api/members/member-1'}, sync: {href: '/api/members/member-1/sync'}},
            }),
            query: () => screen.queryByTestId('sync-error'),
        },
    ];

    it.each(rowAffordances)('shows $icon icon when $affordance exists on the member row', ({row, query}) => {
        renderPageWithMembers([row()]);
        expect(query()).toBeInTheDocument();
    });

    it.each(rowAffordances)('does not show $icon icon when $affordance is absent on the member row', ({query}) => {
        renderPageWithMembers([buildMemberRow()]);
        expect(query()).not.toBeInTheDocument();
    });

    describe('suspend / resume action dialog', () => {
        const suspendRow = () => buildMemberRow({
            _templates: {suspendMember: mockHalFormsTemplate({method: 'POST', title: 'Ukončit členství člena', properties: []})},
        });

        it.each([
            {action: 'suspend', button: /ukončit členství/i, template: 'suspendMember', title: 'Ukončit členství člena'},
            {action: 'resume', button: /reaktivovat/i, template: 'resumeMember', title: 'Reaktivace člena'},
        ])('opens $action dialog titled by the template title', ({button, template, title}) => {
            renderPageWithMembers([buildMemberRow({
                _templates: {[template]: mockHalFormsTemplate({method: 'POST', title, properties: []})},
            })]);
            fireEvent.click(screen.getByRole('button', {name: button}));

            expect(within(screen.getByRole('dialog')).getAllByRole('heading', {name: title})[0]).toBeInTheDocument();
        });

        it('tells in the suspension form that a guardian with another guardian is removed from the guardians', () => {
            renderPageWithMembers([suspendRow()]);
            fireEvent.click(screen.getByRole('button', {name: /ukončit členství/i}));

            expect(within(screen.getByRole('dialog')).getByText(labels.suspensionWarning.suspendFormGuardianNote)).toBeInTheDocument();
        });

        it('does not show the guardian note in the resume dialog', () => {
            renderPageWithMembers([buildMemberRow({
                _templates: {resumeMember: mockHalFormsTemplate({method: 'POST', title: 'Reaktivace člena', properties: []})},
            })]);
            fireEvent.click(screen.getByRole('button', {name: /reaktivovat/i}));

            expect(screen.queryByText(labels.suspensionWarning.suspendFormGuardianNote)).not.toBeInTheDocument();
        });
    });

    it('clicking edit icon navigates to member detail page', () => {
        const navigateToResource = vi.fn();
        const member = buildMemberRow({
            _templates: {updateMember: mockHalFormsTemplate({method: 'PUT'})},
        });
        renderPageWithMembers([member], {navigateToResource, refetch: async () => {}});

        fireEvent.click(screen.getByRole('button', {name: /upravit/i}));
        expect(navigateToResource).toHaveBeenCalledWith(member, {state: {editing: true}});
    });

    it('clicking shield icon opens PermissionsDialog', () => {
        renderPageWithMembers([buildMemberRow({
            _links: {self: {href: '/api/members/member-1'}, permissions: {href: '/api/members/member-1/permissions'}},
        })]);

        fireEvent.click(screen.getByRole('button', {name: /oprávnění/i}));
        expect(screen.getByTestId('permissions-dialog')).toBeInTheDocument();
    });

    it('clicking Banknote button opens FinanceTransactionDialog with the account link', () => {
        renderPageWithMembers([buildMemberRow({
            _links: {self: {href: '/api/members/member-1'}, account: {href: '/api/members/member-1/account'}},
        })]);

        fireEvent.click(screen.getByRole('button', {name: labels.finance.openTransactionDialogAriaLabel}));

        const dialog = screen.getByTestId('finance-transaction-dialog');
        expect(dialog).toBeInTheDocument();
        expect(dialog).toHaveAttribute('data-account-href', '/api/members/member-1/account');
    });

    it.each([
        {icon: 'shield', name: /oprávnění/i, link: {permissions: {href: '/api/members/member-1/permissions'}}},
        {icon: 'Banknote', name: labels.finance.openTransactionDialogAriaLabel, link: {account: {href: '/api/members/member-1/account'}}},
    ])('clicking $icon icon does not trigger row click', ({name, link}) => {
        const navigateToResource = vi.fn();
        renderPageWithMembers([buildMemberRow({_links: {self: {href: '/api/members/member-1'}, ...link}})], {
            navigateToResource,
            refetch: async () => {},
        });

        fireEvent.click(screen.getByRole('button', {name}));
        expect(navigateToResource).not.toHaveBeenCalled();
    });
});

describe('MembersPage — data-incomplete badge', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows badge on incomplete row and not on complete row, when hasManageAuthority is true', () => {
        const resourceData = membersCollection([
            buildMemberRow({id: 'm1', registrationNumber: 'REG-001', dataIncomplete: true}),
            buildMemberRow({id: 'm2', registrationNumber: 'REG-002', dataIncomplete: false}),
        ], manageMembersResource);
        queryReturning(resourceData);
        renderPage(createMockPageData(resourceData));

        const badges = screen.getAllByText(labels.membersFilter.incompleteBadge)
            .filter((el) => el.tagName !== 'TH');
        expect(badges).toHaveLength(1);
    });

    it('does NOT show the badge column when hasManageAuthority is false (field absent)', () => {
        renderPageWithMembers([buildMemberRow({id: 'm1', registrationNumber: 'REG-001'})]);

        expect(screen.queryByText(labels.membersFilter.incompleteBadge)).not.toBeInTheDocument();
    });
});

describe('MembersPage — filters wired to the table request', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('leaves a bare URL bare while requesting the active-only default', () => {
        queryReturning(undefined);
        renderPage(createMockPageData(plainMembersResource));

        expect(screen.getByTestId('search-string')).toHaveTextContent('(none)');
        expect(requestedUrlsOf().every(url => url.includes('status=ACTIVE'))).toBe(true);
    });

    it('does NOT overwrite an existing status param on mount', () => {
        queryReturning(undefined);
        renderPage(createMockPageData(plainMembersResource), '/members?status=INACTIVE');

        expect(screen.getByTestId('search-string')).toHaveTextContent('status=INACTIVE');
    });

    it('manager sees status pills with Aktivní selected by default', () => {
        queryReturning(manageMembersResource);
        renderPage(createMockPageData(manageMembersResource), '/members?status=ACTIVE');

        expect(screen.getByRole('button', {name: labels.membersFilter.statusActive})).toHaveAttribute('aria-pressed', 'true');
        expect(screen.getByRole('button', {name: labels.membersFilter.statusInactive})).toHaveAttribute('aria-pressed', 'false');
    });

    it('the incomplete URL param checks the toggle and reaches the table request', () => {
        queryReturning(manageMembersResource);
        renderPage(createMockPageData(manageMembersResource), '/members?status=ACTIVE&incomplete=true');

        expect(screen.getByRole('checkbox', {name: labels.membersFilter.incompleteOnly})).toBeChecked();
        expect(requestedUrlsOf().some(u => u.includes('incomplete=true'))).toBe(true);
    });

    it('regular user keeps the search while the manager-only filters are hidden', () => {
        queryReturning(plainMembersResource);
        renderPage(createMockPageData(plainMembersResource), '/members?status=ACTIVE');

        expect(screen.queryByRole('button', {name: labels.membersFilter.statusInactive})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: labels.membersFilter.search})).toBeInTheDocument();
    });

    it('prefills the search input from the q URL param and sends it with the table request', () => {
        queryReturning(plainMembersResource);
        renderPage(createMockPageData(plainMembersResource), '/members?status=ACTIVE&q=nov');

        expect(screen.getByRole('textbox', {name: labels.membersFilter.search})).toHaveValue('nov');
        expect(requestedUrlsOf().some(u => u.includes('q=nov') && u.includes('status=ACTIVE'))).toBe(true);
    });

    it('clears the search input and drops q from the request when returning via the menu link', async () => {
        queryReturning(plainMembersResource);
        renderPage(createMockPageData(plainMembersResource), '/members?status=ACTIVE&q=nov');
        vi.mocked(useAuthorizedQuery).mockClear();

        await userEvent.click(screen.getByRole('link', {name: 'menu-members'}));

        expect(screen.getByRole('textbox', {name: labels.membersFilter.search})).toHaveValue('');
        expect(requestedUrlsOf().some(u => u.includes('q='))).toBe(false);
    });

    it('keeps the default active-only filter in the request after returning via the menu link', async () => {
        queryReturning(plainMembersResource);
        renderPage(createMockPageData(plainMembersResource), '/members?status=INACTIVE');
        vi.mocked(useAuthorizedQuery).mockClear();

        await userEvent.click(screen.getByRole('link', {name: 'menu-members'}));

        const urls = requestedUrlsOf();
        expect(urls.length).toBeGreaterThan(0);
        expect(urls.every(u => u.includes('status=ACTIVE'))).toBe(true);
    });

    it('on page 2, a status change reaches the query and requests page=0', async () => {
        const resource: HalResponse = {
            ...manageMembersResource,
            _embedded: {memberSummaryResponseList: []},
            page: {size: 10, totalElements: 30, totalPages: 3, number: 0},
        };
        queryReturning(resource);
        renderPage(createMockPageData(resource));

        await userEvent.click(screen.getByRole('button', {name: /Další/}));
        expect(requestedUrlsOf().at(-1)).toContain('page=1');

        await userEvent.click(screen.getByRole('button', {name: labels.membersFilter.statusInactive}));

        const last = requestedUrlsOf().at(-1)!;
        expect(last).toContain('status=INACTIVE');
        expect(last).toContain('page=0');
        expect(screen.getByTestId('search-string')).toHaveTextContent('status=INACTIVE');
    });
});

describe('MembersPage opened from a legal guardian profile', () => {
    const registerResource = (): HalResponse => ({
        _links: {self: {href: '/api/members'}},
        _templates: {
            registerMember: mockHalFormsTemplate({title: 'Registrovat člena', method: 'POST'}),
        },
    });

    const Probe = () => {
        const location = useLocation();
        return <div data-testid="location" data-state={JSON.stringify(location.state)}>{location.pathname}</div>;
    };

    const renderFromGuardian = (resource: HalResponse, state: unknown = {
        takeOverLegalGuardianId: 'u-1',
        returnTo: '/legal-guardians/u-1',
    }) => {
        const displayHalForm = vi.fn();
        vi.mocked(useHalForm).mockReturnValue({displayHalForm, currentFormRequest: null, closeForm: vi.fn()});
        vi.mocked(useHalPageData).mockReturnValue(createMockPageData(resource));
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[{pathname: '/members', state}]}>
                    <ToastProvider>
                        <MembersPage/>
                        <Probe/>
                    </ToastProvider>
                </MemoryRouter>
            </QueryClientProvider>
        );
        return displayHalForm;
    };

    it('opens the inline registration form once', () => {
        const displayHalForm = renderFromGuardian(registerResource());

        expect(displayHalForm).toHaveBeenCalledTimes(1);
        expect(displayHalForm).toHaveBeenCalledWith(expect.objectContaining({
            templateName: 'registerMember',
            children: expect.any(Function),
            onCancel: expect.any(Function),
        }));
    });

    it('clears the router state after opening the form so Back does not reopen it', () => {
        renderFromGuardian(registerResource());

        expect(screen.getByTestId('location')).toHaveTextContent('/members');
        expect(screen.getByTestId('location').dataset.state).toBe('null');
    });

    it('returns to the guardian profile when the form is cancelled', () => {
        const displayHalForm = renderFromGuardian(registerResource());

        act(() => displayHalForm.mock.calls[0][0].onCancel());

        expect(screen.getByTestId('location')).toHaveTextContent('/legal-guardians/u-1');
    });

    it('does not open the form without the registerMember template', () => {
        const displayHalForm = renderFromGuardian({_links: {self: {href: '/api/members'}}});

        expect(displayHalForm).not.toHaveBeenCalled();
    });

    it('does not open the form when the page is opened without guardian state', () => {
        const displayHalForm = renderFromGuardian(registerResource(), null);

        expect(displayHalForm).not.toHaveBeenCalled();
    });
});
