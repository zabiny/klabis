import '@testing-library/jest-dom';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {stubFetch} from '../../__mocks__/halDialogTest';
import {confirmTemplate, renderDetailPage} from '../../__mocks__/halGroupPageTest';

const {MEMBER_DATA, currentMemberId} = vi.hoisted(() => ({
    MEMBER_DATA: {
        '/members/owner-1': {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9000'},
        '/members/member-1': {firstName: 'Petr', lastName: 'Svoboda', registrationNumber: 'ZBM9500'},
    } as Record<string, {firstName: string; lastName: string; registrationNumber: string}>,
    currentMemberId: {value: 'owner-1' as string | null},
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/authContext.ts', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../contexts/authContext.ts')>()),
    useAuth: () => ({getUser: () => ({memberId: currentMemberId.value})}),
}));

const memberResponse = (url: string): unknown | undefined => {
    const key = Object.keys(MEMBER_DATA).find(path => url.endsWith(path));
    return key ? MEMBER_DATA[key] : undefined;
};

const renderPage = (resourceData: HalResponse, refetch?: () => Promise<void>) =>
    renderDetailPage(<GroupDetailPage/>, resourceData, {pathname: '/groups/group-1', refetch});

const buildOwner = (overrides?: Record<string, unknown>) => ({
    memberId: 'owner-1',
    _links: {member: {href: '/api/members/owner-1'}},
    ...overrides,
});

const buildMember = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    joinedAt: '2025-01-15T10:00:00Z',
    _links: {member: {href: '/api/members/member-1'}},
    ...overrides,
});

const buildGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [buildOwner()],
    members: [buildMember()],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const leaveTemplate = () => confirmTemplate({
    title: 'removeGroupMember',
    target: '/api/groups/group-1/members/member-1',
});

const ownerWithRemoveTemplate = () => buildOwner({
    _links: {
        member: {href: '/api/members/owner-1'},
        self: {href: '/api/groups/group-1/owners/owner-1'},
    },
    _templates: {
        removeGroupOwner: confirmTemplate({
            title: 'Odebrat správce',
            target: '/api/groups/group-1/owners/owner-1',
        }),
    },
});

describe('GroupDetailPage — owners and members are disjoint', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'owner-1';
        ({writeCalls} = stubFetch(memberResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows every person exactly once — the owner is not repeated in the member list', async () => {
        renderPage(buildGroupDetail());

        expect(screen.getByText('SPRÁVCI')).toBeInTheDocument();
        expect(screen.getByText('ČLENOVÉ')).toBeInTheDocument();
        expect(await screen.findAllByText('Jana Nováková (ZBM9000)')).toHaveLength(1);
        expect(await screen.findAllByText('Petr Svoboda (ZBM9500)')).toHaveLength(1);
    });

    it('shows no member section rows when the creator is the group\'s only person', async () => {
        renderPage(buildGroupDetail({members: []}));

        expect(screen.getByText('Skupina nemá žádné členy.')).toBeInTheDocument();
        expect(await screen.findAllByText('Jana Nováková (ZBM9000)')).toHaveLength(1);
    });

    it('warns that removing another owner removes them from the group, and stays on the page', async () => {
        const user = userEvent.setup();
        currentMemberId.value = 'someone-else';
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}));
        await user.click(screen.getByRole('button', {name: /odebrat správce/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Odebrat správce');
        expect(screen.getByTestId('modal-context')).toHaveTextContent(
            'Odebráním správce dotčená osoba opustí skupinu.'
        );

        await user.click(screen.getByRole('button', {name: /odeslat/i}));
        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        await waitFor(() => expect(screen.queryByTestId('modal-title')).not.toBeInTheDocument());
        expect(screen.getByTestId('location')).toHaveTextContent('/groups/group-1');
    });
});

describe('GroupDetailPage — an owner giving up their own ownership', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'owner-1';
        ({writeCalls} = stubFetch(memberResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('warns in the second person that the caller is the one leaving', async () => {
        const user = userEvent.setup();
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}));
        await user.click(screen.getByRole('button', {name: /odebrat správce/i}));

        expect(await screen.findByTestId('modal-context')).toHaveTextContent(
            'Odebráním sebe ze správců opustíte skupinu.'
        );
    });

    it('returns to the group list instead of refetching the now-unreadable detail', async () => {
        const user = userEvent.setup();
        const refetch = vi.fn().mockResolvedValue(undefined);
        renderPage(buildGroupDetail({owners: [ownerWithRemoveTemplate()]}), refetch);
        await user.click(screen.getByRole('button', {name: /odebrat správce/i}));
        await user.click(await screen.findByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/owners/owner-1');
        await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/groups$/));
        expect(refetch).not.toHaveBeenCalled();
    });
});

describe('GroupDetailPage — member leaves the free group', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        currentMemberId.value = 'member-1';
        ({writeCalls} = stubFetch(memberResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers "Opustit skupinu" when the backend sends removeGroupMember on the self link', () => {
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));

        expect(screen.getByRole('button', {name: /opustit skupinu/i})).toBeInTheDocument();
    });

    it('offers nothing to leave with when the self link has no removeGroupMember (owners)', () => {
        renderPage(buildGroupDetail());

        expect(screen.queryByRole('button', {name: /opustit skupinu/i})).not.toBeInTheDocument();
    });

    it('asks for confirmation and explains the consequence', async () => {
        const user = userEvent.setup();
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        await user.click(screen.getByRole('button', {name: /opustit skupinu/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Opustit skupinu');
        expect(screen.getByTestId('modal-context')).toHaveTextContent(
            'Opuštěním skupiny ztratíte členství ve skupině.'
        );
    });

    it('returns the user to the group list after leaving', async () => {
        const user = userEvent.setup();
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        await user.click(screen.getByRole('button', {name: /opustit skupinu/i}));
        await user.click(await screen.findByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/groups/group-1/members/member-1');
        await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/groups$/));
    });

    it('does not navigate away when the leave is cancelled', async () => {
        const user = userEvent.setup();
        renderPage(buildGroupDetail({_templates: {removeGroupMember: leaveTemplate()}}));
        await user.click(screen.getByRole('button', {name: /opustit skupinu/i}));
        await user.click(await screen.findByRole('button', {name: /zrušit/i}));

        expect(writeCalls()).toHaveLength(0);
        expect(screen.getByTestId('location')).toHaveTextContent('/groups/group-1');
    });
});
