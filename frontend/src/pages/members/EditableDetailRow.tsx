import type {ReactElement, ReactNode} from 'react';
import {Field, type FieldProps} from 'formik';
import type {HalFormsProperty, HalFormsTemplate} from '../../api';
import type {OptionItem} from '../../api/types';
import {COMPOSITE_SUBFIELDS, COMPOSITE_DATE_SUBFIELDS} from './compositeTypes';

interface EditableDetailRowProps {
    label: string;
    fieldName: string;
    template: HalFormsTemplate;
    isEditing: boolean;
    children: ReactNode;
}

function findProperty(template: HalFormsTemplate, fieldName: string): HalFormsProperty | null {
    const directMatch = template.properties.find(p => p.name === fieldName);
    if (directMatch) return directMatch;

    const dotIdx = fieldName.indexOf('.');
    if (dotIdx === -1) return null;

    const parentName = fieldName.substring(0, dotIdx);
    const subField = fieldName.substring(dotIdx + 1);

    const parentProp = template.properties.find(p => p.name === parentName);
    if (!parentProp) return null;

    const compositeFields = COMPOSITE_SUBFIELDS[parentProp.type];
    if (!compositeFields || !compositeFields.includes(subField)) return null;

    const inputType = COMPOSITE_DATE_SUBFIELDS.has(subField) ? 'date' : 'text';
    return {
        name: fieldName,
        type: inputType,
        prompt: parentProp.prompt,
        required: parentProp.required,
        readOnly: parentProp.readOnly,
    };
}

function isEditable(prop: HalFormsProperty | null): boolean {
    return prop !== null && !prop.readOnly;
}

function hasInlineOptions(prop: HalFormsProperty): boolean {
    return !!prop.options?.inline && prop.options.inline.length > 0;
}

function getOptionLabel(opt: OptionItem | string | number): string {
    if (typeof opt === 'object' && opt !== null && 'prompt' in opt) {
        return opt.prompt ?? String(opt.value);
    }
    return String(opt);
}

function getOptionValue(opt: OptionItem | string | number): string | number {
    if (typeof opt === 'object' && opt !== null && 'value' in opt) {
        return opt.value;
    }
    return opt;
}

const ReadOnlyRow = ({label, children}: { label: string; children: ReactNode }) => (
    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{label}</dt>
        <dd className="text-sm text-text-primary">{children}</dd>
    </div>
);

const EditableField = ({prop, fieldName}: { prop: HalFormsProperty; fieldName: string }) => {
    const fieldLabel = prop.prompt || fieldName;

    if (hasInlineOptions(prop)) {
        return (
            <Field name={fieldName}>
                {({field}: FieldProps) => (
                    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
                        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{fieldLabel}</dt>
                        <dd className="text-sm text-text-primary flex-1">
                            <select {...field} className="w-full px-3 py-1.5 border border-border rounded-md bg-surface-base text-text-primary">
                                <option value="">Vyberte...</option>
                                {prop.options!.inline!.map((opt) => (
                                    <option key={String(getOptionValue(opt))} value={getOptionValue(opt)}>
                                        {getOptionLabel(opt)}
                                    </option>
                                ))}
                            </select>
                        </dd>
                    </div>
                )}
            </Field>
        );
    }

    if (prop.type === 'boolean') {
        return (
            <Field name={fieldName}>
                {({field, form}: FieldProps) => (
                    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
                        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{fieldLabel}</dt>
                        <dd className="text-sm text-text-primary flex-1">
                            <input
                                type="checkbox"
                                checked={!!field.value}
                                onChange={(e) => form.setFieldValue(fieldName, e.target.checked)}
                                name={field.name}
                                onBlur={field.onBlur}
                            />
                        </dd>
                    </div>
                )}
            </Field>
        );
    }

    if (prop.type === 'textarea') {
        return (
            <Field name={fieldName}>
                {({field}: FieldProps) => (
                    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
                        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{fieldLabel}</dt>
                        <dd className="text-sm text-text-primary flex-1">
                            <textarea
                                {...field}
                                value={field.value ?? ''}
                                required={prop.required}
                                className="w-full px-3 py-1.5 border border-border rounded-md bg-surface-base text-text-primary"
                                rows={3}
                            />
                        </dd>
                    </div>
                )}
            </Field>
        );
    }

    const inputType = (['text', 'email', 'tel', 'url', 'date', 'number'].includes(prop.type))
        ? prop.type
        : 'text';

    return (
        <Field name={fieldName}>
            {({field}: FieldProps) => (
                <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
                    <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{fieldLabel}</dt>
                    <dd className="text-sm text-text-primary flex-1">
                        <input
                            {...field}
                            value={field.value ?? ''}
                            type={inputType}
                            required={prop.required}
                            className="w-full px-3 py-1.5 border border-border rounded-md bg-surface-base text-text-primary"
                        />
                    </dd>
                </div>
            )}
        </Field>
    );
};

export const EditableDetailRow = ({
    label,
    fieldName,
    template,
    isEditing,
    children,
}: EditableDetailRowProps): ReactElement => {
    const property = findProperty(template, fieldName);

    if (!isEditing || !isEditable(property)) {
        return <ReadOnlyRow label={label}>{children}</ReadOnlyRow>;
    }

    return <EditableField prop={property!} fieldName={fieldName}/>;
};

export type {EditableDetailRowProps};
