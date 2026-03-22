import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembersPage} from './MembersPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/members/PermissionsDialog', () => ({
    PermissionsDialog: ({isOpen, memberName}: {isOpen: boolean; memberName: string}) =>
        isOpen ? <div data-testid="permissions-dialog">{memberName}</div> : null,
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: () => <div data-testid="hal-form-display"/>,
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        ModalOverlay: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
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
                <MembersPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildMemberRow = (overrides?: Record<string, unknown>) => ({
    id: 'member-1',
    registrationNumber: 'REG-001',
    lastName: 'Novák',
    firstName: 'Jan',
    email: 'jan.novak@example.com',
    active: true,
    _links: {self: {href: '/api/members/member-1'}},
    ...overrides,
});

const renderPageWithMembers = (members: unknown[], routeOverrides?: any) => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/members'}},
        _embedded: {memberSummaryResponseList: members},
        page: {size: 10, totalElements: members.length, totalPages: 1, number: 0},
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as any);
    const pageData = createMockPageData(resourceData, routeOverrides);
    return renderPage(pageData);
};

describe('MembersPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Členové"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Členové')).toBeInTheDocument();
    });

    it('renders "Registrovat člena" link navigating to /members/new when registerMember template exists', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {
                default: mockHalFormsTemplate({title: 'Get Members', method: 'PATCH'}),
                registerMember: mockHalFormsTemplate({title: 'Registrovat člena', method: 'POST'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        const link = screen.getByRole('link', {name: /registrovat člena/i});
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/members/new');
    });

    it('does NOT render "Registrovat člena" link when only default template exists (MEMBERS:READ only user)', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {
                default: mockHalFormsTemplate({title: 'Get Members', method: 'PATCH'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('link', {name: /registrovat člena/i})).not.toBeInTheDocument();
    });

    it('does NOT render "Registrovat člena" link when no templates exist', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('link', {name: /registrovat člena/i})).not.toBeInTheDocument();
    });

    it('renders basic table columns: Reg. číslo, Příjmení, Jméno', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Reg. číslo')).toBeInTheDocument();
        expect(screen.getByText('Příjmení')).toBeInTheDocument();
        expect(screen.getByText('Jméno')).toBeInTheDocument();
    });

    it('renders E-mail and Stav columns', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('E-mail')).toBeInTheDocument();
        expect(screen.getByText('Stav')).toBeInTheDocument();
    });

    it('renders Akce column header', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Akce')).toBeInTheDocument();
    });

    it('shows loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(screen.getByText(/načítání/i)).toBeInTheDocument();
    });
});

describe('MembersPage — row data rendering', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows Aktivní badge for active member', () => {
        renderPageWithMembers([buildMemberRow({active: true})]);
        expect(screen.getByText('Aktivní')).toBeInTheDocument();
    });

    it('shows Neaktivní badge for inactive member', () => {
        renderPageWithMembers([buildMemberRow({active: false})]);
        expect(screen.getByText('Neaktivní')).toBeInTheDocument();
    });

    it('does not show status badge when active is null', () => {
        renderPageWithMembers([buildMemberRow({active: null})]);
        expect(screen.queryByText('Aktivní')).not.toBeInTheDocument();
        expect(screen.queryByText('Neaktivní')).not.toBeInTheDocument();
    });

    it('shows email value in email column', () => {
        renderPageWithMembers([buildMemberRow({email: 'test@example.com'})]);
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
    });

    it('shows edit icon when _templates.default exists on member row', () => {
        const member = buildMemberRow({
            _templates: {default: mockHalFormsTemplate({method: 'PUT'})},
        });
        renderPageWithMembers([member]);
        expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
    });

    it('does not show edit icon when _templates.default is absent on member row', () => {
        renderPageWithMembers([buildMemberRow()]);
        expect(screen.queryByRole('button', {name: /upravit/i})).not.toBeInTheDocument();
    });

    it('shows shield icon when _links.permissions exists on member row', () => {
        const member = buildMemberRow({
            _links: {
                self: {href: '/api/members/member-1'},
                permissions: {href: '/api/members/member-1/permissions'},
            },
        });
        renderPageWithMembers([member]);
        expect(screen.getByRole('button', {name: /oprávnění/i})).toBeInTheDocument();
    });

    it('does not show shield icon when _links.permissions is absent', () => {
        renderPageWithMembers([buildMemberRow()]);
        expect(screen.queryByRole('button', {name: /oprávnění/i})).not.toBeInTheDocument();
    });

    it('shows suspend icon when _templates.suspendMember exists on member row', () => {
        const member = buildMemberRow({
            _templates: {suspendMember: mockHalFormsTemplate({method: 'POST'})},
        });
        renderPageWithMembers([member]);
        expect(screen.getByRole('button', {name: /ukončit členství/i})).toBeInTheDocument();
    });

    it('shows resume icon when _templates.resumeMember exists on member row', () => {
        const member = buildMemberRow({
            _templates: {resumeMember: mockHalFormsTemplate({method: 'POST'})},
        });
        renderPageWithMembers([member]);
        expect(screen.getByRole('button', {name: /reaktivovat/i})).toBeInTheDocument();
    });

    it('clicking edit icon navigates to member detail page', () => {
        const navigateToResource = vi.fn();
        const member = buildMemberRow({
            _templates: {default: mockHalFormsTemplate({method: 'PUT'})},
        });
        renderPageWithMembers([member], {
            route: {
                pathname: '/members',
                navigateToResource,
                refetch: async () => {},
                queryState: 'success' as const,
                getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
            },
        });

        const editButton = screen.getByRole('button', {name: /upravit/i});
        fireEvent.click(editButton);
        expect(navigateToResource).toHaveBeenCalledWith(member);
    });

    it('clicking shield icon opens PermissionsDialog', () => {
        const member = buildMemberRow({
            firstName: 'Jan',
            lastName: 'Novák',
            _links: {
                self: {href: '/api/members/member-1'},
                permissions: {href: '/api/members/member-1/permissions'},
            },
        });
        renderPageWithMembers([member]);

        const shieldButton = screen.getByRole('button', {name: /oprávnění/i});
        fireEvent.click(shieldButton);
        expect(screen.getByTestId('permissions-dialog')).toBeInTheDocument();
    });

    it('clicking action icons does not trigger row click', () => {
        const navigateToResource = vi.fn();
        const member = buildMemberRow({
            _links: {
                self: {href: '/api/members/member-1'},
                permissions: {href: '/api/members/member-1/permissions'},
            },
        });
        renderPageWithMembers([member], {
            route: {
                pathname: '/members',
                navigateToResource,
                refetch: async () => {},
                queryState: 'success' as const,
                getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
            },
        });

        const shieldButton = screen.getByRole('button', {name: /oprávnění/i});
        fireEvent.click(shieldButton);
        expect(navigateToResource).not.toHaveBeenCalled();
    });
});
