import {ApiError} from './apiError';

const PROBLEM_JSON = 'application/problem+json';
const DROPPED_PROPERTIES = new Set(['title', 'instance', 'status']);
const MAPPED_PROPERTIES = new Set(['type', 'detail', 'field', 'fieldErrors', 'errors', 'parameterErrors']);

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

const asString = (value: unknown): string | undefined => typeof value === 'string' ? value : undefined;

const asStringArray = (value: unknown): string[] =>
    Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];

const asStringRecord = (value: unknown): Record<string, string> =>
    isRecord(value)
        ? Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string] => typeof entry[1] === 'string'))
        : {};

async function readProblem(response: Response): Promise<Record<string, unknown> | undefined> {
    if (!(response.headers.get('Content-Type') ?? '').includes(PROBLEM_JSON)) {
        return undefined;
    }
    try {
        const body: unknown = await response.clone().json();
        return isRecord(body) ? body : undefined;
    } catch {
        return undefined;
    }
}

export async function buildFetchError(response: Response): Promise<ApiError> {
    const problem = await readProblem(response);
    if (!problem) {
        return new ApiError(response.status);
    }
    const params = Object.fromEntries(
        Object.entries(problem).filter(([key]) => !DROPPED_PROPERTIES.has(key) && !MAPPED_PROPERTIES.has(key))
    );
    return new ApiError(response.status, {
        type: asString(problem.type),
        detail: asString(problem.detail),
        field: asString(problem.field),
        fieldErrors: asStringRecord(problem.fieldErrors),
        errors: [...asStringArray(problem.errors), ...asStringArray(problem.parameterErrors)],
        params,
    });
}
