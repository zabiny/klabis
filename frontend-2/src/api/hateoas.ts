import {FetchError} from "./authorizedFetch";
import type {HalResourceLinks, HalResponse, KlabisAction, KlabisActionName, KlabisHateoasObject} from "./types.ts";
import type {Link} from "./index.ts";
import {isString} from "formik";

interface FormValidationError extends Error {
    validationErrors: Record<string, string>,
    formData: Record<string, any>
}

const isFormValidationError = (item: any): item is FormValidationError => {
    return item && item.validationErrors && item.message;
}

/**
 * Checks if a FetchError represents a HAL Forms validation error
 * and converts it to a FormValidationError if so.
 */
function toFormValidationError(error: unknown): Error {
    if (!(error instanceof FetchError)) {
        return error instanceof Error ? error : new Error(String(error));
    }

    // Check for 400 + application/problem+json
    if (
        error.responseStatus === 400 &&
        error.responseHeaders.get('Content-Type') === 'application/problem+json'
    ) {
        try {
            const problemJson = JSON.parse(error.responseBody || '{}');
            return {
                message: 'Form validation errors',
                validationErrors: problemJson.errors || {},
                formData: {}  // We don't have formData in this context
            } as FormValidationError;
        } catch {
            // If parsing fails, return original error
            return error;
        }
    }

    return error;
}

export {
    toFormValidationError,
    type FormValidationError,
    isFormValidationError
};

function isKlabisHateoasObject(item: KlabisHateoasObject | string[]) {
    return (item as KlabisHateoasObject)._actions !== undefined;
}

function isHalModel(item: any): item is HalResponse {
    return (item as HalResponse)._links !== undefined;
}

export const findAction = (rel: string, actions: KlabisAction[]): KlabisAction | undefined => {
    return actions.find(l => isLink(l) && l.rel === rel || rel === l);
}
export const hasAction = (item: KlabisHateoasObject | KlabisAction[] | undefined, action: KlabisActionName): boolean => {
    if (item === undefined) {
        return false;
    }

    if (Array.isArray(item)) {
        return findAction(action, item) !== undefined || false;
    }

    if (isKlabisHateoasObject(item)) {
        return hasAction(item._actions || [], action);
    }

    if (isHalModel(item)) {
        return hasAction(item._links || [], action);
    }

    return false;
}
export const isLink = (item: any): item is Link => {
    return item !== undefined && item !== null && (item as Link).href !== undefined;
}

export function toHref(source: HalResourceLinks): string {
    if (Array.isArray(source)) {
        return toHref(source[0])
    } else if (isLink(source)) {
        if (!source.href) {
            throw new Error("Chybi hodnota href attributu v Link instanci (" + JSON.stringify(source) + ")")
        }
        return source.href
    } else if (isString(source)) {
        return source;
    } else {
        throw new Error("Unknown NavigationTarget: " + JSON.stringify(source, null, 2))
    }
}

/**
 * Converts relative URL to absolute using current window.location's hostname. Absolute URL is left unchanged
 * @param input
 */
export const normalizeUrl = (input: string): string => {
    if (input.startsWith("/")) {
        // relative paths - normalize against current location URL
        const result = new URL(window.location.toString());
        result.pathname = input;
        result.search = "";
        return result.toString();
    } else {
        // presumably absolute path, return unchanged
        return input;
    }
}