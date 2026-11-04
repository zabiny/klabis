import '@testing-library/jest-dom';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {stubFetch} from '../../__mocks__/halDialogTest';
import {confirmTemplate, renderDetailPage} from '../../__mocks__/halGroupPageTest';

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

vi.mock('../../contexts/halRouteContext.ts', () => ({
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

const buildPendingInvitationWithAffordance = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-1',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-1'},
        invitedMember: {href: '/api/members/member-2'},
    },
    _templates: {
        // Spring HATEOAS does not emit properties for DELETE affordances — empty array reflects the actual backend response
        cancelInvitation: confirmTemplate({
            title: 'Zrušit pozvánku',
            target: '/api/groups/group-1/invitations/inv-1',
        }),
    },
    ...overrides,
});

const buildPendingInvitationWithoutAffordance = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-2',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-2'},
        invitedMember: {href: '/api/members/member-3'},
    },
    ...overrides,
});

describe('GroupDetailPage — cancel invitation (task 7)', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch());
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    describe('7.1 / 7.2 — button visibility driven by HAL affordance', () => {
        it('shows "Zrušit pozvánku" button when cancelInvitation affordance is present on invitation row', () => {
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithAffordance()]}));
            expect(screen.getByRole('button', {name: /zrušit pozvánku/i})).toBeInTheDocument();
        });

        it('does NOT show "Zrušit pozvánku" button when cancelInvitation affordance is absent (non-owner view)', () => {
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithoutAffordance()]}));
            expect(screen.queryByRole('button', {name: /zrušit pozvánku/i})).not.toBeInTheDocument();
        });

        it('shows cancel button only for invitations that have the affordance when mixed', () => {
            renderPage(buildGroupDetail({
                pendingInvitations: [
                    buildPendingInvitationWithAffordance(),
                    buildPendingInvitationWithoutAffordance(),
                ],
            }));
            expect(screen.getAllByRole('button', {name: /zrušit pozvánku/i})).toHaveLength(1);
        });
    });

    describe('7.3 — modal opens with reason textarea on button click', () => {
        it('clicking "Zrušit pozvánku" opens a confirmation dialog titled "Zrušit pozvánku"', async () => {
            const user = userEvent.setup();
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithAffordance()]}));
            await user.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));

            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Zrušit pozvánku');
        });

        it('dialog renders "reason" textarea even when backend DELETE affordance has empty properties', async () => {
            const user = userEvent.setup();
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithAffordance()]}));
            await user.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));

            expect(await screen.findByRole('textbox', {name: /důvod/i})).toBeInTheDocument();
        });
    });

    describe('7.4 — cancel closes modal without API call', () => {
        it('opening and dismissing the dialog does not send any request', async () => {
            const user = userEvent.setup();
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithAffordance()]}));
            await user.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            await user.click(await screen.findByTestId('modal-close-button'));

            expect(screen.queryByTestId('modal-title')).not.toBeInTheDocument();
            expect(writeCalls()).toHaveLength(0);
        });
    });

    describe('7.5 — submit', () => {
        it('deletes the invitation and stays on the group page', async () => {
            const user = userEvent.setup();
            renderPage(buildGroupDetail({pendingInvitations: [buildPendingInvitationWithAffordance()]}));
            await user.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/groups/group-1/invitations/inv-1');
            expect((writeCalls()[0][1] as RequestInit).method).toBe('DELETE');
            await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Úspěšně uloženo'));
            expect(screen.getByTestId('location')).toHaveTextContent('/groups/group-1');
        });
    });
});
