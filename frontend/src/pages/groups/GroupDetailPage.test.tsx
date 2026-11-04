import '@testing-library/jest-dom';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {stubFetch} from '../../__mocks__/halDialogTest';
import {
    confirmTemplate,
    memberOptionsResponse,
    memberPickerTemplate,
    renderDetailPage,
} from '../../__mocks__/halGroupPageTest';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/authContext.ts', () => ({
    useAuth: () => ({getUser: () => ({memberId: null})}),
}));

vi.mock('../../contexts/halRouteContext.ts', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9500', _links: {self: {href: '/api/members/member-1'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));

const renderPage = (resourceData: HalResponse | null, options: {isLoading?: boolean; error?: Error | null} = {}) =>
    renderDetailPage(<GroupDetailPage/>, resourceData, {pathname: '/groups/group-1', ...options});

const buildGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [],
    members: [],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const buildOwner = (overrides?: Record<string, unknown>) => ({
    memberId: 'owner-1',
    _links: {member: {href: '/api/members/owner-1'}},
    ...overrides,
});

const buildMember = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    joinedAt: '2025-01-15T00:00:00Z',
    _links: {
        self: {href: '/api/groups/group-1/members/member-1'},
        member: {href: '/api/members/member-1'},
    },
    ...overrides,
});

const buildPendingInvitation = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-1',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-1'},
        accept: {href: '/api/groups/group-1/invitations/inv-1/accept'},
        reject: {href: '/api/groups/group-1/invitations/inv-1/reject'},
        invitedMember: {href: '/api/members/member-2'},
    },
    ...overrides,
});

