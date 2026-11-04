/**
 * Utility functions for HAL Forms handling
 */

import type {HalFormsProperty, HalFormsTemplate} from '../api';
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

export function shouldFetchTargetData(
    templateTarget: string | undefined,
    currentPathname: string
): boolean {
    return !!templateTarget && toApiHref(templateTarget) !== toApiHref(currentPathname);
}
