import type {components} from './klabisApi';

export type ErrorType = components['schemas']['ErrorType'];

export const VALIDATION_FAILED: ErrorType = '/errors/validation-failed';

export interface ApiErrorDetails {
    type?: ErrorType | string;
    detail?: string;
    field?: string;
    fieldErrors?: Record<string, string>;
    errors?: string[];
    params?: Record<string, unknown>;
}

export class ApiError extends Error {
    readonly status: number;
    readonly type?: ErrorType | string;
    readonly detail?: string;
    readonly field?: string;
    readonly fieldErrors: Record<string, string>;
    readonly errors: string[];
    readonly params: Record<string, unknown>;

    constructor(status: number, details: ApiErrorDetails = {}) {
        super(`HTTP ${status}${details.detail ? `: ${details.detail}` : ''}`);
        this.name = 'ApiError';
        this.status = status;
        this.type = details.type;
        this.detail = details.detail;
        this.field = details.field;
        this.fieldErrors = details.fieldErrors ?? {};
        this.errors = details.errors ?? [];
        this.params = details.params ?? {};
    }
}
