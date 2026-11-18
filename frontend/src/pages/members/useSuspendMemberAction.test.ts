import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useSuspendMemberAction} from './useSuspendMemberAction';
import {ApiError} from '../../api/authorizedFetch';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(),
}));

const group = {groupId: 'g1', groupName: 'Skupina A', groupType: 'TRAINING', groupLink: 'http://localhost/api/training-groups/g1'};
const debt = {balance: {amount: -350, currency: 'CZK'}, accountLink: '/api/members/abc-123/account'};

const groups409 = () => new ApiError(409, {params: {groups: {affectedGroups: [group]}, debt: null}});
const debt409 = () => new ApiError(409, {params: {debt, groups: null}});
const both409 = () => new ApiError(409, {params: {debt, groups: {affectedGroups: [group]}}});

const request = {url: '/api/members/m1/suspend', data: {reason: 'ODHLASKA', note: 'pozn'}};

let mutate: ReturnType<typeof vi.fn>;

beforeEach(() => {
    mutate = vi.fn();
    vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false} as unknown as ReturnType<typeof useAuthorizedMutation>);
});

describe('useSuspendMemberAction', () => {
    it('stores groups and debt from a 409 and reports it handled', () => {
        const {result} = renderHook(() => useSuspendMemberAction());

        let handled: true | undefined;
        act(() => {
            handled = result.current.onSubmitError(both409(), request);
        });

        expect(handled).toBe(true);
        expect(result.current.warning).toEqual({groups: [group], debt});
    });

    it('handles a debt-only 409', () => {
        const {result} = renderHook(() => useSuspendMemberAction());

        act(() => {
            result.current.onSubmitError(debt409(), request);
        });

        expect(result.current.warning).toEqual({groups: null, debt});
    });

    it('ignores unknown errors so default error handling applies', () => {
        const {result} = renderHook(() => useSuspendMemberAction());

        let handled: true | undefined;
        act(() => {
            handled = result.current.onSubmitError(new ApiError(500), request);
        });
        act(() => {
            result.current.onSubmitError(new ApiError(409, {params: {message: 'other'}}), request);
        });

        expect(handled).toBeUndefined();
        expect(result.current.warning).toBeNull();
    });

    it('clearWarning closes the warning', () => {
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });

        act(() => {
            result.current.clearWarning();
        });

        expect(result.current.warning).toBeNull();
    });

    it('finishSuspension resubmits the stored request', () => {
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });

        act(() => {
            result.current.finishSuspension();
        });

        expect(mutate).toHaveBeenCalledWith(request, expect.any(Object));
    });

    it('closes the warning after a successful resubmission', () => {
        mutate.mockImplementation((_r, opts) => opts.onSuccess({data: null, location: null}));
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });

        act(() => {
            result.current.finishSuspension();
        });

        expect(result.current.warning).toBeNull();
    });

    it('rebuilds the warning and forgets resolved groups on a repeated 409', () => {
        mutate.mockImplementation((_r, opts) => opts.onError(debt409()));
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });
        act(() => {
            result.current.markGroupResolved('g1');
        });
        expect(result.current.resolvedGroupIds.has('g1')).toBe(true);

        act(() => {
            result.current.finishSuspension();
        });

        expect(result.current.warning).toEqual({groups: null, debt});
        expect(result.current.resolvedGroupIds.size).toBe(0);
    });

    it('keeps the request of the latest 409 for resubmission', () => {
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });
        const second = {url: '/api/members/m2/suspend', data: {reason: 'PRESTUP'}};
        act(() => {
            result.current.onSubmitError(groups409(), second);
        });

        act(() => {
            result.current.finishSuspension();
        });

        expect(mutate).toHaveBeenCalledWith(second, expect.any(Object));
    });

    it('tracks several resolved groups independently', () => {
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.markGroupResolved('g1');
        });
        act(() => {
            result.current.markGroupResolved('g2');
        });

        expect([...result.current.resolvedGroupIds]).toEqual(['g1', 'g2']);
    });

    it('drops the stored request when an error arrives without one', () => {
        const {result} = renderHook(() => useSuspendMemberAction());
        act(() => {
            result.current.onSubmitError(groups409(), request);
        });
        act(() => {
            result.current.onSubmitError(groups409());
        });

        act(() => {
            result.current.finishSuspension();
        });

        expect(mutate).not.toHaveBeenCalled();
    });
    describe('whole legal guardian group suspension', () => {
        it('resubmits the stored request with the chosen cascadeGroupIds', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(groups409(), request);
            });
            act(() => {
                result.current.chooseCascade('g1');
            });

            act(() => {
                result.current.finishSuspension();
            });

            expect(mutate).toHaveBeenCalledWith(
                {url: request.url, data: {...request.data, cascadeGroupIds: ['g1']}},
                expect.any(Object),
            );
        });

        it('does not send cascadeGroupIds when none is chosen', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(groups409(), request);
            });

            act(() => {
                result.current.finishSuspension();
            });

            expect(mutate).toHaveBeenCalledWith(request, expect.any(Object));
        });

        it('drops a cancelled group from the resubmission', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(groups409(), request);
            });
            act(() => {
                result.current.chooseCascade('g1');
            });
            act(() => {
                result.current.cancelCascade('g1');
            });
            expect(result.current.cascadeGroupIds.size).toBe(0);

            act(() => {
                result.current.finishSuspension();
            });

            expect(mutate).toHaveBeenCalledWith(request, expect.any(Object));
        });

        const legal = {groupId: 'lg', groupName: 'Rodina', groupType: 'LEGAL_GUARDIAN', groupLink: 'http://localhost/api/legal-guardian-groups/lg'};
        const dependentsOnly409 = () => new ApiError(409, {params: {
            dependentMembers: [{memberId: 'k1', memberName: 'Anna Nováková', debt}],
        }});

        it('keeps the chosen cascade and its row when the resubmission is blocked by dependents', () => {
            mutate.mockImplementation((_r, opts) => opts.onError(dependentsOnly409()));
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(new ApiError(409, {params: {groups: {affectedGroups: [legal]}}}), request);
            });
            act(() => {
                result.current.chooseCascade('lg');
            });

            act(() => {
                result.current.finishSuspension();
            });

            expect(result.current.cascadeGroupIds.has('lg')).toBe(true);
            expect(result.current.warning?.groups).toEqual([legal]);
            expect(result.current.warning?.dependentMembers).toEqual([{memberId: 'k1', memberName: 'Anna Nováková', debt}]);
        });

        it('does not duplicate a chosen group that the fresh 409 reports again', () => {
            mutate.mockImplementation((_r, opts) => opts.onError(new ApiError(409, {params: {
                groups: {affectedGroups: [legal]},
                dependentMembers: [{memberId: 'k1', memberName: 'Anna Nováková', debt}],
            }})));
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(new ApiError(409, {params: {groups: {affectedGroups: [legal]}}}), request);
            });
            act(() => {
                result.current.chooseCascade('lg');
            });

            act(() => {
                result.current.finishSuspension();
            });

            expect(result.current.warning?.groups).toEqual([legal]);
        });

        it('starts fresh when a new form submission is blocked', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            act(() => {
                result.current.onSubmitError(new ApiError(409, {params: {groups: {affectedGroups: [legal]}}}), request);
            });
            act(() => {
                result.current.chooseCascade('lg');
            });

            act(() => {
                result.current.onSubmitError(groups409(), request);
            });

            expect(result.current.cascadeGroupIds.size).toBe(0);
            expect(result.current.warning?.groups).toEqual([group]);
        });

        it('handles a 409 that carries only dependent member blockers', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let handled: true | undefined;
            act(() => {
                handled = result.current.onSubmitError(new ApiError(409, {params: {
                    dependentMembers: [{memberId: 'k1', memberName: 'Anna Nováková', debt}],
                }}), request);
            });

            expect(handled).toBe(true);
            expect(result.current.warning?.dependentMembers).toHaveLength(1);
        });
    });
});
