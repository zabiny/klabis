import {type HalFormsResponse, type TemplateTarget} from "./index";
import {authorizedFetch, FetchError} from "./authorizedFetch";

interface FormValidationError extends Error {
    validationErrors: Record<string, string>,
    formData: Record<string, any>
}

const isFormValidationError = (item: any): item is FormValidationError => {
    return item && item.validationErrors && item.message;
}

const HAL_FORMS_CONTENT_TYPE = "application/prs.hal-forms+json; charset=utf-8";

async function fetchHalFormsData(link: TemplateTarget): Promise<HalFormsResponse> {
    if (!link.target) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const res = await authorizedFetch(link.target, {
        headers: {
            Accept: HAL_FORMS_CONTENT_TYPE,
        },
    });
    return res.json();
}

async function submitHalFormsData(link: TemplateTarget, formData: Record<string, unknown>): Promise<Response> {
    if (!link.target) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const res = await authorizedFetch(
        link.target,
        {
            method: link.method || 'POST',
            body: JSON.stringify(formData),
            headers: {
                "Content-type": "application/json",
            },
        },
        false // Don't throw on error - handle 400 validation errors specially
    );

    // Handle form validation errors with problem+json response
    if (res.status === 400 && res.headers.get("Content-type") === "application/problem+json") {
        const responseBody = await res.json();
        throw {
            message: "Form validation errors",
            validationErrors: responseBody.errors,
            formData: formData
        } as FormValidationError;
    }

    // Throw for other error statuses
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res;
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
    fetchHalFormsData,
    submitHalFormsData,
    toFormValidationError,
    type TemplateTarget,
    type FormValidationError,
    isFormValidationError
};