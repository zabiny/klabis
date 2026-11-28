import '@testing-library/jest-dom';
import React from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {buildMemberRow} from '../../__mocks__/membersPageTest';
import {
    blocked409,
    blockingTrainingGroup,
    installSuspendMutation,
    SUSPENSION_GROUP_URL,
    trainingGroupResource,
    type SuspendOutcome,
} from '../../__mocks__/suspensionFlowTest';
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

const membersResource = (): HalResponse => ({
    _links: {self: {href: 'http://localhost/api/members'}},
    _embedded: {
        memberSummaryResponseList: [buildMemberRow({
            _templates: {
                suspendMember: mockHalFormsTemplate({method: 'POST', target: SUSPEND_URL, title: 'Ukončit členství člena', properties: []}),
            },
        })],
    },
    page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
});

const setup = (suspendResponses: SuspendOutcome[]) => {
    const {calls} = installSuspendMutation(SUSPEND_URL, suspendResponses);

    const resource = membersResource();
    vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined, options?: {select?: (d: unknown) => unknown}) => {
        if (url === SUSPENSION_GROUP_URL) return {data: trainingGroupResource(), error: null, isLoading: false};
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

    it('409 closes the suspension form, lists the blocking group and, once resolved, resubmits the stored request', async () => {
        const {calls} = setup([{error: blocked409({affectedGroups: [blockingTrainingGroup]})}, {}]);
        submitSuspension();

        await screen.findByRole('dialog', {name: labels.suspensionWarning.title});
        expect(screen.getAllByRole('dialog')).toHaveLength(1);
        expect(screen.getByText('U10 Závoďáci')).toBeInTheDocument();

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
});
