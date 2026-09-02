import {type ReactElement, useEffect, useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {BedDouble, Bus, Calendar, Check, ChevronDown, CreditCard, Hourglass, Info, MapPin, Pencil, UserPlus} from 'lucide-react';
import {labels} from '../../localization';
import {Alert, Button, Modal, Skeleton} from '../UI';
import type {SelectOption} from '../UI/forms';
import {authorizedFetch} from '../../api/authorizedFetch';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useHalFormOptions} from '../../hooks/useHalFormOptions';
import {useToast} from '../../contexts/toastContext';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas';
import type {HalFormsProperty, HalFormsTemplate} from '../../api/types';
import type {components} from '../../api/klabisApi';
import {formatDate, getRelevantDeadlineIndex} from '../../utils/dateUtils';
import {getTodayIso} from '../../utils/dateUtils';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils';

export interface EventRegistrationDialogProps {
    isOpen: boolean;
    onClose: () => void;
    mode: 'new' | 'edit';
    template: HalFormsTemplate;
    event: {
        name?: string;
        eventDate?: string;
        location?: string | null;
        deadlines?: string[];
        sharedTransportEnabled?: boolean;
        sharedAccommodationEnabled?: boolean;
    };
    prefillHref?: string;
    initialValuesHref?: string;
    onRegistered?: () => void;
}

type RegistrationFormData = components['schemas']['RegistrationDto'];

const SI_FALLBACK_REGEX = /^\d{4,8}$/;

const initialsOf = (name: string): string =>
    name.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part.charAt(0).toUpperCase()).join('');

