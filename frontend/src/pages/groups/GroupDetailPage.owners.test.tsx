import '@testing-library/jest-dom';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

const renderPage = (resourceData: HalResponse) =>
    renderDetailPage(<GroupDetailPage/>, resourceData, {pathname: '/groups/group-1'});

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

describe('GroupDetailPage — owner management (task 6.3)', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch(memberOptionsResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows "Přidat správce" button when addOwner template exists on resource', () => {
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
            _templates: {addGroupOwner: memberPickerTemplate({title: 'Přidat správce'})},
        });
        renderPage(resourceData);
        expect(screen.getByRole('button', {name: /přidat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat správce" button when addOwner template is absent', () => {
        renderPage(buildGroupDetail({owners: [buildOwner()]}));
        expect(screen.queryByRole('button', {name: /přidat správce/i})).not.toBeInTheDocument();
    });

    it('"Přidat správce" dialog offers only current members as candidates', async () => {
        const user = userEvent.setup();
        const resourceData = buildGroupDetail({
            owners: [buildOwner()],
            members: [buildMember()],
            _templates: {addGroupOwner: memberPickerTemplate({title: 'Přidat správce', target: '/api/groups/group-1/owners'})},
        });
        renderPage(resourceData);
        await user.click(screen.getByRole('button', {name: /přidat správce/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přidat správce');
        expect(await screen.findByRole('option', {name: 'Petr Svoboda'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Jana Nováková'})).not.toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Karel Dvořák'})).not.toBeInTheDocument();

        await user.selectOptions(screen.getByRole('combobox', {name: /člen/i}), 'member-1');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/owners');
    });

    it('shows "Odebrat správce" button per owner when removeOwner template exists on owner', () => {
        const ownerWithTemplate = {
            ...buildOwner(),
            _templates: {removeGroupOwner: confirmTemplate({title: 'Odebrat správce', target: '/api/groups/group-1/owners/owner-1'})},
        };
        renderPage(buildGroupDetail({owners: [ownerWithTemplate]}));
        expect(screen.getByRole('button', {name: /odebrat správce/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat správce" button when removeOwner template is absent on owner', () => {
        renderPage(buildGroupDetail({owners: [buildOwner()]}));
        expect(screen.queryByRole('button', {name: /odebrat správce/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat správce" opens a confirmation dialog that submits to the owner self link', async () => {
        const user = userEvent.setup();
        const ownerWithTemplate = {
            ...buildOwner(),
            _links: {
                member: {href: '/api/members/owner-1'},
                self: {href: '/api/groups/group-1/owners/owner-1'},
            },
            _templates: {removeGroupOwner: confirmTemplate({title: 'Odebrat správce'})},
        };
        renderPage(buildGroupDetail({owners: [ownerWithTemplate]}));
        await user.click(screen.getByRole('button', {name: /odebrat správce/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Odebrat správce');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/owners/owner-1');
        expect((writeCalls()[0][1] as RequestInit).method).toBe('DELETE');
    });
});
