import type {HalFormsTemplate, HalResponse, Link} from './types';
import {asLinkArray, selfHref} from './hateoas';
import {getLinkLabel, savedMessage, templateDialogTitle, templateLabel} from '../localization';

export {getLinkLabel} from '../localization';

/**
 * A HAL-FORMS template together with the href it submits to and the texts the UI renders, so a
 * call site names the action instead of restating its target, label, dialog title and toast.
 * `href` is undefined only when the template has no target and the resource has no self link.
 *
 * `resource` is the envelope the action was resolved from: it is what the form edits and prefills
 * from, so call sites never have to hand the resource back alongside the action.
 */
export interface HalTemplateAction {
    name: string;
    template: HalFormsTemplate;
    resource: HalResponse;
    href: string | undefined;
    label: string;
    dialogTitle: string;
    successMessage: string;
}

/**
 * A link relation resolved to the concrete link an action navigates or submits to.
 */
export interface HalLinkAction {
    rel: string;
    link: Link;
    href: string;
    label: string;
}

export interface HalResource {
    template(name: string): HalTemplateAction | undefined;

    link(rel: string): HalLinkAction | undefined;
}

/**
 * Answers which actions a HAL resource offers. `undefined` means the server did not send that
 * template/link — that is how authorization and resource state are expressed, so callers gate
 * on it instead of keeping their own register of available actions.
 *
 * Pure factory, safe inside a `map` over table rows.
 */
export function halResource(resource: object | null | undefined): HalResource {
    const envelope = (resource ?? undefined) as HalResponse | undefined;

    return {
        template(name) {
            const template = envelope?._templates?.[name];
            if (!template || !envelope) {
                return undefined;
            }
            const label = templateLabel(name, template);
            return {
                name,
                template,
                resource: envelope,
                href: template.target || selfHref(envelope),
                label,
                dialogTitle: templateDialogTitle(name, template),
                successMessage: savedMessage(template),
            };
        },

        link(rel) {
            const links = envelope?._links?.[rel];
            if (!links) {
                return undefined;
            }
            const candidates = asLinkArray(links);
            const link = Array.isArray(links) ? candidates.find(candidate => !candidate.templated) : candidates[0];
            if (!link?.href) {
                return undefined;
            }
            return {rel, link, href: link.href, label: getLinkLabel(rel) ?? link.title ?? rel};
        },
    };
}