export const EventRegistrationDialog = ({
    isOpen,
    onClose,
    mode,
    template,
    event,
    prefillHref,
    initialValuesHref,
    onRegistered,
}: EventRegistrationDialogProps): ReactElement | null => {
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();

    const [siCardNumber, setSiCardNumber] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [wantsSharedTransport, setWantsSharedTransport] = useState(false);
    const [wantsSharedAccommodation, setWantsSharedAccommodation] = useState(false);
    const [siError, setSiError] = useState<string | null>(null);
    const [categoryError, setCategoryError] = useState<string | null>(null);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const formHref = mode === 'new' ? prefillHref : initialValuesHref;

    const {data, isLoading: isFormLoading, isError: isFormError} = useQuery<RegistrationFormData>({
        queryKey: ['event-registration-form', formHref ?? ''],
        queryFn: async () => {
            const response = await authorizedFetch(formHref!);
            return response.json();
        },
        enabled: isOpen && !!formHref,
        staleTime: 0,
        gcTime: 0,
        retry: false,
    });

    useEffect(() => {
        if (!isOpen) return;
        setSiCardNumber(data?.siCardNumber ?? '');
        setCategoryId(mode === 'edit' ? (data?.category?.id ?? '') : '');
        setWantsSharedTransport(data?.wantsSharedTransport ?? false);
        setWantsSharedAccommodation(data?.wantsSharedAccommodation ?? false);
        setSiError(null);
        setCategoryError(null);
        setSubmitError(null);
    }, [isOpen, data, mode]);

    const siProp = template.properties.find(prop => prop.name === 'siCardNumber');
    const categoryProp = template.properties.find(prop => prop.name === 'categoryId');
    const transportProp = template.properties.find(prop => prop.name === 'wantsSharedTransport');
    const accommodationProp = template.properties.find(prop => prop.name === 'wantsSharedAccommodation');
    const transportOffered = event.sharedTransportEnabled === true;
    const accommodationOffered = event.sharedAccommodationEnabled === true;
    const showTransportCheckbox = transportProp !== undefined && transportOffered;
    const showAccommodationCheckbox = accommodationProp !== undefined && accommodationOffered;

    const {options: categoryOptions, isLoading: areCategoryOptionsLoading} = useHalFormOptions(categoryProp?.options);

    const {mutate: submitRegistration, isPending: isSubmitting} = useAuthorizedMutation({
        method: template.method || 'POST',
    });

    const memberName = [data?.firstName, data?.lastName].filter(Boolean).join(' ').trim();

    const validateSiChip = (value: string): string | null => {
        const trimmed = value.trim();
        if (!trimmed) return labels.events.registrationModal.siChipRequired;
        let regex = SI_FALLBACK_REGEX;
        if (siProp?.regex) {
            try {
                regex = new RegExp(siProp.regex);
            } catch {
                regex = SI_FALLBACK_REGEX;
            }
        }
        if (!regex.test(trimmed)) return labels.events.registrationModal.siChipInvalidFormat;
        return null;
    };

    const handleClose = () => {
        setSiCardNumber('');
        setCategoryId('');
        setWantsSharedTransport(false);
        setWantsSharedAccommodation(false);
        setSiError(null);
        setCategoryError(null);
        setSubmitError(null);
        onClose();
    };

    const handleSubmit = () => {
        if (!template.target) return;

        const siValidationError = validateSiChip(siCardNumber);
        const categoryValidationError = categoryProp?.required && !categoryId
            ? labels.events.registrationModal.categoryRequired
            : null;
        setSiError(siValidationError);
        setCategoryError(categoryValidationError);
        setSubmitError(null);
        if (siValidationError || categoryValidationError) return;

        const payload: Record<string, unknown> = {siCardNumber: siCardNumber.trim()};
        if (categoryProp && categoryId) payload.categoryId = categoryId;
        if (showTransportCheckbox) payload.wantsSharedTransport = wantsSharedTransport;
        if (showAccommodationCheckbox) payload.wantsSharedAccommodation = wantsSharedAccommodation;

        submitRegistration(
            {url: normalizeKlabisApiPath(template.target), data: payload},
            {
                onSuccess: async () => {
                    await invalidateAllCaches();
                    addToast(template.title
                        ? labels.events.registrationModal.savedWithTemplate(template.title)
                        : labels.ui.savedSuccessfully, 'success');
                    handleClose();
                    onRegistered?.();
                },
                onError: (error: unknown) => {
                    const validationError = toFormValidationError(error);
                    if (isFormValidationError(validationError)) {
                        const fieldErrors = validationError.validationErrors;
                        const siMessage = fieldErrors['siCardNumber'];
                        const categoryMessage = fieldErrors['categoryId'];
                        setSiError(siMessage ?? null);
                        setCategoryError(categoryMessage ?? null);
                        const fallback = Object.values(fieldErrors)[0];
                        setSubmitError(siMessage || categoryMessage ? null : (fallback ?? labels.errors.requestFailed));
                    } else {
                        setSubmitError(validationError.message || labels.errors.requestFailed);
                    }
                },
            }
        );
    };

    const confirmLabel = mode === 'new'
        ? labels.events.registrationModal.confirmNew
        : labels.events.registrationModal.confirmEdit;

    const footer = (
        <>
            <Button variant="secondary" onClick={handleClose} disabled={isSubmitting}>
                {labels.buttons.cancel}
            </Button>
            <Button
                variant="primary"
                startIcon={<Check className="w-4 h-4"/>}
                loading={isSubmitting}
                disabled={isSubmitting}
                onClick={handleSubmit}
            >
                {isSubmitting ? labels.buttons.submitting : confirmLabel}
            </Button>
        </>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            closeOnBackdropClick={!isSubmitting}
            size="lg"
            title={mode === 'new' ? labels.dialogTitles.registerForEvent : labels.dialogTitles.editRegistration}
            headerIcon={mode === 'new'
                ? <UserPlus className="h-5 w-5 text-primary"/>
                : <Pencil className="h-5 w-5 text-primary"/>}
            context={!isFormLoading
                ? <RegistrationContext event={event} memberName={mode === 'edit' ? memberName : undefined}/>
                : undefined}
            footer={isFormLoading ? undefined : footer}
            footerNote={mode === 'edit' && !isFormLoading ? (
                <span className="flex items-center gap-2">
                    <Info className="h-3.5 w-3.5 text-primary"/>
                    <span className="text-xs text-gray-500">{labels.events.registrationModal.editFooterNote}</span>
                </span>
            ) : undefined}
        >
            {isFormLoading ? (
                <DialogSkeleton/>
            ) : (
                <div
                    data-testid="event-registration-form"
                    className={`flex flex-col gap-4 ${isSubmitting ? 'pointer-events-none opacity-60' : ''}`}
                >
                    {isFormError && (
                        <Alert severity="error">{labels.events.registrationModal.prefillLoadError}</Alert>
                    )}
                    {submitError && (
                        <Alert severity="error">{submitError}</Alert>
                    )}
                    <SiChipField
                        value={siCardNumber}
                        error={siError}
                        helperText={mode === 'new'
                            ? labels.events.registrationModal.siChipHelperPrefilled
                            : labels.events.registrationModal.siChipHelper}
                        disabled={isSubmitting}
                        onChange={(value) => {
                            setSiCardNumber(value);
                            if (siError) setSiError(null);
                        }}
                    />
                    {categoryProp && (
                        <CategoryField
                            prop={categoryProp}
                            value={categoryId}
                            error={categoryError}
                            options={categoryOptions}
                            isLoadingOptions={areCategoryOptionsLoading}
                            disabled={isSubmitting}
                            onChange={(value) => {
                                setCategoryId(value);
                                if (categoryError) setCategoryError(null);
                            }}
                        />
                    )}
                    {(showTransportCheckbox || showAccommodationCheckbox) && (
                        <div className="flex flex-col gap-3">
                            {showTransportCheckbox && (
                                <SharedServiceCheckbox
                                    name="wantsSharedTransport"
                                    label={labels.fields.wantsSharedTransport}
                                    icon={<Bus className="h-[18px] w-[18px]"/>}
                                    checked={wantsSharedTransport}
                                    disabled={isSubmitting}
                                    onChange={setWantsSharedTransport}
                                />
                            )}
                            {showAccommodationCheckbox && (
                                <SharedServiceCheckbox
                                    name="wantsSharedAccommodation"
                                    label={labels.fields.wantsSharedAccommodation}
                                    icon={<BedDouble className="h-[18px] w-[18px]"/>}
                                    checked={wantsSharedAccommodation}
                                    disabled={isSubmitting}
                                    onChange={setWantsSharedAccommodation}
                                />
                            )}
                        </div>
                    )}
                </div>
            )}
        </Modal>
    );
};

