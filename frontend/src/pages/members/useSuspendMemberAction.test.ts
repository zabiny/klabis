import {renderHook, act} from '@testing-library/react';
import {vi, describe, it, expect, beforeEach} from 'vitest';
import {useSuspendMemberAction} from './useSuspendMemberAction';
import {FetchError} from '../../api/authorizedFetch';

const makeAffectedGroups409 = () =>
    new FetchError(
        '409 Conflict',
        409,
        'Conflict',
        new Headers(),
        JSON.stringify({
            message: 'member is last owner',
            affectedGroups: [{groupId: 'g1', groupName: 'Skupina A', groupType: 'FREE'}],
        }),
    );

const makeNegativeBalance409 = () =>
    new FetchError(
        '409 Conflict',
        409,
        'Conflict',
        new Headers(),
        JSON.stringify({
            balance: {amount: -350, currency: 'CZK'},
            accountLink: '/api/members/abc-123/account',
        }),
    );

const makeUnknown409 = () =>
    new FetchError('409 Conflict', 409, 'Conflict', new Headers(), JSON.stringify({message: 'some other conflict'}));

const makeNon409Error = () =>
    new FetchError('500 Internal Server Error', 500, 'Internal Server Error', new Headers(), '{}');

describe('useSuspendMemberAction', () => {
    let closeActionModal: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        closeActionModal = vi.fn();
    });

    describe('affected-groups 409', () => {
        it('returns true (handled) and sets suspensionWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

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
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeAffectedGroups409());
            });

            expect(closeActionModal).not.toHaveBeenCalled();
        });

        it('does NOT set negativeBalanceWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeAffectedGroups409());
            });

            expect(result.current.negativeBalanceWarning).toBeNull();
        });

        it('clearSuspensionWarning resets the warning to null', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

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
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

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

        it('closes the action modal before opening the negative-balance dialog', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeNegativeBalance409());
            });

            expect(closeActionModal).toHaveBeenCalledTimes(1);
        });

        it('does NOT set suspensionWarning', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeNegativeBalance409());
            });

            expect(result.current.suspensionWarning).toBeNull();
        });

        it('clearNegativeBalanceWarning resets the warning to null', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeNegativeBalance409());
            });

            act(() => {
                result.current.clearNegativeBalanceWarning();
            });

            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('unknown 409 (unrecognised body)', () => {
        it('returns undefined so HalFormDisplay falls back to default error handling', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeUnknown409());
            });

            expect(returnValue).toBeUndefined();
        });

        it('does not close the action modal', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeUnknown409());
            });

            expect(closeActionModal).not.toHaveBeenCalled();
        });

        it('leaves both warning states null', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeUnknown409());
            });

            expect(result.current.suspensionWarning).toBeNull();
            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('non-409 error', () => {
        it('returns undefined so HalFormDisplay falls back to default error handling', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(makeNon409Error());
            });

            expect(returnValue).toBeUndefined();
        });

        it('does not close the action modal', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeNon409Error());
            });

            expect(closeActionModal).not.toHaveBeenCalled();
        });

        it('leaves both warning states null', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            act(() => {
                result.current.onSubmitError(makeNon409Error());
            });

            expect(result.current.suspensionWarning).toBeNull();
            expect(result.current.negativeBalanceWarning).toBeNull();
        });
    });

    describe('plain (non-FetchError) exception', () => {
        it('returns undefined for a plain Error', () => {
            const {result} = renderHook(() => useSuspendMemberAction({closeActionModal}));

            let returnValue: true | undefined;
            act(() => {
                returnValue = result.current.onSubmitError(new Error('network failure'));
            });

            expect(returnValue).toBeUndefined();
        });
    });
});
