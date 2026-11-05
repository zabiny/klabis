import {act, renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiError} from '../api/apiError';
import {usePasswordSetup} from './usePasswordSetup';

vi.mock('../api/passwordSetup', () => ({
    completePasswordSetup: vi.fn(),
}));

import {completePasswordSetup} from '../api/passwordSetup';

const submit = async (error: unknown) => {
    vi.mocked(completePasswordSetup).mockRejectedValue(error);
    const {result} = renderHook(() => usePasswordSetup('token', {onSuccess: vi.fn()}));
    await act(async () => {
        result.current.onSubmit({password: 'p', passwordConfirmation: 'p'});
    });
    await waitFor(() => expect(result.current.isSubmitting).toBe(false));
    return result;
};

describe('usePasswordSetup', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the Czech text of the violated password requirements', async () => {
        const result = await submit(new ApiError(400, {
            type: '/errors/password-requirements-not-met',
            detail: 'Password too weak',
            params: {violations: ['TOO_SHORT', 'MISSING_DIGIT'], minLength: 12},
        }));

        expect(result.current.serverError)
            .toBe('Heslo musí mít alespoň 12 znaků, obsahovat číslici.');
    });

    it('binds passwords-do-not-match to the confirmation field', async () => {
        const result = await submit(new ApiError(400, {
            type: '/errors/passwords-do-not-match',
            field: 'passwordConfirmation',
            detail: 'Passwords do not match',
        }));

        expect(result.current.serverFieldErrors).toEqual({passwordConfirmation: 'Hesla se neshodují.'});
        expect(result.current.serverError).toBeNull();
    });

    it('binds bean validation fieldErrors to their fields', async () => {
        const result = await submit(new ApiError(400, {
            type: '/errors/validation-failed',
            fieldErrors: {password: 'nesmí být prázdné'},
        }));

        expect(result.current.serverFieldErrors).toEqual({password: 'nesmí být prázdné'});
        expect(result.current.serverError).toBeNull();
    });

    it('clears a single server field error', async () => {
        const result = await submit(new ApiError(400, {
            type: '/errors/validation-failed',
            fieldErrors: {password: 'a', passwordConfirmation: 'b'},
        }));

        act(() => result.current.onClearServerFieldError('password'));

        expect(result.current.serverFieldErrors).toEqual({passwordConfirmation: 'b'});
    });

    it.each([
        ['/errors/token-expired', 'Platnost odkazu vypršela. Požádejte o nový odkaz.'],
        ['/errors/token-already-used', 'Tento odkaz již byl použit.'],
        ['/errors/invalid-token', 'Odkaz pro nastavení hesla není platný.'],
    ])('describes %s in Czech', async (type, expected) => {
        const result = await submit(new ApiError(410, {type, detail: 'English detail'}));

        expect(result.current.serverError).toBe(expected);
    });

    it('never shows the English detail of an unknown error', async () => {
        const result = await submit(new ApiError(400, {detail: 'Something English'}));

        expect(result.current.serverError).not.toContain('English');
    });

    it('shows the generic text for a network failure', async () => {
        const result = await submit(new TypeError('Failed to fetch'));

        expect(result.current.serverError).toBe('Došlo k neočekávané chybě. Zkuste to prosím znovu.');
    });
});
