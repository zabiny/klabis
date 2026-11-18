import '@testing-library/jest-dom';
import React from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {ApiError} from '../../api/authorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {MembersPage} from './MembersPage';
import type {HalResponse} from '../../api';
import {labels} from '../../localization';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({displayHalForm: vi.fn(), currentFormRequest: null, closeForm: vi.fn()}),
}));

vi.mock('../../hooks/useHalFormData', () => ({
    useHalFormData: (_t: unknown, resourceData: Record<string, unknown>) => ({
        formData: resourceData ?? {},
        isLoadingTargetData: false,
        targetFetchError: null,
        refetchTargetData: vi.fn(),
    }),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'})},
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

vi.mock('../../components/members/PermissionsDialog', () => ({
    PermissionsDialog: () => null,
}));

vi.mock('../../hooks/usePermissionsEditor', () => ({
    usePermissionsEditor: vi.fn().mockReturnValue({permissions: undefined, isLoading: false, onSave: vi.fn(), isSaving: false, error: null}),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title?: string}) =>
        isOpen ? <div role="dialog" aria-label={title}>{children}</div> : null,
}));

const SUSPEND_URL = '/api/members/member-1/suspend';
const GROUP_URL = 'http://localhost/api/training-groups/g-1';
const ADD_TRAINER_URL = '/api/training-groups/g-1/trainers';

const trainingGroup = {groupId: 'g-1', groupName: 'U10 Závoďáci', groupType: 'TRAINING', groupLink: GROUP_URL};
const debt = {balance: {amount: -350, currency: 'CZK'}, accountLink: '/api/members/member-1/account'};

const blocked409 = (groups: unknown, debtBody: unknown = null) => new ApiError(409, {params: {groups, debt: debtBody}});

const membersResource = (): HalResponse => ({
    _links: {self: {href: 'http://localhost/api/members'}},
    _embedded: {
        memberSummaryResponseList: [{
            id: 'member-1',
            registrationNumber: 'REG-001',
            lastName: 'Novák',
            firstName: 'Jan',
            active: true,
            _links: {self: {href: '/api/members/member-1'}},
            _templates: {
                suspendMember: mockHalFormsTemplate({method: 'POST', target: SUSPEND_URL, title: 'Ukončit členství člena', properties: []}),
            },
        }],
    },
    page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
});

const groupResource = (): HalResponse => ({
    _links: {self: {href: GROUP_URL}},
    _templates: {
        addTrainer: mockHalFormsTemplate({method: 'POST', target: ADD_TRAINER_URL, title: 'Přidat trenéra', properties: []}),
    },
});

const ok = {data: null, location: null};

const setup = (suspendResponses: Array<{error?: unknown}>) => {
    const calls: Array<{url: string; data: unknown}> = [];
    let suspendCall = 0;
    const mutate = vi.fn((vars: {url: string; data: unknown}, opts: {onError?: (e: unknown) => void; onSuccess?: (r: unknown) => void}) => {
        calls.push(vars);
        if (vars.url === SUSPEND_URL) {
            const outcome = suspendResponses[suspendCall++] ?? {};
            if (outcome.error) opts.onError?.(outcome.error);
            else opts.onSuccess?.(ok);
        } else {
            opts.onSuccess?.(ok);
        }
    });
    vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false, error: null} as unknown as ReturnType<typeof useAuthorizedMutation>);

    const resource = membersResource();
    vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined, options?: {select?: (d: unknown) => unknown}) => {
        if (url === GROUP_URL) return {data: groupResource(), error: null, isLoading: false};
        return {data: options?.select ? options.select(resource) : resource, error: null, isLoading: false};
    }) as unknown as typeof useAuthorizedQuery);
    vi.mocked(useHalPageData).mockReturnValue(makePageData({
        resourceData: resource,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
    }));

    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter initialEntries={['/members']}>
                <ToastProvider>
                    <MembersPage/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
    return {calls};
};

const submitSuspension = () => {
    fireEvent.click(screen.getByRole('button', {name: labels.templates.suspendMember}));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', {name: /uložit|odeslat|potvrdit/i}));
};

const warningDialog = (title = labels.suspensionWarning.title) => screen.getByRole('dialog', {name: title});

