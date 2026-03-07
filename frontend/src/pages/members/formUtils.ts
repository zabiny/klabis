import * as Yup from 'yup';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {COMPOSITE_SUBFIELDS} from './compositeTypes';

export function buildValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<any> {
    const shape: Record<string, Yup.AnySchema> = {};

    for (const prop of template.properties) {
        if (prop.readOnly) continue;

        let schema: Yup.AnySchema = Yup.string();

        if (prop.type === 'number') {
            schema = Yup.number();
        } else if (prop.type === 'boolean') {
            schema = Yup.boolean();
        }

        if (prop.required) {
            schema = (schema as any).required(`${prop.prompt || prop.name} je povinné pole`);
        }

        if (prop.regex) {
            schema = (schema as Yup.StringSchema).matches(
                new RegExp(prop.regex),
                `${prop.prompt || prop.name} nemá správný formát`
            );
        }

        shape[prop.name] = schema;
    }

    return Yup.object().shape(shape);
}

export function buildInitialValues(
    resourceData: HalResponse,
    template: HalFormsTemplate
): Record<string, unknown> {
    const values: Record<string, unknown> = {};

    for (const prop of template.properties) {
        const subfields = COMPOSITE_SUBFIELDS[prop.type];
        if (subfields) {
            const compositeData = (resourceData[prop.name] as Record<string, unknown>) ?? {};
            const nested: Record<string, unknown> = {};
            for (const sf of subfields) {
                nested[sf] = compositeData[sf] ?? '';
            }
            values[prop.name] = nested;
        } else {
            values[prop.name] = resourceData[prop.name] ?? '';
        }
    }

    return values;
}

export function buildEmptyInitialValues(
    template: HalFormsTemplate
): Record<string, unknown> {
    const values: Record<string, unknown> = {};

    for (const prop of template.properties) {
        if (prop.readOnly) continue;

        const subfields = COMPOSITE_SUBFIELDS[prop.type];
        if (subfields) {
            const nested: Record<string, unknown> = {};
            for (const sf of subfields) {
                nested[sf] = '';
            }
            values[prop.name] = nested;
        } else if (prop.type === 'boolean') {
            values[prop.name] = false;
        } else {
            values[prop.name] = '';
        }
    }

    return values;
}
