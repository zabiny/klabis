import {useCallback, useEffect, useRef, useState} from 'react';
import {
    completePasswordSetup,
    PasswordSetupError,
    type SetPasswordRequest,
} from '../api/passwordSetup';
import {labels} from '../localization';

export interface UsePasswordSetupOptions {
    onSuccess: (registrationNumber: string) => void;
}

export interface UsePasswordSetupResult {
    onSubmit: (data: { password: string; passwordConfirmation: string }) => void;
    isSubmitting: boolean;
    serverError: string | null;
    onClearServerError: () => void;
    showSuccess: boolean;
}

export function usePasswordSetup(
    token: string,
    options: UsePasswordSetupOptions,
): UsePasswordSetupResult {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [serverError, setServerError] = useState<string | null>(null);
    const [showSuccess, setShowSuccess] = useState(false);
    const successTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    useEffect(() => () => {
        if (successTimeoutRef.current) {
            clearTimeout(successTimeoutRef.current);
        }
    }, []);

    const onSubmit = useCallback(
        async (data: { password: string; passwordConfirmation: string }) => {
            setServerError(null);
            setIsSubmitting(true);

            try {
                const request: SetPasswordRequest = {
                    token,
                    password: data.password,
                    passwordConfirmation: data.passwordConfirmation,
                };

                const response = await completePasswordSetup(request);
                setShowSuccess(true);

                successTimeoutRef.current = setTimeout(() => {
                    options.onSuccess(response.registrationNumber || '');
                }, 2000);
            } catch (error) {
                if (error instanceof PasswordSetupError) {
                    if (error.status === 400) {
                        setServerError(error.getErrorMessage());
                    } else if (error.status === 410) {
                        setServerError(labels.errors.tokenExpired);
                    } else if (error.status === 404) {
                        setServerError(labels.errors.tokenInvalid);
                    } else {
                        setServerError(error.getErrorMessage());
                    }
                } else {
                    setServerError(labels.errors.unexpectedError);
                }
            } finally {
                setIsSubmitting(false);
            }
        },
        [token, options],
    );

    const onClearServerError = useCallback(() => {
        setServerError(null);
    }, []);

    return {
        onSubmit,
        isSubmitting,
        serverError,
        onClearServerError,
        showSuccess,
    };
}