function DialogSkeleton() {
    return (
        <div data-testid="event-registration-dialog-skeleton" className="flex flex-col gap-4">
            <Skeleton height="1.25rem" width="60%"/>
            <Skeleton height="1rem" width="45%"/>
            <Skeleton height="2.75rem"/>
            <Skeleton height="2.75rem"/>
        </div>
    );
}

function RegistrationContext({
    event,
    memberName,
}: {
    event: EventRegistrationDialogProps['event'];
    memberName?: string;
}) {
    const relevantDeadline = event.deadlines && event.deadlines.length > 0
        ? event.deadlines[getRelevantDeadlineIndex(event.deadlines, getTodayIso())]
        : undefined;

    return (
        <div className="flex flex-col gap-2.5">
            {memberName && (
                <div
                    data-testid="registration-member-chip"
                    className="flex items-center gap-2.5 rounded-md border border-border bg-surface px-3 py-2.5"
                >
                    <span className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-primary-subtle text-xs font-semibold text-primary">
                        {initialsOf(memberName)}
                    </span>
                    <span className="text-[13px] font-medium text-text-primary">{memberName}</span>
                    <span className="ml-auto text-[11px] font-semibold text-text-secondary">
                        {labels.events.registrationModal.editingCaption}
                    </span>
                </div>
            )}
            {event.name && (
                <p className="text-base font-bold text-text-primary">{event.name}</p>
            )}
            {(event.eventDate || event.location) && (
                <div className="flex flex-wrap items-center gap-[18px]">
                    {event.eventDate && (
                        <span className="flex items-center gap-1.5">
                            <Calendar className="h-3.5 w-3.5 text-primary"/>
                            <span className="text-[13px] text-text-secondary">{formatDate(event.eventDate)}</span>
                        </span>
                    )}
                    {event.location && (
                        <span className="flex items-center gap-1.5">
                            <MapPin className="h-3.5 w-3.5 text-primary"/>
                            <span className="text-[13px] text-text-secondary">{event.location}</span>
                        </span>
                    )}
                </div>
            )}
            {relevantDeadline && (
                <span
                    data-testid="registration-deadline-chip"
                    className="inline-flex items-center gap-2 self-start rounded-md border border-orange-200 bg-orange-50 px-3 py-1.5"
                >
                    <Hourglass className="h-3.5 w-3.5 text-amber-600"/>
                    <span className="text-xs font-semibold text-amber-800">
                        {labels.events.registrationModal.deadlinePrefix(formatDate(relevantDeadline))}
                    </span>
                </span>
            )}
        </div>
    );
}

