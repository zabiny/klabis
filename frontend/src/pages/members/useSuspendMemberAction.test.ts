import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {useSuspendMemberAction} from './useSuspendMemberAction';
import {ApiError} from '../../api/authorizedFetch';

const makeAffectedGroups409 = () =>
    new ApiError(409, {params: {
            groups: {
                message: 'member is last owner',
                affectedGroups: [{groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'}],
            },
            debt: null,
        }});

const makeNegativeBalance409 = () =>
    new ApiError(409, {params: {
            debt: {
                balance: {amount: -350, currency: 'CZK'},
                accountLink: '/api/members/abc-123/account',
            },
            groups: null,
        }});

const makeBothBlockers409 = () =>
    new ApiError(409, {params: {
            debt: {
                balance: {amount: -350, currency: 'CZK'},
                accountLink: '/api/members/abc-123/account',
            },
            groups: {
                message: 'member is last owner',
                affectedGroups: [{groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'}],
            },
        }});

const makeUnknown409 = () =>
    new ApiError(409, {params: {message: 'some other conflict'}});

const makeNon409Error = () =>
    new ApiError(500);

describe('useSuspendMemberAction', () => {
    describe('suspension-blocked problem+json with only the present blocker', () => {
        it('reads groups and debt from params of the typed error', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            const error = new ApiError(409, {
                type: '/errors/suspension-blocked',
                params: {
                    groups: {affectedGroups: [{groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'}]},
                    debt: {balance: {amount: -350, currency: 'CZK'}, accountLink: '/api/members/abc/account'},
                },
            });

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(error);
            });

            expect(returnValue).toBe(true);
            expect(result.current.suspensionWarning).toHaveLength(1);
            expect(result.current.negativeBalanceWarning?.balance.amount).toBe(-350);
        });

        it('handles a debt-only error where groups is absent', () => {
            const {result} = renderHook(() => useSuspendMemberAction());
            const error = new ApiError(409, {
                type: '/errors/suspension-blocked',
                params: {debt: {balance: {amount: -10, currency: 'CZK'}, accountLink: '/api/members/abc/account'}},
            });

            act(() => {
                result.current.onSubmitError(error);
            });

            expect(result.current.suspensionWarning).toBeNull();
            expect(result.current.negativeBalanceWarning).not.toBeNull();
        });
    });

    describe('affected-groups 409', () => {
        it('returns true (handled) and sets suspensionWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeAffectedGroups409());
            });

            expect(returnValue).toBe(true);
            expect(result.current.suspensionWarning).toEqual([
                {groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'},
            ]);
        });

        it('does NOT close the action modal', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeAffectedGroups409());
            });
        });

        it('does NOT set negativeBalanceWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeAffectedGroups409());
            });

            expect(result.current.negativeBalanceWarning).toBeNull();
        });

        it('clearSuspensionWarning resets the warning to null', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeAffectedGroups409());
            });

            act(() => {
                result.current.clearSuspensionWarning();
            });

            expect(result.current.suspensionWarning).toBeNull();
        });
    });

    describe('negative-balance 409', () => {
        it('returns true (handled) and sets negativeBalanceWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeNegativeBalance409());
            });

            expect(returnValue).toBe(true);
            expect(result.current.negativeBalanceWarning).toEqual({
                balance: {amount: -350, currency: 'CZK'},
                accountLink: '/api/members/abc-123/account',
            });
        });

        it('does NOT set suspensionWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeNegativeBalance409());
            });

            expect(result.current.suspensionWarning).toBeNull();
        });

        it('clearNegativeBalanceWarning resets the warning to null', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeNegativeBalance409());
            });

            act(() => {
                result.current.clearNegativeBalanceWarning();
            });

            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('both blockers present (debt + groups)', () => {
        it('returns true (handled) and sets both warning states', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeBothBlockers409());
            });

            expect(returnValue).toBe(true);
            expect(result.current.negativeBalanceWarning).toEqual({
                balance: {amount: -350, currency: 'CZK'},
                accountLink: '/api/members/abc-123/account',
            });
            expect(result.current.suspensionWarning).toEqual([
                {groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'},
            ]);
        });

        it('closes the action modal (debt blocker present)', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeBothBlockers409());
            });
        });

        it('clearing one warning leaves the other intact', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeBothBlockers409());
            });

            act(() => {
                result.current.clearNegativeBalanceWarning();
            });

            expect(result.current.negativeBalanceWarning).toBeNull();
            expect(result.current.suspensionWarning).not.toBeNull();
        });
    });

    describe('unknown 409 (unrecognised body)', () => {
        it('returns undefined so HalFormDisplay falls back to default error handling', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeUnknown409());
            });

            expect(returnValue).toBeUndefined();
        });

        it('does not close the action modal', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeUnknown409());
            });
        });

        it('leaves both warning states null', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeUnknown409());
            });

            expect(result.current.suspensionWarning).toBeNull();
            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('non-409 error', () => {
        it('returns undefined so HalFormDisplay falls back to default error handling', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeNon409Error());
            });

            expect(returnValue).toBeUndefined();
        });

        it('does not close the action modal', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeNon409Error());
            });
        });

        it('leaves both warning states null', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            act(() => {
                result.current.onSubmitError(makeNon409Error());
            });

            expect(result.current.suspensionWarning).toBeNull();
            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('plain (non-ApiError) exception', () => {
        it('returns undefined for a plain Error', () => {
            const {result} = renderHook(() => useSuspendMemberAction());

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(new Error('network failure'));
            });

            expect(returnValue).toBeUndefined();
        });
    });
});
