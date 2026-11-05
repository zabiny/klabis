import {ApiError} from '../api/apiError';
import type {ErrorType} from '../api/apiError';
import {labels} from './labels';

const isKnownType = (type: string | undefined): type is ErrorType =>
    type !== undefined && Object.hasOwn(labels.errorTypes, type);

const genericTypeForStatus = (status: number): ErrorType | undefined => {
    if (status === 400) return '/errors/invalid-request';
    if (status === 401) return '/errors/unauthorized';
    if (status === 403) return '/errors/forbidden';
    if (status === 404) return '/errors/not-found';
    if (status === 409) return '/errors/conflict';
    if (status >= 500) return '/errors/unexpected';
    return undefined;
};

export function describeError(error: unknown): string {
    if (!(error instanceof ApiError)) {
        return labels.errors.unexpectedError;
    }
    if (isKnownType(error.type)) {
        return labels.errorTypes[error.type](error.params);
    }
    const generic = genericTypeForStatus(error.status);
    return generic ? labels.errorTypes[generic]({}) : labels.errors.unexpectedError;
}