describe('MembersPage - suspension warning dialog with groups', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('lists the blocking group and closes the suspension form', async () => {
        setup([{error: blocked409({affectedGroups: [trainingGroup]})}]);
        submitSuspension();

        expect(await screen.findByRole('dialog', {name: labels.suspensionWarning.title})).toBeInTheDocument();
        expect(screen.getAllByRole('dialog')).toHaveLength(1);
        expect(screen.getByText('U10 Závoďáci')).toBeInTheDocument();
    });

    it('shows debt and groups in one dialog', async () => {
        setup([{error: blocked409({affectedGroups: [trainingGroup]}, debt)}]);
        submitSuspension();

        const dialog = await screen.findByRole('dialog', {name: labels.suspensionWarning.title});
        expect(screen.getAllByRole('dialog')).toHaveLength(1);
        expect(within(dialog).getByText('U10 Závoďáci')).toBeInTheDocument();
        expect(within(dialog).getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
    });

    it('debt-only 409 shows the balance and the account link', async () => {
        setup([{error: blocked409(null, debt)}]);
        submitSuspension();

        const dialog = await screen.findByRole('dialog', {name: labels.negativeBalanceWarning.title});
        expect(within(dialog).getByText(/350/)).toBeInTheDocument();
        expect(within(dialog).getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
    });

    it('resolving the group enables "Ukončit členství" that resubmits the stored request', async () => {
        const {calls} = setup([{error: blocked409({affectedGroups: [trainingGroup]})}, {}]);
        submitSuspension();
        await screen.findByRole('dialog', {name: labels.suspensionWarning.title});
        expect(within(warningDialog()).queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();

        fireEvent.click(within(warningDialog()).getByRole('button', {name: labels.suspensionWarning.designateSuccessor}));
        const trainerForm = screen.getAllByRole('dialog').find(d => d !== warningDialog())!;
        fireEvent.click(within(trainerForm).getByRole('button', {name: /uložit|odeslat|potvrdit/i}));

        await waitFor(() => expect(within(warningDialog()).getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument());
        fireEvent.click(within(warningDialog()).getByRole('button', {name: labels.suspensionWarning.finishSuspension}));

        const suspendCalls = calls.filter(c => c.url === SUSPEND_URL);
        expect(suspendCalls).toHaveLength(2);
        expect(suspendCalls[1]).toEqual(suspendCalls[0]);
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('resolved group with debt offers no "Ukončit členství" and asks to settle the balance', async () => {
        setup([{error: blocked409({affectedGroups: [trainingGroup]}, debt)}]);
        submitSuspension();
        await screen.findByRole('dialog', {name: labels.suspensionWarning.title});

        fireEvent.click(within(warningDialog()).getByRole('button', {name: labels.suspensionWarning.designateSuccessor}));
        const trainerForm = screen.getAllByRole('dialog').find(d => d !== warningDialog())!;
        fireEvent.click(within(trainerForm).getByRole('button', {name: /uložit|odeslat|potvrdit/i}));

        await waitFor(() => expect(within(warningDialog()).getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument());
        expect(within(warningDialog()).queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();
        expect(within(warningDialog()).getByText(labels.negativeBalanceWarning.description)).toBeInTheDocument();
    });

    it('rebuilds the dialog from a repeated 409 on resubmission', async () => {
        setup([
            {error: blocked409({affectedGroups: [trainingGroup]})},
            {error: blocked409(null, debt)},
        ]);
        submitSuspension();
        await screen.findByRole('dialog', {name: labels.suspensionWarning.title});
        fireEvent.click(within(warningDialog()).getByRole('button', {name: labels.suspensionWarning.designateSuccessor}));
        const trainerForm = screen.getAllByRole('dialog').find(d => d !== warningDialog())!;
        fireEvent.click(within(trainerForm).getByRole('button', {name: /uložit|odeslat|potvrdit/i}));
        await waitFor(() => expect(within(warningDialog()).getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument());

        fireEvent.click(within(warningDialog()).getByRole('button', {name: labels.suspensionWarning.finishSuspension}));

        const rebuilt = await screen.findByRole('dialog', {name: labels.negativeBalanceWarning.title});
        expect(within(rebuilt).getByText(/350/)).toBeInTheDocument();
        expect(screen.queryByText('U10 Závoďáci')).not.toBeInTheDocument();
    });
});