function SiChipField({
    value,
    error,
    helperText,
    disabled,
    onChange,
}: {
    value: string;
    error: string | null;
    helperText: string;
    disabled: boolean;
    onChange: (value: string) => void;
}) {
    return (
        <div className="flex flex-col gap-1.5">
            <label htmlFor="event-registration-si-chip" className="flex items-center gap-1 text-sm font-semibold text-gray-700 dark:text-gray-300">
                {labels.events.registrationModal.siChip}
                <span className="text-sm font-bold text-red-600">*</span>
            </label>
            <div className="relative">
                <CreditCard className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-tertiary"/>
                <input
                    id="event-registration-si-chip"
                    type="text"
                    inputMode="numeric"
                    value={value}
                    disabled={disabled}
                    onChange={(e) => onChange(e.target.value)}
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
}

function CategoryField({
    value,
    error,
    options,
    isLoadingOptions,
    disabled,
    onChange,
}: {
    prop: HalFormsProperty;
    value: string;
    error: string | null;
    options: SelectOption[];
    isLoadingOptions: boolean;
    disabled: boolean;
    onChange: (value: string) => void;
}) {
    return (
        <div className="flex flex-col gap-1.5">
            <label htmlFor="event-registration-category" className="flex items-center gap-1 text-sm font-semibold text-gray-700 dark:text-gray-300">
                {labels.events.registrationModal.category}
                <span className="text-sm font-bold text-red-600">*</span>
            </label>
            <div className="relative">
                <select
                    id="event-registration-category"
                    value={value}
                    disabled={disabled || isLoadingOptions}
                    onChange={(e) => onChange(e.target.value)}
                    className={`h-11 w-full appearance-none rounded-md border bg-surface py-2.5 pl-3 pr-10 text-[15px] text-text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-60 ${error ? 'border-error' : 'border-border'}`}
                >
                    <option value="">{labels.events.registrationModal.selectCategoryPlaceholder}</option>
                    {options.map(option => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-tertiary"/>
            </div>
            {error && (
                <p className="text-xs text-error">{error}</p>
            )}
        </div>
    );
}

function SharedServiceCheckbox({
    name,
    label,
    icon,
    checked,
    disabled,
    onChange,
}: {
    name: string;
    label: string;
    icon: ReactElement;
    checked: boolean;
    disabled: boolean;
    onChange: (checked: boolean) => void;
}) {
    return (
        <label htmlFor={`event-registration-${name}`} className="flex cursor-pointer items-center gap-2.5">
            <input
                id={`event-registration-${name}`}
                type="checkbox"
                className="peer sr-only"
                checked={checked}
                disabled={disabled}
                onChange={(e) => onChange(e.target.checked)}
            />
            <span
                className={`flex h-[18px] w-[18px] flex-shrink-0 items-center justify-center rounded border-[1.5px] transition-colors peer-focus-visible:ring-2 peer-focus-visible:ring-primary peer-focus-visible:ring-offset-0 ${checked ? 'border-primary bg-primary' : 'border-border bg-surface'}`}
            >
                {checked && <Check className="h-3 w-3 text-white"/>}
            </span>
            <span className="text-sm text-text-primary">{label}</span>
            <span className="ml-auto text-zinc-400">{icon}</span>
        </label>
    );
}
