import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {GroupDetailPage} from './GroupDetailPage';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {createTestQueryClient, LocationProbe} from '../../__mocks__/halDialogTest';
import {confirmTemplate} from '../../__mocks__/halGroupPageTest';
import {createMockResponse} from '../../__mocks__/mockFetch';

const {currentMemberId} = vi.hoisted(() => ({currentMemberId: {value: 'owner-1'}}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../contexts/authContext.ts', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../contexts/authContext.ts')>()),
    useAuth: () => ({getUser: () => ({memberId: currentMemberId.value})}),
}));

const MEMBERS: Record<string, unknown> = {
    '/members/owner-1': {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9000'},
    '/members/member-1': {firstName: 'Petr', lastName: 'Svoboda', registrationNumber: 'ZBM9500'},
};

const groupWith = (overrides: Record<string, unknown>) => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [{memberId: 'owner-1', _links: {member: {href: '/api/members/owner-1'}}}],
    members: [{
        memberId: 'member-1',
        joinedAt: '2025-01-15T10:00:00Z',
        _links: {member: {href: '/api/members/member-1'}},
    }],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const deletableGroup = () => groupWith({
    _templates: {deleteGroup: confirmTemplate({title: 'Smazat skupinu', target: '/api/groups/group-1'})},
});

const leavableGroup = () => groupWith({
    _templates: {
        removeGroupMember: confirmTemplate({
            title: 'removeGroupMember',
            target: '/api/groups/group-1/members/member-1',
        }),
    },
});

const groupWithSelfRemovableOwner = () => groupWith({
    owners: [{
        memberId: 'owner-1',
        _links: {member: {href: '/api/members/owner-1'}, self: {href: '/api/groups/group-1/owners/owner-1'}},
        _templates: {
            removeGroupOwner: confirmTemplate({
                title: 'Odebrat správce',
                target: '/api/groups/group-1/owners/owner-1',
            }),
        },
    }],
});

// The write succeeds, then the group can no longer be read — as after the caller has removed their own access.
const stubFetchWhereGroupVanishesAfterWrite = (group: unknown, statusAfterWrite: number) => {
    let written = false;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
        const method = init?.method ?? 'GET';
        if (method === 'OPTIONS') return createMockResponse(null, 200);
        if (method !== 'GET') {
            written = true;
            return createMockResponse(null, 204);
        }
        const member = Object.keys(MEMBERS).find(path => url.endsWith(path));
        if (member) return createMockResponse(MEMBERS[member]);
        return written
            ? createMockResponse({title: 'gone', status: statusAfterWrite}, statusAfterWrite)
            : createMockResponse(group);
    }));
};

const renderGroupDetail = () => render(
    <QueryClientProvider client={createTestQueryClient()}>
        <MemoryRouter initialEntries={['/groups/group-1']}>
            <ToastProvider>
                <Routes>
                    <Route path="/groups/:id" element={<HalRouteProvider><GroupDetailPage/></HalRouteProvider>}/>
                    <Route path="/groups" element={<div data-testid="groups-list"/>}/>
                </Routes>
                <LocationProbe/>
            </ToastProvider>
        </MemoryRouter>
    </QueryClientProvider>
);

const confirmAction = async (buttonName: RegExp) => {
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', {name: buttonName}));
    await user.click(await screen.findByRole('button', {name: /odeslat/i}));
};

const expectBackOnGroupList = async () => {
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/groups$/));
    expect(screen.getByTestId('groups-list')).toBeInTheDocument();
};

describe('GroupDetailPage — navigation survives a failing post-write refetch of the group', () => {
    beforeEach(() => {
        currentMemberId.value = 'owner-1';
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('returns to the group list after deleting the group when the group then answers 404', async () => {
        stubFetchWhereGroupVanishesAfterWrite(deletableGroup(), 404);
        renderGroupDetail();

        await confirmAction(/smazat skupinu/i);

        await expectBackOnGroupList();
    });

    it('returns to the group list after leaving the group when the group then answers 403', async () => {
        currentMemberId.value = 'member-1';
        stubFetchWhereGroupVanishesAfterWrite(leavableGroup(), 403);
        renderGroupDetail();

        await confirmAction(/opustit skupinu/i);

        await expectBackOnGroupList();
    });

    it('returns to the group list after removing oneself as owner when the group then answers 403', async () => {
        stubFetchWhereGroupVanishesAfterWrite(groupWithSelfRemovableOwner(), 403);
        renderGroupDetail();

        await confirmAction(/odebrat správce/i);

        await expectBackOnGroupList();
    });
});
