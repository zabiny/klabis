import '@testing-library/jest-dom';
import React from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SuspensionWarningDialog} from './SuspensionWarningDialog';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {ApiError} from '../../api/authorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {labels} from '../../localization';
import type {AffectedGroup, SuspensionBlockedWarning} from './suspensionUtils';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => mockNavigate,
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(() => ({mutate: vi.fn(), isPending: false, error: null})),
}));

vi.mock('../../hooks/useHalFormData', () => ({
    useHalFormData: (_t: unknown, resourceData: Record<string, unknown>) => ({
        formData: resourceData ?? {},
        isLoadingTargetData: false,
        targetFetchError: null,
        refetchTargetData: vi.fn(),
    }),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, title, context}: {isOpen: boolean; children: React.ReactNode; title?: string; context?: React.ReactNode}) =>
        isOpen ? <div role="dialog" aria-label={title}>{context}{children}</div> : null,
}));

const trainingGroup: AffectedGroup = {groupId: 'g-3', groupName: 'U10 Závoďáci', groupType: 'TRAINING', groupLink: 'http://localhost/api/training-groups/g-3'};
const otherTrainingGroup: AffectedGroup = {groupId: 'g-4', groupName: 'U12 Závoďáci', groupType: 'TRAINING', groupLink: 'http://localhost/api/training-groups/g-4'};
const freeGroup: AffectedGroup = {groupId: 'g-1', groupName: 'Trail Runners', groupType: 'FREE', groupLink: 'http://localhost/api/groups/g-1'};
const legalGroup: AffectedGroup = {groupId: 'g-2', groupName: 'Rodina Novákových', groupType: 'LEGAL_GUARDIAN', groupLink: 'http://localhost/api/legal-guardian-groups/g-2'};
const debt = {balance: {amount: -250, currency: 'CZK'}, accountLink: '/api/members/abc-123/account'};

const groupResource = (templates: Record<string, unknown>) => ({
    _links: {self: {href: 'http://localhost/api/training-groups/g-3'}},
    _templates: templates,
});
const addTrainer = mockHalFormsTemplate({method: 'POST', target: '/api/training-groups/g-3/trainers', title: 'Přidat trenéra', properties: []});
const deleteTrainingGroup = mockHalFormsTemplate({method: 'DELETE', target: '/api/training-groups/g-3', properties: []});

const addGroupOwner = mockHalFormsTemplate({method: 'POST', target: '/api/groups/g-1/owners', title: 'Přidat vlastníka', properties: []});
const deleteGroup = mockHalFormsTemplate({method: 'DELETE', target: '/api/groups/g-1', title: 'Smazat skupinu', properties: []});
const freeGroupResource = (templates: Record<string, unknown>, members: unknown[] = [{memberId: 'm-2'}]) => ({
    _links: {self: {href: 'http://localhost/api/groups/g-1'}},
    members,
    _templates: templates,
});

const mockGroupQueries = (byUrl: Record<string, {data?: unknown; error?: unknown; isLoading?: boolean}>) => {
    vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined) =>
        ({data: undefined, error: null, isLoading: false, ...(url ? byUrl[url] : {})})) as unknown as typeof useAuthorizedQuery);
};

const defaultProps = () => ({
    warning: {groups: [trainingGroup], debt: null} as SuspensionBlockedWarning | null,
    resolvedGroupIds: new Set<string>() as ReadonlySet<string>,
    cascadeGroupIds: new Set<string>() as ReadonlySet<string>,
    suspendedMemberName: 'Jan Novák',
    isFinishing: false,
    onClose: vi.fn(),
    onGroupResolved: vi.fn(),
    onCascadeChosen: vi.fn(),
    onCascadeCancelled: vi.fn(),
    onFinish: vi.fn(),
});

const renderDialog = (overrides: Partial<ReturnType<typeof defaultProps>> = {}) => {
    const props = {...defaultProps(), ...overrides};
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <SuspensionWarningDialog {...props}/>
            </MemoryRouter>
        </QueryClientProvider>
    );
    return props;
};

