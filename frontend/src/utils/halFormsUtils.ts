/**
 * Utility functions for HAL Forms handling
 */

import type {HalFormsProperty, HalFormsTemplate} from '../api';
import type {Link} from '../api/types';
import {toApiHref} from '../api/apiPath';

export function enrichTemplateWithReadOnlyFields(
    template: HalFormsTemplate,
    resourceData: Record<string, unknown>
): HalFormsTemplate {
    const templateFieldNames = new Set(template.properties.map(p => p.name));

    const readOnlyProps: HalFormsProperty[] = Object.keys(resourceData)
        .filter(key => !templateFieldNames.has(key) && !key.startsWith('_'))
        .filter(key => {
            const value = resourceData[key];
            return value === null || value === undefined || typeof value !== 'object';
        })
        .map(key => ({
            name: key,
            type: 'text',
            readOnly: true,
        }));

    return {
        ...template,
        properties: [...template.properties, ...readOnlyProps],
    };
}

/**
 * Determine if data should be fetched from the template target
 * @param templateTarget - The target from the HAL Forms template
 * @param currentPathname - The current resource pathname
 * @returns true if target differs from current pathname and should be fetched
 */
export function shouldFetchTargetData(
    templateTarget: string | undefined,
    currentPathname: string
): boolean {
    // No target specified - use current resource data
    if (!templateTarget || templateTarget === '') {
        return false;
    }

    const normalizedTarget = toApiHref(templateTarget);
    const normalizedCurrent = toApiHref(currentPathname);

    // If normalized paths are the same, no need to fetch
    return normalizedTarget !== normalizedCurrent;
}

export function toRegistrationLink(target?: string): Link | undefined {
    return target ? {href: target} : undefined;
}
