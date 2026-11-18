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
});
