import {type HalFormsResponse, type HalFormsTemplateMethod, type Link} from "./index";

import {klabisAuthUserManager} from "./klabisUserManager";

const HAL_FORMS_CONTENT_TYPE = "application/prs.hal-forms+json; charset=utf-8";

async function fetchHalFormsData(link: Link): Promise<HalFormsResponse> {
    if (!link.href) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const user = await klabisAuthUserManager.getUser();
    const res = await fetch(link.href, {
        headers: {
            Accept: HAL_FORMS_CONTENT_TYPE,
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

async function submitHalFormsData(method: HalFormsTemplateMethod, link: Link, formData: Record<string, any>) {
    if (!link.href) {
        throw new Error("Incorrect link instance - href is missing!")
    }

    const user = await klabisAuthUserManager.getUser();
    const res = await fetch(link.href, {
        method: method,
        body: JSON.stringify(formData),
        headers: {
            "Content-type": "application/json",
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return;
}


export {fetchHalFormsData, submitHalFormsData};