describe('GroupDetailPage', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch(memberOptionsResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(null, {isLoading: true});
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(null, {error: new Error('Skupina nenalezena')});
        expect(screen.getByText('Skupina nenalezena')).toBeInTheDocument();
    });

    it('renders group name as page heading', () => {
        renderPage(buildGroupDetail({name: 'Závoďáci'}));
        expect(screen.getByRole('heading', {level: 1, name: 'Závoďáci'})).toBeInTheDocument();
    });

    it('renders back link to /groups', () => {
        renderPage(buildGroupDetail());
        const backLink = screen.getByRole('link', {name: /zpět na seznam/i});
        expect(backLink).toHaveAttribute('href', '/groups');
    });

    it('renders "Členové" section heading', () => {
        renderPage(buildGroupDetail());
        expect(screen.getByText('ČLENOVÉ')).toBeInTheDocument();
    });

    it('renders owners section when owners exist', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
        });
        renderPage(resourceData);
        expect(screen.getByText('SPRÁVCI')).toBeInTheDocument();
    });

    it('resolves owner name with registration number via HAL member link', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
        });
        renderPage(resourceData);
        expect(screen.getByText('Jana Nováková (ZBM9500)')).toBeInTheDocument();
    });

    it('does not render owners section when owners list is empty', () => {
        renderPage(buildGroupDetail({owners: []}));
        expect(screen.queryByText('SPRÁVCI')).not.toBeInTheDocument();
    });

    it('shows "edit name" button when updateGroup template exists', () => {
        const resourceData = buildGroupDetail({
            _templates: {updateGroup: mockHalFormsTemplate({title: 'Upravit název', method: 'PATCH'})},
        });
        renderPage(resourceData);
        expect(screen.getByRole('button', {name: /upravit název/i})).toBeInTheDocument();
    });

    it('does not show "edit name" button when updateGroup template is absent', () => {
        renderPage(buildGroupDetail());
        expect(screen.queryByRole('button', {name: /upravit název/i})).not.toBeInTheDocument();
    });

    it('clicking "edit name" button shows inline form', async () => {
        const resourceData = buildGroupDetail({
            _templates: {updateGroup: mockHalFormsTemplate({title: 'Upravit název', method: 'PATCH'})},
        });
        renderPage(resourceData);
        await userEvent.click(screen.getByRole('button', {name: /upravit název/i}));
        expect(await screen.findByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
    });

    describe('delegated permissions', () => {
        it('shows the permissions owners hold over members', () => {
            renderPage(buildGroupDetail({delegatedAuthorities: ['MEMBERS:EDIT_PROFILE']}));
            expect(screen.getByText('Vlastníci skupiny (i budoucí) mohou: Úprava údajů člena')).toBeInTheDocument();
        });

        it('states that owners gain nothing when no permission is delegated', () => {
            renderPage(buildGroupDetail({delegatedAuthorities: []}));
            expect(screen.getByText('Vlastníci skupiny nad členy nezískávají žádná oprávnění.')).toBeInTheDocument();
        });

        it('shows the delegated permissions read-only inside the edit form', async () => {
            const resourceData = buildGroupDetail({
                delegatedAuthorities: ['MEMBERS:EDIT_PROFILE'],
                _templates: {updateGroup: mockHalFormsTemplate({title: 'Upravit název', method: 'PATCH'})},
            });
            renderPage(resourceData);
            expect(screen.queryByTestId('delegated-authorities-readonly')).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: /upravit název/i}));

            const readOnly = screen.getByTestId('delegated-authorities-readonly');
            expect(readOnly).toHaveTextContent('Úprava údajů člena');
            expect(readOnly.querySelector('input')).toBeNull();
        });
    });

    it('shows "Přidat člena" button when addGroupMember template exists', () => {
        const resourceData = buildGroupDetail({
            _templates: {addGroupMember: mockHalFormsTemplate({title: 'Přidat člena', method: 'POST'})},
        });
        renderPage(resourceData);
        expect(screen.getByRole('button', {name: /přidat člena/i})).toBeInTheDocument();
    });

    it('does not show "Přidat člena" button when addGroupMember template is absent', () => {
        renderPage(buildGroupDetail());
        expect(screen.queryByRole('button', {name: /přidat člena/i})).not.toBeInTheDocument();
    });

    it('clicking "Přidat člena" opens dialog titled "Přidat člena"', async () => {
        const resourceData = buildGroupDetail({
            _templates: {addGroupMember: memberPickerTemplate({title: 'Přidat člena'})},
        });
        renderPage(resourceData);
        await userEvent.click(screen.getByRole('button', {name: /přidat člena/i}));
        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přidat člena');
    });

    it('"Přidat člena" picker does not offer current members and submits to the template target', async () => {
        const user = userEvent.setup();
        const resourceData = buildGroupDetail({
            members: [buildMember()],
            _templates: {addGroupMember: memberPickerTemplate({title: 'Přidat člena', target: '/api/groups/group-1/members'})},
        });
        renderPage(resourceData);
        await user.click(screen.getByRole('button', {name: /přidat člena/i}));

        expect(await screen.findByRole('option', {name: 'Jana Nováková'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Karel Dvořák'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Petr Svoboda'})).not.toBeInTheDocument();

        await user.selectOptions(screen.getByRole('combobox', {name: /člen/i}), 'member-2');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/members');
        expect((writeCalls()[0][1] as RequestInit).method).toBe('POST');
        await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Úspěšně uloženo'));
    });

    it('renders member rows with resolved names, registration numbers, and joined dates', () => {
        const resourceData = buildGroupDetail({
            members: [buildMember()],
        });
        renderPage(resourceData);
        expect(screen.getByText('Jana Nováková (ZBM9500)')).toBeInTheDocument();
        expect(screen.getByText('15. 1. 2025')).toBeInTheDocument();
    });

    it('renders "Odebrat" button per member when removeGroupMember template exists on member', () => {
        const member = buildMember({
            _templates: {removeGroupMember: mockHalFormsTemplate({title: 'Odebrat', method: 'DELETE', target: '/api/groups/group-1/members/member-1'})},
        });
        const resourceData = buildGroupDetail({members: [member]});
        renderPage(resourceData);
        expect(screen.getByRole('button', {name: /odebrat/i})).toBeInTheDocument();
    });

    it('does not render "Odebrat" button when removeGroupMember template is absent on member', () => {
        const resourceData = buildGroupDetail({members: [buildMember()]});
        renderPage(resourceData);
        expect(screen.queryByRole('button', {name: /odebrat/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat" opens the dialog with the template title and submits to the member self link', async () => {
        const user = userEvent.setup();
        const member = buildMember({
            _templates: {removeGroupMember: confirmTemplate({title: 'Odebrat člena'})},
        });
        renderPage(buildGroupDetail({members: [member]}));
        await user.click(screen.getByRole('button', {name: /odebrat/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Odebrat člena');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/members/member-1');
        expect((writeCalls()[0][1] as RequestInit).method).toBe('DELETE');
    });

    it('shows empty state message when no members', () => {
        renderPage(buildGroupDetail({members: []}));
        expect(screen.getByText('Skupina nemá žádné členy.')).toBeInTheDocument();
    });

    describe('pending invitations (owner view)', () => {
        it('shows pending invitations section heading when invitations exist', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitation()],
            });
            renderPage(resourceData);
            expect(screen.getByText('ČEKAJÍCÍ POZVÁNKY')).toBeInTheDocument();
        });

        it('does not show pending invitations section when list is empty', () => {
            renderPage(buildGroupDetail({pendingInvitations: []}));
            expect(screen.queryByText('ČEKAJÍCÍ POZVÁNKY')).not.toBeInTheDocument();
        });

        it('does not show pending invitations section when field is absent', () => {
            renderPage(buildGroupDetail());
            expect(screen.queryByText('ČEKAJÍCÍ POZVÁNKY')).not.toBeInTheDocument();
        });

        it('renders invited member entry when invitedMember link exists', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitation()],
            });
            renderPage(resourceData);
            expect(screen.queryByText('inv-1')).not.toBeInTheDocument();
        });

        it('falls back to invitationId when member link is absent', () => {
            const invitation = buildPendingInvitation({_links: {self: {href: '/api/groups/group-1/invitations/inv-1'}}});
            const resourceData = buildGroupDetail({pendingInvitations: [invitation]});
            renderPage(resourceData);
            expect(screen.getByText('inv-1')).toBeInTheDocument();
        });
    });

    describe('invite member (owner)', () => {
        it('shows "Pozvat člena" button when inviteMember template exists', () => {
            const resourceData = buildGroupDetail({
                _templates: {inviteMember: mockHalFormsTemplate({title: 'Pozvat člena', method: 'POST'})},
            });
            renderPage(resourceData);
            expect(screen.getByRole('button', {name: /pozvat člena/i})).toBeInTheDocument();
        });

        it('does not show "Pozvat člena" button when inviteMember template is absent', () => {
            renderPage(buildGroupDetail());
            expect(screen.queryByRole('button', {name: /pozvat člena/i})).not.toBeInTheDocument();
        });

        it('clicking "Pozvat člena" opens the dialog and its picker does not offer current members', async () => {
            const user = userEvent.setup();
            const resourceData = buildGroupDetail({
                members: [buildMember()],
                _templates: {inviteMember: memberPickerTemplate({title: 'Pozvat člena'})},
            });
            renderPage(resourceData);
            await user.click(screen.getByRole('button', {name: /pozvat člena/i}));

            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Pozvat člena');
            expect(await screen.findByRole('option', {name: 'Jana Nováková'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'Petr Svoboda'})).not.toBeInTheDocument();
        });
    });
});
