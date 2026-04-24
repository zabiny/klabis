import {useCallback, useEffect, useRef, useState} from 'react';
import {
    RateLimitError,
    requestNewToken,
    TokenRequestError,
    type TokenRequestRequest,
} from '../api/passwordSetup';
import {labels} from '../localization';

export interface UseRequestNewTokenOptions {
    onSuccess: () => void;
}

export interface UseRequestNewTokenResult {
    onSubmit: (data: { registrationNumber: string; email: string }) => void;
    isSubmitting: boolean;
    serverError: string | null;
    onClearServerError: () => void;
    showSuccess: boolean;
}

export function useRequestNewToken(
    options: UseRequestNewTokenOptions,
): UseRequestNewTokenResult {
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
        async (data: { registrationNumber: string; email: string }) => {
            setServerError(null);
            setIsSubmitting(true);

            try {
                const request: TokenRequestRequest = {
                    registrationNumber: data.registrationNumber,
                    email: data.email,
                };

                await requestNewToken(request);
                setShowSuccess(true);

                successTimeoutRef.current = setTimeout(() => {
                    options.onSuccess();
                }, 3000);
            } catch (error) {
                if (error instanceof RateLimitError) {
                    const retryText = error.retryAfter
                        ? labels.errors.rateLimitedRetryAfter.replace('{seconds}', String(error.retryAfter))
                        : labels.errors.rateLimitedRetryLater;
                    setServerError(retryText);
                } else if (error instanceof TokenRequestError) {
                    setServerError(error.detail.detail || labels.errors.requestFailed);
                } else {
                    setServerError(labels.errors.unexpectedError);
                }
            } finally {
                setIsSubmitting(false);
            }
        },
        [options],
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
