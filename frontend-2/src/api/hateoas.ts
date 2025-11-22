import {type HalFormsResponse, type TemplateTarget} from "./index";

import {klabisAuthUserManager} from "./klabisUserManager";

interface FormValidationError extends Error {
    validationErrors: Record<string, string>
}

const isFormValidationError = (item: any): item is FormValidationError => {
    return item && item.validationErrors && item.message;
}

const HAL_FORMS_CONTENT_TYPE = "application/prs.hal-forms+json; charset=utf-8";

async function fetchHalFormsData(link: TemplateTarget): Promise<HalFormsResponse> {
    if (!link.target) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const user = await klabisAuthUserManager.getUser();
    const res = await fetch(link.target, {
        headers: {
            Accept: HAL_FORMS_CONTENT_TYPE,
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

async function submitHalFormsData(link: TemplateTarget, formData: Record<string, any>): Promise<Response> {
    if (!link.target) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const user = await klabisAuthUserManager.getUser();
    const res = await fetch(link.target, {
        method: link.method || 'POST',
        body: JSON.stringify(formData),
        headers: {
            "Content-type": "application/json",
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (res.status == 400 && res.headers.get("Content-type") === "application/problem+json") {
        const responseBody = await res.json();
        throw {
            message: "Form validation errors",
            validationErrors: responseBody.errors
        } as FormValidationError;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res;
}

export {fetchHalFormsData, submitHalFormsData, type TemplateTarget, type FormValidationError, isFormValidationError};