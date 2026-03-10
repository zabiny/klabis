/**
 * API functions for password setup flow
 * These endpoints are public (no authentication required)
 */

import type {
    ErrorResponse,
    PasswordSetupResponse,
    SetPasswordRequest,
    TokenRequestRequest,
    TokenRequestResponse,
    ValidateTokenResponse,
} from './index';

// Re-export types for convenience
export type {
    SetPasswordRequest,
    PasswordSetupResponse,
    TokenRequestRequest,
    TokenRequestResponse,
    ValidateTokenResponse,
    ErrorResponse,
};

/**
 * Validates a password setup token
 * @param token - The plain text token from email link
 */
export async function validateToken(token: string): Promise<ValidateTokenResponse> {
    const url = `/api/auth/password-setup/validate?token=${encodeURIComponent(token)}`;
    const response = await fetch(url, {
        headers: {
            Accept: 'application/json',
        },
    });

    if (!response.ok) {
        const errorData: ErrorResponse = await response.json().catch(() => ({ message: response.statusText }));
        throw new TokenValidationError(
            errorData.detail || 'Token validation failed',
            response.status,
            errorData
        );
    }

    return response.json();
}

/**
 * Completes password setup by setting the user's password
 * @param request - Token and password data
 */
export async function completePasswordSetup(request: SetPasswordRequest): Promise<PasswordSetupResponse> {
    const response = await fetch('/api/auth/password-setup/complete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
        },
        body: JSON.stringify(request),
    });

    if (!response.ok) {
        const errorData: ErrorResponse = await response.json().catch(() => ({ message: response.statusText }));
        throw new PasswordSetupError(
            errorData.detail || 'Password setup failed',
            response.status,
            errorData
        );
    }

    return response.json();
}

/**
 * Requests a new password setup token
 * @param request - Registration number and email
 */
export async function requestNewToken(request: TokenRequestRequest): Promise<TokenRequestResponse> {
    const response = await fetch('/api/auth/password-setup/request', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
        },
        body: JSON.stringify(request),
    });

    // Rate limited (429)
    if (response.status === 429) {
        const retryAfter = response.headers.get('Retry-After');
        throw new RateLimitError(
            'Too many token requests. Please try again later.',
            retryAfter ? parseInt(retryAfter) : undefined
        );
    }

    if (!response.ok) {
        const errorData: ErrorResponse = await response.json().catch(() => ({ message: response.statusText }));
        throw new TokenRequestError(
            errorData.detail || 'Token request failed',
            response.status,
            errorData
        );
    }

    return response.json();
}

// Custom error classes
export class TokenValidationError extends Error {
    status: number;
    detail: import('./index').ErrorResponse;

    constructor(
        message: string,
        status: number,
        detail: ErrorResponse
    ) {
        super(message);
        this.name = 'TokenValidationError';
        this.status = status;
        this.detail = detail;
    }
}

export class PasswordSetupError extends Error {
    status: number;
    detail: ErrorResponse;

    constructor(
        message: string,
        status: number,
        detail: ErrorResponse
    ) {
        super(message);
        this.name = 'PasswordSetupError';
        this.status = status;
        this.detail = detail;
    }

    /**
     * Extracts error message from the response detail
     */
    getErrorMessage(): string {
        return this.detail.detail || 'Neznámá chyba';
    }
}

export class TokenRequestError extends Error {
    status: number;
    detail: ErrorResponse;

    constructor(
        message: string,
        status: number,
        detail: ErrorResponse
    ) {
        super(message);
        this.name = 'TokenRequestError';
        this.status = status;
        this.detail = detail;
    }
}

export class RateLimitError extends Error {
    retryAfter: number | undefined;

    constructor(
        message: string,
        retryAfter: number | undefined
    ) {
        super(message);
        this.name = 'RateLimitError';
        this.retryAfter = retryAfter;
    }
}