describe('SuspensionWarningDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockGroupQueries({[trainingGroup.groupLink]: {data: groupResource({addTrainer})}});
    });

    it('renders nothing without a warning', () => {
        renderDialog({warning: null});
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders title, description and group names', () => {
        renderDialog({warning: {groups: [trainingGroup, freeGroup], debt: null}});
        expect(screen.getByRole('dialog', {name: labels.suspensionWarning.title})).toBeInTheDocument();
        expect(screen.getByText(/posledním správcem/i)).toBeInTheDocument();
        expect(screen.getByText('U10 Závoďáci')).toBeInTheDocument();
        expect(screen.getByText('Trail Runners')).toBeInTheDocument();
    });

    it('shows human-readable group type labels', () => {
        renderDialog({warning: {groups: [trainingGroup, freeGroup, legalGroup], debt: null}});
        expect(screen.getByText('Tréninková skupina')).toBeInTheDocument();
        expect(screen.getByText('Volná skupina')).toBeInTheDocument();
        expect(screen.getByText('Skupina zákonných zástupců')).toBeInTheDocument();
    });

    it.each([
        ['training', trainingGroup],
        ['legal guardian', legalGroup],
    ])('requests the %s group resource from its groupLink', (_kind, group) => {
        renderDialog({warning: {groups: [group], debt: null}});
        expect(vi.mocked(useAuthorizedQuery)).toHaveBeenCalledWith(group.groupLink, expect.anything());
    });

    describe('training group row', () => {
        it('offers "Určit nástupce" when the group offers addTrainer', () => {
            renderDialog();
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
        });

        it('does not offer "Určit nástupce" without addTrainer', () => {
            mockGroupQueries({[trainingGroup.groupLink]: {data: groupResource({})}});
            renderDialog();
            expect(screen.queryByRole('button', {name: /určit nástupce/i})).not.toBeInTheDocument();
        });

        it('never offers "Rozpustit skupinu", even when deleteTrainingGroup is offered', () => {
            mockGroupQueries({[trainingGroup.groupLink]: {data: groupResource({addTrainer, deleteTrainingGroup})}});
            renderDialog();
            expect(screen.queryByRole('button', {name: /rozpustit skupinu/i})).not.toBeInTheDocument();
        });

        it('shows the training-groups-administrator contact text on 403', () => {
            mockGroupQueries({[trainingGroup.groupLink]: {error: new ApiError(403)}});
            renderDialog();
            expect(screen.getByText(/správce tréninkových skupin/i)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /určit nástupce/i})).not.toBeInTheDocument();
        });

        it('shows a load error for other failures', () => {
            mockGroupQueries({[trainingGroup.groupLink]: {error: new ApiError(500)}});
            renderDialog();
            expect(screen.getByText(labels.suspensionWarning.groupLoadFailed)).toBeInTheDocument();
        });

        it('opens the addTrainer form from "Určit nástupce"', () => {
            renderDialog();
            fireEvent.click(screen.getByRole('button', {name: /určit nástupce/i}));
            expect(screen.getByRole('dialog', {name: 'Přidat trenéra'})).toBeInTheDocument();
        });
    });

    it.each([
        ['training', trainingGroup, groupResource({addTrainer})],
        ['free', freeGroup, freeGroupResource({addGroupOwner, deleteGroup})],
    ])('shows the resolved marker and hides the actions for a resolved %s group row', (_kind, group, resource) => {
        mockGroupQueries({[group.groupLink]: {data: resource}});
        renderDialog({warning: {groups: [group], debt: null}, resolvedGroupIds: new Set([group.groupId])});
        expect(screen.getByText(new RegExp(labels.suspensionWarning.resolved))).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /určit nástupce|rozpustit skupinu/i})).not.toBeInTheDocument();
    });

    describe('free group row', () => {
        const renderFree = (resource: unknown, overrides: Partial<ReturnType<typeof defaultProps>> = {}) => {
            mockGroupQueries({[freeGroup.groupLink]: {data: resource}});
            return renderDialog({warning: {groups: [freeGroup], debt: null}, ...overrides});
        };

        it('offers both actions when addGroupOwner and deleteGroup are offered and the group has other members', () => {
            renderFree(freeGroupResource({addGroupOwner, deleteGroup}));
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /rozpustit skupinu/i})).toBeInTheDocument();
        });

        it('offers only "Rozpustit skupinu" when the group has no other members', () => {
            renderFree(freeGroupResource({addGroupOwner, deleteGroup}, []));
            expect(screen.queryByRole('button', {name: /určit nástupce/i})).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: /rozpustit skupinu/i})).toBeInTheDocument();
        });

        it('offers only "Určit nástupce" without deleteGroup', () => {
            renderFree(freeGroupResource({addGroupOwner}));
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /rozpustit skupinu/i})).not.toBeInTheDocument();
        });

        it('shows the contact text when no action is available', () => {
            renderFree(freeGroupResource({}));
            expect(screen.getByText(labels.suspensionWarning.contactFreeGroupAdmin)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /určit nástupce|rozpustit skupinu/i})).not.toBeInTheDocument();
        });

        it('shows the contact text on 403', () => {
            mockGroupQueries({[freeGroup.groupLink]: {error: new ApiError(403)}});
            renderDialog({warning: {groups: [freeGroup], debt: null}});
            expect(screen.getByText(labels.suspensionWarning.contactFreeGroupAdmin)).toBeInTheDocument();
        });

        it('opens the addGroupOwner form from "Určit nástupce"', () => {
            renderFree(freeGroupResource({addGroupOwner, deleteGroup}));
            fireEvent.click(screen.getByRole('button', {name: /určit nástupce/i}));
            expect(screen.getByRole('dialog', {name: labels.templates.addGroupOwner})).toBeInTheDocument();
        });

        it('lists exactly the options returned by the template options link, without client-side filtering', () => {
            const optionsHref = '/api/groups/g-1/member-options?role=MEMBER';
            const pickerTemplate = mockHalFormsTemplate({
                method: 'POST',
                target: '/api/groups/g-1/owners',
                properties: [{name: 'memberId', prompt: 'Člen', type: 'MemberId', required: true, options: {link: {href: optionsHref}}}],
            });
            const groupData = freeGroupResource({addGroupOwner: pickerTemplate, deleteGroup});
            vi.mocked(useAuthorizedQuery).mockImplementation(((url: string | undefined, opts?: {select?: (d: unknown) => unknown}) => {
                if (url === freeGroup.groupLink) return {data: groupData, error: null, isLoading: false};
                if (url === optionsHref) {
                    const raw = [{value: 'm-2', prompt: 'Petr Svoboda'}, {value: 'm-3', prompt: 'Alena Rodičová'}];
                    return {data: opts?.select ? opts.select(raw) : raw, error: null, isLoading: false};
                }
                return {data: undefined, error: null, isLoading: false};
            }) as unknown as typeof useAuthorizedQuery);
            renderDialog({warning: {groups: [freeGroup], debt: null}});
            fireEvent.click(screen.getByRole('button', {name: /určit nástupce/i}));
            expect(screen.getAllByRole('option').map(o => o.textContent).filter(t => t === 'Petr Svoboda' || t === 'Alena Rodičová'))
                .toEqual(['Petr Svoboda', 'Alena Rodičová']);
            expect(vi.mocked(useAuthorizedQuery)).toHaveBeenCalledWith(optionsHref, expect.anything());
        });

        it('asks for confirmation before dissolving', () => {
            renderFree(freeGroupResource({addGroupOwner, deleteGroup}));
            fireEvent.click(screen.getByRole('button', {name: /rozpustit skupinu/i}));
            const confirmation = screen.getByRole('dialog', {name: 'Smazat skupinu'});
            expect(within(confirmation).getByText(labels.suspensionWarning.dissolveConfirmation(freeGroup.groupName))).toBeInTheDocument();
        });

        it('keeps the group and the row unresolved when the confirmation is cancelled', () => {
            const props = renderFree(freeGroupResource({addGroupOwner, deleteGroup}));
            fireEvent.click(screen.getByRole('button', {name: /rozpustit skupinu/i}));
            const confirmation = screen.getByRole('dialog', {name: 'Smazat skupinu'});
            fireEvent.click(within(confirmation).getByRole('button', {name: /zrušit|zavřít/i}));
            expect(screen.queryByRole('dialog', {name: 'Smazat skupinu'})).not.toBeInTheDocument();
            expect(props.onGroupResolved).not.toHaveBeenCalled();
        });

        it('marks the row resolved after the group is dissolved', async () => {
            vi.mocked(useAuthorizedMutation).mockReturnValueOnce({
                mutate: (_vars: unknown, options: {onSuccess: (r: unknown) => void}) => options.onSuccess({data: undefined, location: null}),
                isPending: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);
            const props = renderFree(freeGroupResource({addGroupOwner, deleteGroup}));
            fireEvent.click(screen.getByRole('button', {name: /rozpustit skupinu/i}));
            const confirmation = screen.getByRole('dialog', {name: 'Smazat skupinu'});
            fireEvent.click(within(confirmation).getByRole('button', {name: /odeslat/i}));
            await waitFor(() => expect(props.onGroupResolved).toHaveBeenCalledWith(freeGroup.groupId));
        });
    });

    describe('legal guardian group row', () => {
        const legalResource = {
            _links: {self: {href: legalGroup.groupLink}},
            name: 'Rodina Skupinová',
            minors: [{firstName: 'Anna', lastName: 'Nováková'}, {firstName: 'Petr', lastName: 'Novák'}],
        };

        const renderLegal = (overrides: Partial<ReturnType<typeof defaultProps>> = {}) => {
            mockGroupQueries({[legalGroup.groupLink]: {data: legalResource}});
            return renderDialog({warning: {groups: [legalGroup], debt: null}, ...overrides});
        };

        it('offers "Pozastavit celou skupinu" and "Určit nástupce" but never dissolve', () => {
            renderLegal();
            expect(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /rozpustit skupinu/i})).not.toBeInTheDocument();
        });

        it('navigates to the legal guardian group page from "Určit nástupce"', () => {
            const props = renderLegal();
            fireEvent.click(screen.getByRole('button', {name: /určit nástupce/i}));
            expect(props.onClose).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith('/legal-guardian-groups/g-2');
        });

        it('lists the guardian and the minors from the group resource in the confirmation', () => {
            renderLegal();
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup}));
            const confirmation = screen.getByRole('dialog', {name: labels.suspensionWarning.suspendWholeGroupConfirmTitle});
            expect(within(confirmation).getByText('Jan Novák')).toBeInTheDocument();
            expect(within(confirmation).queryByText('Rodina Skupinová')).not.toBeInTheDocument();
            expect(within(confirmation).getByText('Anna Nováková')).toBeInTheDocument();
            expect(within(confirmation).getByText('Petr Novák')).toBeInTheDocument();
        });

        it('marks the row as chosen on confirmation without any request', () => {
            const mutate = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false, error: null} as unknown as ReturnType<typeof useAuthorizedMutation>);
            const props = renderLegal();
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup}));
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroupConfirm}));
            expect(props.onCascadeChosen).toHaveBeenCalledWith(legalGroup.groupId);
            expect(mutate).not.toHaveBeenCalled();
        });

        it('leaves the row unchanged when the confirmation is cancelled', () => {
            const props = renderLegal();
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup}));
            const confirmation = screen.getByRole('dialog', {name: labels.suspensionWarning.suspendWholeGroupConfirmTitle});
            fireEvent.click(within(confirmation).getByRole('button', {name: labels.buttons.cancel}));
            expect(screen.queryByRole('dialog', {name: labels.suspensionWarning.suspendWholeGroupConfirmTitle})).not.toBeInTheDocument();
            expect(props.onCascadeChosen).not.toHaveBeenCalled();
        });

        it('shows a chosen row as resolved with "Zrušit" and no group actions', () => {
            renderLegal({resolvedGroupIds: new Set(), cascadeGroupIds: new Set([legalGroup.groupId])});
            expect(screen.getByText(labels.suspensionWarning.suspendWholeGroupMarked, {exact: false})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: labels.suspensionWarning.suspendWholeGroup})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /určit nástupce/i})).not.toBeInTheDocument();
        });

        it('reverts the choice with "Zrušit"', () => {
            const props = renderLegal({cascadeGroupIds: new Set([legalGroup.groupId])});
            const row = screen.getByText(legalGroup.groupName).closest('li') as HTMLElement;
            fireEvent.click(within(row).getByRole('button', {name: labels.suspensionWarning.suspendWholeGroupCancel}));
            expect(props.onCascadeCancelled).toHaveBeenCalledWith(legalGroup.groupId);
        });

        it('lets the user finish once the whole-group suspension is the only open blocker', () => {
            const props = renderLegal({cascadeGroupIds: new Set([legalGroup.groupId])});
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.finishSuspension}));
            expect(props.onFinish).toHaveBeenCalledTimes(1);
        });

        it.each([
            ['cannot be loaded', {error: new ApiError(500)}, true],
            ['is loading', {isLoading: true}, false],
        ])('keeps "Určit nástupce" usable and the whole-group action disabled while the group %s', (_state, query, showsLoadError) => {
            mockGroupQueries({[legalGroup.groupLink]: query});
            renderDialog({warning: {groups: [legalGroup], debt: null}});
            expect(screen.queryByText(labels.suspensionWarning.groupLoadFailed) !== null).toBe(showsLoadError);
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeEnabled();
            expect(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup})).toBeDisabled();
        });
    });

    describe('blockers of the minors suspended together', () => {
        const dependents = [
            {memberId: 'k1', memberName: 'Anna Nováková', debt},
            {memberId: 'k2', memberName: 'Petr Novák', groups: [freeGroup]},
        ];

        it('lists every dependent member with its reason', () => {
            renderDialog({warning: {groups: [trainingGroup], debt: null, dependentMembers: dependents}, cascadeGroupIds: new Set(['g-2'])});
            expect(screen.getByText(labels.suspensionWarning.dependentMembersDescription)).toBeInTheDocument();
            const anna = screen.getByText('Anna Nováková').closest('li') as HTMLElement;
            expect(within(anna).getByText(/250/)).toBeInTheDocument();
            const petr = screen.getByText('Petr Novák').closest('li') as HTMLElement;
            expect(within(petr).getByText(/Trail Runners/)).toBeInTheDocument();
        });

        const cascadeWarning = () => ({groups: [legalGroup], debt: null, dependentMembers: dependents});

        it('has no "Ukončit členství" while the cascade is chosen and dependents block, and keeps the chosen row with "Zrušit"', () => {
            mockGroupQueries({[legalGroup.groupLink]: {data: {minors: []}}});
            renderDialog({warning: cascadeWarning(), cascadeGroupIds: new Set([legalGroup.groupId])});
            expect(screen.getByText(labels.suspensionWarning.dependentMembersTitle)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();
            const row = screen.getByText(legalGroup.groupName).closest('li') as HTMLElement;
            expect(within(row).getByRole('button', {name: labels.suspensionWarning.suspendWholeGroupCancel})).toBeInTheDocument();
        });

        it('hides the dependents and brings back both actions once the cascade is cancelled', () => {
            mockGroupQueries({[legalGroup.groupLink]: {data: {minors: []}}});
            renderDialog({warning: cascadeWarning()});
            expect(screen.queryByText('Anna Nováková')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.suspensionWarning.suspendWholeGroup})).toBeInTheDocument();
        });
    });

    describe('"Ukončit členství"', () => {
        it('is absent while a group is unresolved', () => {
            renderDialog();
            expect(screen.queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();
        });

        it('is shown when all groups are resolved and resubmits on click', () => {
            const props = renderDialog({resolvedGroupIds: new Set([trainingGroup.groupId])});
            fireEvent.click(screen.getByRole('button', {name: labels.suspensionWarning.finishSuspension}));
            expect(props.onFinish).toHaveBeenCalledTimes(1);
        });

        it.each([
            ['stays absent while only some of several groups are resolved', [trainingGroup.groupId], false],
            ['appears once every one of several groups is resolved', [trainingGroup.groupId, otherTrainingGroup.groupId], true],
        ])('%s', (_title, resolvedIds, offered) => {
            mockGroupQueries({
                [trainingGroup.groupLink]: {data: groupResource({addTrainer})},
                [otherTrainingGroup.groupLink]: {data: groupResource({addTrainer})},
            });
            renderDialog({
                warning: {groups: [trainingGroup, otherTrainingGroup], debt: null},
                resolvedGroupIds: new Set(resolvedIds),
            });
            expect(screen.queryByRole('button', {name: labels.suspensionWarning.finishSuspension}) !== null).toBe(offered);
        });
    });

    describe('negative balance in the same dialog', () => {
        it('shows the balance and the account link next to the groups in one dialog', () => {
            renderDialog({warning: {groups: [trainingGroup], debt}});
            expect(screen.getAllByRole('dialog')).toHaveLength(1);
            expect(screen.getByText(labels.negativeBalanceWarning.currentBalance, {exact: false})).toBeInTheDocument();
            expect(screen.getByText(/250/)).toBeInTheDocument();
            expect(screen.getByText('U10 Závoďáci')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
        });

        it('debt-only warning shows the balance and the account link without groups', () => {
            renderDialog({warning: {groups: null, debt}});
            expect(screen.getByRole('dialog', {name: labels.negativeBalanceWarning.title})).toBeInTheDocument();
            expect(screen.getByText(/250/)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.negativeBalanceWarning.openAccount})).toBeInTheDocument();
            expect(screen.queryByText(/posledním správcem/i)).not.toBeInTheDocument();
        });

        it('navigates to the account and closes the dialog on "Otevřít účet"', () => {
            const props = renderDialog({warning: {groups: null, debt}});
            fireEvent.click(screen.getByRole('button', {name: labels.negativeBalanceWarning.openAccount}));
            expect(props.onClose).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith('/members/abc-123/account');
        });

        it('has no "Ukončit členství" while debt is reported, even with all groups resolved, and says to settle the balance', () => {
            renderDialog({warning: {groups: [trainingGroup], debt}, resolvedGroupIds: new Set([trainingGroup.groupId])});
            expect(screen.queryByRole('button', {name: labels.suspensionWarning.finishSuspension})).not.toBeInTheDocument();
            expect(screen.getByText(labels.negativeBalanceWarning.description)).toBeInTheDocument();
        });
    });

    it('closes through the cancel button', () => {
        const props = renderDialog();
        fireEvent.click(screen.getByRole('button', {name: labels.buttons.cancel}));
        expect(props.onClose).toHaveBeenCalled();
    });
});
