import {useCallback, useEffect, useRef, useState} from 'react';
import {completePasswordSetup, type SetPasswordRequest} from '../api/passwordSetup';
import {ApiError} from '../api/apiError';
import {describeError} from '../localization';

export interface UsePasswordSetupOptions {
    onSuccess: (registrationNumber: string) => void;
}

export interface UsePasswordSetupResult {
    onSubmit: (data: { password: string; passwordConfirmation: string }) => void;
    isSubmitting: boolean;
    serverError: string | null;
    serverFieldErrors: Record<string, string>;
    onClearServerError: () => void;
    onClearServerFieldError: (field: string) => void;
    showSuccess: boolean;
}

export function usePasswordSetup(
    token: string,
    options: UsePasswordSetupOptions,
): UsePasswordSetupResult {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [serverError, setServerError] = useState<string | null>(null);
    const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
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
            setServerFieldErrors({});
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
                if (error instanceof ApiError && error.field) {
                    setServerFieldErrors({[error.field]: describeError(error)});
                } else if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
                    setServerFieldErrors(error.fieldErrors);
                } else {
                    setServerError(describeError(error));
                }
            } finally {
                setIsSubmitting(false);
            }
        },
        [token, options],
    );

    const onClearServerError = useCallback(() => {
        setServerError(null);
        setServerFieldErrors({});
    }, []);

    const onClearServerFieldError = useCallback((field: string) => {
        setServerFieldErrors((prev) => {
            if (!(field in prev)) return prev;
            const rest = {...prev};
            delete rest[field];
            return rest;
        });
    }, []);

    return {
        onSubmit,
        isSubmitting,
        serverError,
        serverFieldErrors,
        onClearServerError,
        onClearServerFieldError,
        showSuccess,
    };
}
