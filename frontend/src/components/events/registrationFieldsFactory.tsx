/* eslint-disable react-refresh/only-export-components -- module exports a field-factory function alongside its private field components, mirroring eventFormFieldsFactory.tsx */
import {type ReactElement} from 'react';
import {useField} from 'formik';
import {BedDouble, Bus, Check, ChevronDown, CreditCard} from 'lucide-react';
import type {HalFormFieldFactory, HalFormsInputProps} from '../HalNavigator2/halforms';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormOptions} from '../../hooks/useHalFormOptions';
import {labels} from '../../localization';

interface RegistrationFactoryOptions {
    mode: 'new' | 'edit';
    serverFieldErrors: Record<string, string>;
}

/**
 * Domain-specific client-side message for the SI chip field. The engine's Yup schema
 * (built from `required` + `regex`) still gates submission with generic messages;
 * this only chooses which localized string the dialog shows on the field.
 */
const siCardDisplayError = (prop: HalFormsInputProps['prop'], raw: unknown): string | undefined => {
    const value = typeof raw === 'string' ? raw.trim() : '';
    if (prop.required && !value) return labels.events.registrationModal.siChipRequired;
    if (value && prop.regex) {
        let regex: RegExp | null = null;
        try {
            regex = new RegExp(prop.regex);
        } catch {
            regex = null;
        }
        if (regex && !regex.test(value)) return labels.events.registrationModal.siChipInvalidFormat;
    }
    return undefined;
};

const SiCardNumberField = ({prop, mode, serverError}: {
    prop: HalFormsInputProps['prop'];
    mode: 'new' | 'edit';
    serverError?: string;
}): ReactElement => {
    const [field, meta] = useField<string>(prop.name);
    const error = serverError ?? (meta.touched ? siCardDisplayError(prop, field.value) : undefined);
    const helperText = mode === 'new'
        ? labels.events.registrationModal.siChipHelperPrefilled
        : labels.events.registrationModal.siChipHelper;

    return (
        <div className="flex flex-col gap-1.5">
            <label
                htmlFor="event-registration-si-chip"
                className="flex items-center gap-1 text-sm font-semibold text-gray-700 dark:text-gray-300"
            >
                {labels.events.registrationModal.siChip}
                <span className="text-sm font-bold text-red-600">*</span>
            </label>
            <div className="relative">
                <CreditCard className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-tertiary"/>
                <input
                    id="event-registration-si-chip"
                    type="text"
                    inputMode="numeric"
                    {...field}
                    value={field.value ?? ''}
                    className={`h-11 w-full rounded-md border bg-surface py-2.5 pl-9 pr-3 text-[15px] text-text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-60 ${error ? 'border-error' : 'border-border'}`}
                />
            </div>
            {error ? (
                <p className="text-xs text-error">{error}</p>
            ) : (
                <p className="text-xs text-text-tertiary">{helperText}</p>
            )}
        </div>
    );
};

const CategoryField = ({prop, serverError}: {
    prop: HalFormsInputProps['prop'];
    serverError?: string;
}): ReactElement => {
    const [field, meta] = useField<string>(prop.name);
    const {options, isLoading} = useHalFormOptions(prop.options);
    const clientError = prop.required && !field.value ? labels.events.registrationModal.categoryRequired : undefined;
    const error = serverError ?? (meta.touched ? clientError : undefined);

    return (
        <div className="flex flex-col gap-1.5">
            <label
                htmlFor="event-registration-category"
                className="flex items-center gap-1 text-sm font-semibold text-gray-700 dark:text-gray-300"
            >
                {labels.events.registrationModal.category}
                <span className="text-sm font-bold text-red-600">*</span>
            </label>
            <div className="relative">
                <select
                    id="event-registration-category"
                    {...field}
                    value={field.value ?? ''}
                    disabled={isLoading}
                    className={`h-11 w-full appearance-none rounded-md border bg-surface py-2.5 pl-3 pr-10 text-[15px] text-text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-60 ${error ? 'border-error' : 'border-border'}`}
                >
                    <option value="">{labels.events.registrationModal.selectCategoryPlaceholder}</option>
                    {options.map(option => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-tertiary"/>
            </div>
            {error && <p className="text-xs text-error">{error}</p>}
        </div>
    );
};

const SHARED_SERVICE_META: Record<string, {label: string; icon: ReactElement}> = {
    wantsSharedTransport: {label: labels.fields.wantsSharedTransport, icon: <Bus className="h-[18px] w-[18px]"/>},
    wantsSharedAccommodation: {label: labels.fields.wantsSharedAccommodation, icon: <BedDouble className="h-[18px] w-[18px]"/>},
};

const SharedServiceCheckbox = ({prop}: {prop: HalFormsInputProps['prop']}): ReactElement => {
    const [field] = useField({name: prop.name, type: 'checkbox'});
    const meta = SHARED_SERVICE_META[prop.name];
    const checked = Boolean(field.value);

    return (
        <label htmlFor={`event-registration-${prop.name}`} className="flex cursor-pointer items-center gap-2.5">
            <input
                id={`event-registration-${prop.name}`}
                type="checkbox"
                className="peer sr-only"
                {...field}
                checked={checked}
                aria-label={meta.label}
            />
            <span
                className={`flex h-[18px] w-[18px] flex-shrink-0 items-center justify-center rounded border-[1.5px] transition-colors peer-focus-visible:ring-2 peer-focus-visible:ring-primary peer-focus-visible:ring-offset-0 ${checked ? 'border-primary bg-primary' : 'border-border bg-surface'}`}
            >
                {checked && <Check className="h-3 w-3 text-white"/>}
            </span>
            <span className="text-sm text-text-primary">{meta.label}</span>
            <span className="ml-auto text-zinc-400">{meta.icon}</span>
        </label>
    );
};

/**
 * Field factory for the event-registration dialog. Composes over klabisFieldsFactory,
 * overriding only the four registration properties with presentation the HAL-FORMS
 * template cannot express: an icon-prefixed SI input with a localized format message,
 * a category select whose "required" message is domain-specific, and the shared-service
 * checkboxes with per-offer icons. Server field-validation errors are threaded through
 * `serverFieldErrors` so a rejected submit lands on the right field. Submission itself
 * stays gated by the engine's Yup schema (built from the template's required/regex).
 */
export const createRegistrationFieldsFactory = (
    {mode, serverFieldErrors}: RegistrationFactoryOptions
): HalFormFieldFactory => (fieldType, conf) => {
    switch (conf.prop.name) {
        case 'siCardNumber':
            return <SiCardNumberField prop={conf.prop} mode={mode} serverError={serverFieldErrors[conf.prop.name]}/>;
        case 'categoryId':
            return <CategoryField prop={conf.prop} serverError={serverFieldErrors[conf.prop.name]}/>;
        case 'wantsSharedTransport':
        case 'wantsSharedAccommodation':
            return <SharedServiceCheckbox prop={conf.prop}/>;
        default:
            return klabisFieldsFactory(fieldType, conf);
    }
};
