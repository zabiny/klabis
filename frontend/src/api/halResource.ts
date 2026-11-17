import type {HalEnvelope, HalFormsTemplate, Link} from './types';
import {selfHref} from './hateoas';
import {getDialogTitleLabel, getTemplateLabel, labels} from '../localization';

/**
 * A HAL-FORMS template together with the href it submits to and the texts the UI renders, so a
 * call site names the action instead of restating its target, label, dialog title and toast.
 * `href` is undefined only when the template has no target and the resource has no self link.
 */
export interface HalTemplateAction {
    name: string;
    template: HalFormsTemplate;
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

export function getLinkLabel(rel: string): string | undefined {
    const linkLabels = labels.links as Record<string, string>;
    return linkLabels[rel];
}

/**
 * Answers which actions a HAL resource offers. `undefined` means the server did not send that
 * template/link — that is how authorization and resource state are expressed, so callers gate
 * on it instead of keeping their own register of available actions.
 *
 * Pure factory, safe inside a `map` over table rows.
 */
export function halResource(resource: object | null | undefined): HalResource {
    const envelope = (resource ?? undefined) as HalEnvelope | undefined;

    return {
        template(name) {
            const template = envelope?._templates?.[name];
            if (!template) {
                return undefined;
            }
            const label = getTemplateLabel(name) ?? template.title ?? name;
            return {
                name,
                template,
                href: template.target || selfHref(envelope),
                label,
                dialogTitle: getDialogTitleLabel(name) ?? label,
                successMessage: template.title ? `${template.title} — úspěšně uloženo` : labels.ui.savedSuccessfully,
            };
        },

        link(rel) {
            const links = envelope?._links?.[rel];
            if (!links) {
                return undefined;
            }
            const link = Array.isArray(links) ? links.find(candidate => !candidate.templated) : links;
            if (!link?.href) {
                return undefined;
            }
            return {rel, link, href: link.href, label: getLinkLabel(rel) ?? link.title ?? rel};
        },
    };
}
